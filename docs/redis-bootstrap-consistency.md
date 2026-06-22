# Redis Lazy Bootstrap 정합성 설계 고민

## 배경

Post 좋아요 기능에 Redis Set을 캐시로 도입하면서, 서버 재시작 후 Redis가 비어 있는 상태에서 요청이 들어올 때 DB 데이터를 Redis로 옮기는 **Lazy Bootstrap** 전략을 채택했다.

초기 구현에서는 `SETNX(initKey)` 하나로 "락 획득"과 "초기화 완료 표시"를 동시에 처리했다. 이로 인해 다음 동시성 버그가 발생한다.

## 문제: lock ≠ done 혼용

```
DB: member 1이 post 1에 좋아요 있음 / Redis: 비어 있음

요청 A: SETNX(post:likes:1:init) → true  → DB 조회 시작 (아직 Redis 비어있음)
요청 B: SETNX(post:likes:1:init) → false → "초기화 완료"로 착각 → 바로 toggle 실행

B의 toggle: SISMEMBER → 0 (없음) → SADD → Redis count=1, liked=true 응답
A 완료:     DB member 1 → Redis 추가 (이미 있으므로 Set 중복 무시)

결과: B는 "새로 좋아요"로 판단 → DB에 save() → 중복 like row 또는 restore 로직 오염
```

`setIfAbsent`는 **락을 잡았는가**를 알려줄 뿐, **작업이 완료됐는가**를 보장하지 않는다.

## 두 가지 해결 방안

### 방안 A: lock 실패 시 skip (return)

```
B: lockKey SETNX 실패 → 그냥 return → 빈 Redis에 toggle
```

| 항목 | 내용 |
|------|------|
| 장점 | 구현 단순, 응답 빠름 |
| 단점 | 최초 bootstrap 순간 B가 빈 Redis에 toggle → DB 동기화 로직까지 오염 |
| 위험 | 정합성 보장 불가, DB에 잘못된 row 삽입 가능 |

### 방안 B: lock 실패 시 initKey 생길 때까지 대기 (채택)

```
B: lockKey SETNX 실패 → sleep(100ms) → initKey 재확인 → 완료되면 통과
```

| 항목 | 내용 |
|------|------|
| 장점 | A 완료 후 정상 상태의 Redis에서 toggle → 정합성 보장 |
| 단점 | 응답 지연 가능 (최대 retry × sleep ms) |
| 위험 | A가 비정상 종료 시 B가 오래 기다릴 수 있음 → **최대 재시도 횟수 + finally lockKey 삭제**로 완화 |

## 채택한 설계: 방안 B + 재시도 제한

```
lock key  = "post:likes:{id}:lock"  — 작업 중 표시, 짧은 TTL (10s)
init key  = "post:likes:{id}:init"  — 완료 표시, TTL 없음 (영구)
```

**ensureBootstrap 흐름:**

```
1. initKey 존재하면 → return (완료됨)
2. lockKey SETNX 성공 → {
     likeKey 먼저 삭제 (stale 데이터 방지)
     DB 조회 → Redis 적재
     initKey 영구 저장
   } finally {
     lockKey 삭제 (예외 발생해도 반드시 해제)
   }
3. lockKey SETNX 실패 → 최대 5회, 100ms 간격으로 initKey 재확인
   - initKey 나타나면 → return (정상 통과)
   - 재시도 초과 → throw EBException(REDIS_BOOTSTRAP_TIMEOUT)
     (그냥 return하면 빈 Redis에 toggle → 원래 버그 재발)
```

**왜 likeKey를 먼저 지우는가:**

bootstrap이 중간에 실패하면 likeKey에 부분 데이터가 남는다.

```
1차 bootstrap: DB에 member 1, 2, 3 좋아요 있음
  → SADD(1, 2) 후 서버 예외 → initKey 미세팅
  → Redis: {1, 2}  /  DB: {1, 2, 3}  ← stale
```

2차 bootstrap이 SADD만 하면 기존 {1, 2}에 덧붙여진다.
만약 그 사이 member 1, 2가 좋아요를 취소했다면 DB는 {3}인데 Redis는 {1, 2, 3}이 된다.
lock을 잡은 순간 likeKey를 먼저 delete하고 DB 기준으로 완전히 새로 적재해야
이전 실패의 잔재를 깔끔히 제거할 수 있다.

**왜 finally로 lockKey를 해제하는가:**

DB 조회나 Redis 적재 중 예외가 발생하면 lockKey가 남는다.
lockKey가 살아있으면 후속 요청은 모두 "retry 후 타임아웃 예외"로 실패한다.
finally 블록은 성공/실패 무관하게 반드시 lockKey를 삭제해 이 상황을 방지한다.
TTL(10s)은 finally 자체가 실패하는 극단적 케이스의 최후 안전망이다.

**왜 재시도 초과 시 return이 아니라 예외를 던지는가:**

return하면 B는 빈 Redis 또는 stale Redis에 그대로 toggle을 실행한다.

```
B: retry 5회 → initKey 미등장 → return (통과)
B: toggle 실행 → SISMEMBER → 0 (없음) → SADD
B: PostLikeService → DB save() 실행
실제 DB: 이미 좋아요 있음 → 중복 insert 또는 restore 로직 오염
```

이는 1번 문제(빈 Redis에 toggle)와 완전히 동일한 버그가 재발하는 것이다.
재시도 로직을 구현한 의미가 사라진다.
EBException을 던지면 클라이언트에게 "일시적으로 처리 불가"를 명확히 알릴 수 있고,
잘못된 상태로 DB를 오염시키는 것보다 훨씬 안전하다.

## 트레이드오프 요약

| | 방안 A | 방안 B (채택) |
|--|--------|--------------|
| 정합성 | 최초 1회 불일치 가능 | 보장 |
| 응답속도 | 빠름 | 최대 500ms 지연 가능 |
| 구현 복잡도 | 단순 | 보통 (retry + finally) |
| DB 오염 위험 | 있음 | 없음 |

## 한계 및 개선 가능성

- 재시도 초과 fallback 경우 여전히 불일치 가능 (극히 드문 경우)
- 더 강한 보장이 필요하다면: bootstrap 미완료 상태에서는 Redis 대신 DB를 직접 조회하는 fallback 경로 추가
- 분산 환경(다중 서버)에서는 Redisson 같은 분산 락 라이브러리 사용 권장
