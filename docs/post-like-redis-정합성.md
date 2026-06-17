# Post 좋아요 — Redis + DB 정합성 설계 (면접 치트시트)

## 1. 전체 흐름 (한 번의 좋아요 클릭)

```
[유저 좋아요 클릭]
   │
   ▼
① ensureBootstrap(postId)   Redis Set 비었으면 DB에서 적재 (SET NX 락으로 1개만)
   │
   ▼
② toggle (Lua, 원자적)       있으면 SREM / 없으면 SADD + SCARD → (liked, count)
   │                         ※ 이 순간 Redis는 이미 바뀜
   ▼
③ syncToDb + flush()         DB에 insert/restore/delete, flush로 즉시 실행
   │
   ├─ 성공 ─▶ 응답 (liked, count)       ✅ Redis = DB 일치
   │
   └─ 실패 ─▶ compensate() → Redis 되돌림 → throw (클라엔 실패 통보)
                  └─ 보상도 실패 ─▶ log.error 후 포기   ❌ 영구 불일치

[Redis 자체가 죽음] ─▶ fallbackToDb()   DB로만 토글 (graceful degradation)
```

## 2. 부품별 "왜"

| 부품 | 왜 존재하나 |
|---|---|
| Redis Set | 좋아요 토글·카운트가 빈번. SISMEMBER/SCARD/SADD/SREM 전부 O(1). DB COUNT 부하 offload |
| DB(PostLike) | **영속 기록 = 최종 원본.** Redis는 휘발성이라 재구성 기준이 필요 |
| Lua script | check-then-act(SISMEMBER→SADD/SREM)를 **원자화**. Redis 싱글스레드라 스크립트 통째 무중단 실행 |
| bootstrap + initKey | Redis 비었을 때 DB→Redis 적재. Redis는 "빈 Set"과 "키 없음"을 구분 못 해서 완료표시(initKey) 필요 |
| lock (SET NX) | 동시 요청이 다 같이 적재하는 thundering herd 방지. 하나만 적재, 나머지는 대기 |
| flush() | JPA는 SQL을 commit까지 미룸(쓰기 지연). flush로 **지금** 실행시켜 DB 예외를 catch에서 잡아 보상 가능 |
| compensate | Redis는 이미 바뀜 + 클라엔 실패 통보 → 되돌려야 정합성. (유실 아니라 정합성 문제) |
| fallback | Redis 다운 시 DB로 동작. 가용성 우선 |

## 3. 정합성 모델 (핵심)

- Redis와 DB는 **별개 시스템, 공유 트랜잭션 없음 → atomic 아님.** best-effort + 보상.
- **깨지는 3지점:**
  1. **보상 자체 실패** → 영구 불일치 (코드가 log.error로 인정).
  2. **toggle~sync 윈도우**에 다른 요청이 중간 상태를 읽음 (시스템 간 dirty read).
  3. **toggle 후 sync 전 프로세스 크래시** → Redis 바뀌고 DB 안 바뀜, 보상도 못 함.
- DB = 최종 원본. 다음 bootstrap에서 DB 기준 재구성 → 어긋난 Redis는 결국 수렴(eventual). 단 그 전까지는 틀림.

## 4. 검증 (`PostLikeConcurrencyTest`)

- 30스레드 `CountDownLatch`로 동시 발사 → `Redis SCARD == DB COUNT == 30` 단언.
- Redis 비우고 DB만 둔 뒤 → `checkLiked`/`likeCount`가 DB 기준으로 복구되는지 검증.

## 5. 면접 답변 (60초)

> "좋아요는 토글·카운트가 빈번해서 Redis Set으로 처리했습니다. 토글은 SISMEMBER 후 SADD/SREM이라 둘 사이 race가 생겨서, Lua 스크립트로 원자화했습니다. Redis가 휘발성이라 DB를 영속 원본으로 두고, 첫 접근 시 DB→Redis로 적재하되 SET NX 락으로 동시 적재를 막았습니다. 토글 후 DB 동기화가 실패하면 클라이언트엔 실패를 알리고 Redis를 보상으로 되돌립니다. 다만 Redis·DB는 공유 트랜잭션이 없어 완벽한 원자성은 아니고, 보상 실패나 프로세스 크래시 시 불일치가 남을 수 있는 best-effort 설계라는 한계를 알고 있습니다. 동시성은 30스레드 테스트로 SCARD와 DB COUNT 일치를 검증했습니다."
