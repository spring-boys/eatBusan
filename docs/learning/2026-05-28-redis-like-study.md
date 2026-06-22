# Redis 좋아요 설계를 위한 학습 가이드

이 문서는 `docs/superpowers/specs/2026-05-28-redis-like-design.md`를 구현하기 전, Redis 초보자가 필수로 학습해야 할 개념을 공식 문서 기준으로 정리한다. 챕터 순서대로 읽고, 각 챕터의 "공식 문서"를 직접 한 번씩 열어보는 것이 핵심이다.

---

## 학습 순서 한눈에 보기

```
1. Redis가 무엇인가
2. 자료구조 — String vs Set
3. 원자성 — 단일 명령 / Pipeline / MULTI / Lua
4. Lua Scripting — EVAL, EVALSHA, KEYS/ARGV
5. Spring Data Redis — RedisTemplate, DefaultRedisScript
6. 캐시 패턴 — Cache-Aside, Write-Through
7. 영속성 — RDB / AOF
8. 메모리 관리 — maxmemory, Eviction Policy
9. 우리 설계와의 매핑 (총정리)
```

---

## 1. Redis가 무엇인가

### 핵심
- In-memory data structure store
- Key-Value 기반이지만 Value 자리에 String, List, Set, Hash, Sorted Set, Stream 등 다양한 자료구조가 들어간다
- **싱글 스레드 명령 처리** — 한 번에 한 명령씩 처리하므로 단일 명령은 원자적이다

### 우리 프로젝트에서 왜 쓰는가
- `post.like_count` 컬럼은 Read-Modify-Write 패턴이라 동시 요청 시 Lost Update 발생
- Redis는 단일 스레드 처리 덕분에 `SADD`, `SREM`, `SCARD` 같은 명령이 race condition 없이 동작

### 공식 문서
- 개요: https://redis.io/docs/latest/
- "About Redis" introduction: https://redis.io/docs/latest/develop/

### 학습 체크포인트
- [ ] 왜 Redis 단일 명령은 race condition이 없는지 한 문장으로 설명할 수 있는가
- [ ] Redis가 "데이터베이스"인지 "캐시"인지에 대한 답이 "둘 다 가능"임을 안다

---

## 2. 자료구조 — String vs Set

### String
- 단순 key → value (문자열, 숫자, 직렬화된 객체)
- 대표 명령: `SET`, `GET`, `INCR`, `DECR`
- 카운터 용도로 자주 쓰임

```
SET post:view:1:count 0
INCR post:view:1:count        → 1
INCR post:view:1:count        → 2
```

### Set
- 중복 없는 원소의 집합 (순서 없음)
- 대표 명령:

| 명령 | 의미 | 시간복잡도 |
|---|---|---|
| `SADD key member` | 원소 추가 | O(1) |
| `SREM key member` | 원소 제거 | O(1) per element |
| `SISMEMBER key member` | 포함 여부 (0/1) | O(1) |
| `SCARD key` | 원소 개수 | O(1) |
| `SMEMBERS key` | 모든 원소 반환 | O(N) — 큰 Set엔 위험 |
| `SSCAN` | 점진적 순회 | 페이징형, 안전 |

### 왜 좋아요는 Set인가
- "어떤 회원이 눌렀는가"를 알아야 하므로 단순 카운터(String) 부족
- 중복 좋아요를 Redis 레벨에서 자연히 차단 (Set은 중복 허용 안 함)
- `SCARD`로 좋아요 수, `SISMEMBER`로 내가 눌렀는지를 즉시 답할 수 있음

### 공식 문서
- Strings: https://redis.io/docs/latest/develop/data-types/strings/
- Sets: https://redis.io/docs/latest/develop/data-types/sets/

### 학습 체크포인트
- [ ] `SADD` 같은 원소를 두 번 추가해도 Set 크기가 1인 이유를 안다
- [ ] `SMEMBERS`가 큰 Set에서 위험한 이유, 대안인 `SSCAN`을 안다

---

## 3. 원자성 — 단일 명령 / Pipeline / MULTI / Lua

Redis에서 "여러 단계의 동작을 race 없이" 처리하는 4가지 방법:

| 방식 | 원자성 | 용도 |
|---|---|---|
| 단일 명령 | 보장 (단일 스레드) | `INCR`, `SADD` 등 한 명령으로 끝날 때 |
| Pipeline | 보장 안 됨 | 네트워크 왕복 줄이기 (성능용) |
| MULTI/EXEC | 보장 (트랜잭션) | 명령 큐잉 후 한 번에 실행. 단, 조건 분기 불가 |
| **Lua script** | **보장** | **조건 분기/계산 포함한 다단계** |

### MULTI vs Lua
- MULTI: `EXEC` 직전까지 명령을 큐잉. 중간 결과로 다음 명령을 바꿀 수 없음
- Lua: 스크립트 내부에서 `redis.call` 결과로 if/for 가능. 우리가 원하는 토글 패턴은 Lua만 가능

```
좋아요 토글:
  IF SISMEMBER == 1 THEN SREM ELSE SADD
```

MULTI로는 `SISMEMBER` 결과를 보고 분기할 수 없다. → **Lua 필수.**

### 공식 문서
- Programmability (개요): https://redis.io/docs/latest/develop/interact/programmability/
- Transactions (MULTI/EXEC): https://redis.io/docs/latest/develop/interact/transactions/

### 학습 체크포인트
- [ ] 단일 스레드 모델이 곧 단일 명령 원자성을 의미함을 안다
- [ ] MULTI로 토글이 안 되는 이유를 설명할 수 있다

---

## 4. Lua Scripting — EVAL, EVALSHA, KEYS/ARGV

### EVAL
스크립트를 매번 풀 텍스트로 보냄.
```
EVAL "스크립트본문" <KEYS 개수> key1 key2 ... arg1 arg2 ...
```

### EVALSHA
스크립트를 한 번 `SCRIPT LOAD`로 등록 후 SHA1 해시로만 호출.
```
EVALSHA <sha1> <KEYS 개수> key1 ... arg1 ...
```
네트워크 페이로드가 작아 운영 환경에서 권장.

### KEYS vs ARGV — 왜 분리하는가
- `KEYS`: Redis Cluster에서 슬롯 라우팅 대상이 되는 키 목록
- `ARGV`: 그 외 임의 값 (값, 옵션, 멤버 ID 등)

같은 노드의 키들만 한 스크립트에 묶을 수 있도록 KEYS는 명시적으로 분리해야 한다. 단일 노드라도 클러스터 호환을 위해 분리하는 게 표준.

### 우리 토글 스크립트

```lua
local key    = KEYS[1]    -- post:likes:{postId}
local member = ARGV[1]    -- memberId

if redis.call('SISMEMBER', key, member) == 1 then
    redis.call('SREM', key, member)
    return {0, redis.call('SCARD', key)}
else
    redis.call('SADD', key, member)
    return {1, redis.call('SCARD', key)}
end
```

- `KEYS[1]`: postId가 포함된 키 → 클러스터에서 같은 슬롯
- `ARGV[1]`: memberId → 단순 값

반환은 Lua 테이블 `{liked, likeCount}`. Spring에서는 `List<Long>`으로 받는다.

### 공식 문서
- EVAL intro: https://redis.io/docs/latest/develop/interact/programmability/eval-intro/
- EVAL command: https://redis.io/docs/latest/commands/eval/
- EVALSHA command: https://redis.io/docs/latest/commands/evalsha/
- SCRIPT LOAD: https://redis.io/docs/latest/commands/script-load/

### 학습 체크포인트
- [ ] EVAL과 EVALSHA의 차이, 언제 어느 쪽을 쓰는지
- [ ] KEYS와 ARGV를 분리해야 하는 이유 (클러스터 슬롯)
- [ ] Lua 안에서 `redis.call`과 `redis.pcall`의 차이를 안다 (에러 처리)

---

## 5. Spring Data Redis — RedisTemplate, DefaultRedisScript

### RedisTemplate
- Redis 명령을 Java에서 호출하는 추상화
- key/value 직렬화 방식을 설정해야 함 (`StringRedisSerializer` 권장)

```java
@Bean
RedisTemplate<String, String> redisTemplate(RedisConnectionFactory cf) {
    var template = new RedisTemplate<String, String>();
    template.setConnectionFactory(cf);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new StringRedisSerializer());
    return template;
}
```

### DefaultRedisScript
- Lua 스크립트를 Bean으로 등록 → SHA1 자동 계산 → 첫 호출은 EVAL, 다음부터 EVALSHA 자동 전환
- `setResultType(List.class)`로 반환 타입 지정

```java
@Bean
RedisScript<List> postLikeToggleScript() {
    var script = new DefaultRedisScript<List>();
    script.setLocation(new ClassPathResource("scripts/post-like-toggle.lua"));
    script.setResultType(List.class);
    return script;
}
```

### 실행

```java
List<Long> result = redisTemplate.execute(
    postLikeToggleScript,
    List.of("post:likes:" + postId),    // KEYS
    String.valueOf(memberId)             // ARGV
);
long liked     = result.get(0);
long likeCount = result.get(1);
```

### 공식 문서
- Spring Data Redis Reference: https://docs.spring.io/spring-data/redis/reference/
- Scripting: https://docs.spring.io/spring-data/redis/reference/redis/scripting.html
- RedisTemplate: https://docs.spring.io/spring-data/redis/reference/redis/template.html

### Lua 반환 타입 함정

Lua는 number를 반환하지만 Spring은 `DefaultRedisScript<List>`로 받을 때 일반적으로 `List<Long>`으로 매핑한다. 다음 함정에 주의:

- `setResultType(Long.class)`: 단일 number 반환용. Lua가 `return {0, 1}` 같은 테이블이면 캐스트 에러
- `setResultType(List.class)`: 테이블 반환용. 원소는 `Long`으로 들어옴 (Integer 아님)
- 반환값을 `Integer`로 받으려 하면 ClassCastException 발생 가능 → 항상 `Long`으로

```java
List<Long> result = ...;   // OK
long liked = result.get(0);   // 0L or 1L
```

### 학습 체크포인트
- [ ] `DefaultRedisScript`가 자동으로 SHA1을 캐싱한다는 사실
- [ ] Serializer를 잘못 설정하면 키 이름이 깨져서 키가 두 개로 분리될 수 있다는 것 (`StringRedisSerializer` vs `JdkSerializationRedisSerializer`)
- [ ] Lua가 반환한 number는 Spring에서 `Long`으로 받아야 한다는 사실

---

## 6. 캐시 패턴 — Cache-Aside, Write-Through

### Cache-Aside (Lazy loading)
```
read:  cache 확인 → miss → DB 조회 → cache 채움 → 반환
write: DB 갱신 → cache invalidate (또는 갱신)
```
우리 Bootstrap이 이 패턴이다.

### Write-Through
```
write: cache 갱신 → cache가 DB까지 동기적으로 갱신
read:  cache에서 바로 반환
```

### Write-Behind (Write-Back)
```
write: cache 갱신 → 비동기로 DB 반영
```
viewCount용 패턴 (REDIS_LIKE_GOAL.md 참조).

### 우리 좋아요는 "Redis First + DB Sync"
- 정확히는 변형된 Write-Through
- Redis 토글 결과를 DB에 즉시 반영, 실패 시 보상

### 학습 참고
- AWS docs (한국어 자료 있음): https://aws.amazon.com/ko/caching/best-practices/
- Microsoft Cloud Design Patterns — Cache-Aside: https://learn.microsoft.com/en-us/azure/architecture/patterns/cache-aside

### 학습 체크포인트
- [ ] Cache-Aside와 Write-Through의 차이, 어떤 상황에서 어느 쪽을 쓰는지
- [ ] 우리 설계가 왜 "Write-Through 변형 + Lazy Bootstrap"인지 설명할 수 있다

---

## 7. 영속성 — RDB / AOF

Redis는 메모리 기반이지만 디스크에 저장하는 두 가지 방식이 있다.

### RDB (Redis DataBase)
- 주기적으로 메모리 전체를 스냅샷으로 저장 (`dump.rdb` 파일)
- 압축 효율 좋고 복구 빠름
- 마지막 스냅샷 이후 데이터는 손실 가능

### AOF (Append-Only File)
- 모든 쓰기 명령을 로그처럼 append
- 데이터 손실 가능성 작음 (정책에 따라 매초/매명령)
- 파일이 커짐, 재시작 시 명령을 다시 실행하므로 복구 느림

### 우리 설계에서의 선택
- 학습/개발 환경: RDB 기본값
- 이유: 원장은 DB이고 Redis는 캐시이므로 일부 손실은 다음 Bootstrap으로 복구 가능

### 공식 문서
- Persistence: https://redis.io/docs/latest/operate/oss_and_stack/management/persistence/

### 학습 체크포인트
- [ ] RDB와 AOF의 차이, trade-off
- [ ] 우리 프로젝트에서 영속성 손실이 치명적이지 않은 이유

---

## 8. 메모리 관리 — maxmemory, Eviction Policy

### maxmemory
Redis가 사용할 최대 메모리. 도달하면 eviction policy 적용.
```
CONFIG SET maxmemory 256mb
```

### Eviction Policy (정책 8가지 중 핵심)

| 정책 | 동작 | 추천 상황 |
|---|---|---|
| `noeviction` | 메모리 초과 시 쓰기 에러 반환 | 절대 잃으면 안 되는 데이터 |
| `allkeys-lru` | 모든 키 중 최근 안 쓴 것부터 제거 | **일반 캐시** (우리 권장) |
| `allkeys-lfu` | 모든 키 중 빈도 낮은 것 제거 | 접근 패턴이 한쪽으로 쏠릴 때 |
| `volatile-lru` | TTL 있는 키만 LRU 제거 | 일부 키만 캐시인 혼합 운영 |
| `volatile-ttl` | TTL 가까운 키부터 제거 | 만료 임박 키 우선 정리 |

### 우리 권장 — `allkeys-lru`
- 좋아요 Set은 전부 캐시 (원장은 DB)
- 안 쓰는 게시글의 Set이 자연히 evict됨
- TTL을 안 두는 대신 LRU가 메모리 관리

### `maxmemory-samples`
LRU는 진짜 LRU가 아니라 샘플링 기반 근사. `maxmemory-samples 5`가 기본이고 늘리면 정확도 ↑, CPU 비용 ↑.

### 공식 문서
- Eviction: https://redis.io/docs/latest/develop/reference/eviction/

### 학습 체크포인트
- [ ] 8가지 eviction policy 중 우리가 왜 `allkeys-lru`를 선택했는지
- [ ] TTL을 두지 않는데도 메모리 관리가 가능한 이유

---

## 9. 우리 설계와의 매핑 (총정리)

스펙의 각 부분이 위 학습 내용 어디에 대응되는지:

| 스펙 항목 | 학습 챕터 | 한 줄 요약 |
|---|---|---|
| Redis Set `post:likes:{postId}` | 2 | 중복 없는 회원 ID 집합 |
| Lua 토글 스크립트 | 3, 4 | SISMEMBER → SREM/SADD 원자 실행 |
| `:init` 마커 + SETNX | 3 | bootstrap race 차단 |
| Lazy Bootstrap | 6 | Cache-Aside 패턴 |
| Redis First + DB Sync | 6 | Write-Through 변형 |
| 보상 Lua | 4 | DB 실패 시 Redis 역연산 |
| `RedisTemplate` + `DefaultRedisScript` | 5 | Spring Data Redis로 Lua 실행 |
| `allkeys-lru` + maxmemory | 8 | TTL 없이 메모리 관리 |
| RDB only (학습 환경) | 7 | 캐시 손실 허용, DB가 원장 |
| Fallback to DB COUNT | 6 | Redis 장애 시 가용성 우선 |

---

## 학습 순서 권장

1. **공식 문서 1회독** (각 챕터의 링크) — 영어가 어렵다면 한국어 자료를 보조로
2. **로컬에서 명령 직접 실행** — `redis-cli`로 `SADD`, `SREM`, `SCARD`, `SISMEMBER` 손에 익히기
3. **Lua 스크립트 EVAL 수동 실행** — `redis-cli --eval scripts/post-like-toggle.lua post:likes:1 , 100`
4. **Spring에서 RedisTemplate 한 줄짜리 예제** — 키 SET/GET부터
5. **이 스펙 구현** — 챕터 9 매핑 표를 참고하며 한 단계씩

---

## redis-cli 환경 준비 (WSL2 Ubuntu)

```bash
# 설치
sudo apt update
sudo apt install -y redis-server

# 시작 (WSL2는 systemd 활성화 여부에 따라 다름)
sudo service redis-server start
# 또는
sudo systemctl start redis-server

# 동작 확인
redis-cli ping        # → PONG
```

`redis-cli` 단독 접속:
```bash
redis-cli              # 기본 localhost:6379
redis-cli -h <host> -p <port>
```

## redis-cli 빠른 연습 시나리오

```bash
# 1. 좋아요 추가
SADD post:likes:1 100
SADD post:likes:1 200

# 2. 좋아요 수 확인
SCARD post:likes:1        # → 2

# 3. 특정 사용자 좋아요 눌렀는지
SISMEMBER post:likes:1 100   # → 1
SISMEMBER post:likes:1 999   # → 0

# 4. 취소
SREM post:likes:1 100
SCARD post:likes:1        # → 1

# 5. 중복 SADD 동작 확인
SADD post:likes:1 200     # → 0 (이미 있음)
SADD post:likes:1 300     # → 1 (새로 추가)

# 6. 모든 좋아요 회원 (작은 Set만)
SMEMBERS post:likes:1
```

---

## 학습 종료 후 자기점검

설계 문서를 다시 읽었을 때 다음 질문에 답할 수 있어야 한다:

1. Lua script가 없어도 Redis Set만으로 race condition을 막을 수 있는가? (답: 토글은 못 막는다)
2. `:init` 마커 없이 `EXISTS post:likes:{postId}`로 bootstrap 여부를 판단하면 안 되는 이유? (답: 좋아요 0개와 미초기화 구분 불가)
3. Redis 장애 시 fallback에서도 likeCount가 정확한가? (답: 동시 토글 시 흔들릴 수 있음, 가용성 우선)
4. `post.like_count` 컬럼이 사라지면 좋아요 수는 어디서 오는가? (답: 정상 — Redis SCARD, 장애 — DB COUNT)
5. Redis가 재시작되면 좋아요 데이터는 사라지는가? (답: 사라지지만 DB에서 다음 요청 시 Lazy Bootstrap)

---

## 참고 문서 일괄 목록

### Redis 공식
- 전체: https://redis.io/docs/latest/
- Sets: https://redis.io/docs/latest/develop/data-types/sets/
- Strings: https://redis.io/docs/latest/develop/data-types/strings/
- Transactions: https://redis.io/docs/latest/develop/interact/transactions/
- Programmability: https://redis.io/docs/latest/develop/interact/programmability/
- EVAL intro: https://redis.io/docs/latest/develop/interact/programmability/eval-intro/
- EVAL: https://redis.io/docs/latest/commands/eval/
- EVALSHA: https://redis.io/docs/latest/commands/evalsha/
- Persistence: https://redis.io/docs/latest/operate/oss_and_stack/management/persistence/
- Eviction: https://redis.io/docs/latest/develop/reference/eviction/

### Spring Data Redis 공식
- Reference: https://docs.spring.io/spring-data/redis/reference/
- Template: https://docs.spring.io/spring-data/redis/reference/redis/template.html
- Scripting: https://docs.spring.io/spring-data/redis/reference/redis/scripting.html

### 캐시 패턴
- AWS Caching Best Practices: https://aws.amazon.com/caching/best-practices/
- Microsoft Cache-Aside Pattern: https://learn.microsoft.com/en-us/azure/architecture/patterns/cache-aside
