# Redis Set 기반 Post Like 설계 (v2 — 정합성 보강)

## 목표

`post.like_count` 컬럼의 Lost Update 문제를 제거하고, Redis Set + Lua script로 원자적 좋아요 토글을 구현한다. DB는 정합성 원장, Redis는 빠른 토글/조회 캐시.

---

## 결정 사항

| 항목 | 결정 | 근거 |
|---|---|---|
| Redis 역할 | 캐시 (원자적 토글, 빠른 조회) | DB는 원장 유지로 영속성/정합성 보장 |
| DB 역할 | 정합성 원장 (`post_like`) | unique constraint로 중복 방지 최종 보루 |
| 토글 원자성 | Redis Lua script | SISMEMBER → SADD/SREM 사이 race 차단 |
| 동기화 순서 | **Redis 먼저 → DB** | Lua로 결정된 결과를 DB에 반영 |
| 보상 처리 | DB 실패 시 Redis 역연산 | Best-effort, 실패 시 스케줄러 재조정 |
| likeCount 조회 | Redis `SCARD` | 장애 시 DB `COUNT` fallback |
| Bootstrap | Lazy + `SETNX` 가드 | 메모리 효율, 동시 bootstrap race 차단 |
| TTL | 없음 + `allkeys-lru` | 활성 게시글만 자연히 유지 |
| `post.like_count` 컬럼 | 제거 | Redis SCARD가 역할 대체 |

---

## Redis Key 규칙

```
post:likes:{postId}          Set<memberId>   좋아요 회원 ID 집합
post:likes:{postId}:init     "1"             bootstrap 완료 마커
post:likes:{postId}:lock     "1"             bootstrap 작업 락 (SET NX EX 10)
```

빈 Set은 SCARD=0이지만 Redis에서 빈 Set은 키가 없는 것과 구분되지 않는다. 그래서 `:init` 마커가 필수다 (cache miss와 좋아요 0개 구분).

---

## 컴포넌트 구조

```
postlike/
├── service/
│   ├── PostLikeService.java          기존, DB + 흐름 제어
│   └── PostLikeCacheService.java     신규, Redis 전담
golbal/
└── config/
    └── RedisConfig.java              신규, RedisTemplate + DefaultRedisScript Bean
src/main/resources/
└── scripts/
    └── post-like-toggle.lua          Lua 스크립트 (clamspathResource)
```

### 클래스 책임

| 클래스 | 책임 |
|---|---|
| `RedisConfig` | `RedisTemplate<String, String>`, `DefaultRedisScript<List>` 등록 |
| `PostLikeCacheService` | Lua 토글, Lazy bootstrap (SETNX), SCARD 조회, fallback |
| `PostLikeService` | 트랜잭션 경계, DB sync, Redis 실패 시 보상 |

---

## 정합성 전략 (Redis ↔ DB)

### 동기화 순서 — Redis First

```
1. PostLikeCacheService.toggle()       Lua 원자 토글 → {liked, likeCount}
2. PostLikeService DB sync             liked 결과대로 INSERT/restore/soft-delete
3. 응답 반환                            Lua가 반환한 likeCount 그대로
```

### 실패 시나리오와 대응

| # | 시나리오 | 대응 |
|---|---|---|
| 1 | Redis 실패 (1단계) | DB-only 경로로 fallback (아래 fallback 모드 참조) |
| 2 | Lua 성공, DB 실패 | **보상 Lua 호출** (역연산 SADD/SREM) + 로그 |
| 3 | 보상 Lua도 실패 | dirty key 기록 → 스케줄러가 재조정 |
| 4 | Redis와 DB 둘 다 OK | 정상 |

### 보상 연산

성공한 토글의 정확한 역연산:

```text
좋아요 후 DB 실패 → SREM post:likes:{postId} {memberId}
취소 후 DB 실패   → SADD post:likes:{postId} {memberId}
```

`SREM`, `SADD`는 각각 단일 Redis 명령이라 별도 Lua 없이도 원자적이다.

### 정기 재조정 (선택, 운영용)

`@Scheduled`로 일정 주기마다 dirty postId의 DB → Redis 재bootstrap.
범위가 작으면 dirty 리스트를 Redis Set `post:likes:dirty`에 SADD하고 스케줄러가 비우는 방식.

---

## 데이터 흐름

### 토글

```
[Controller] like(postId, memberId)
        │
        ▼
[PostLikeService.like()]
        │
        ├── PostLikeCacheService.ensureBootstrap(postId)   Lazy + SETNX
        │
        ├── PostLikeCacheService.toggle(postId, memberId)  Lua 실행
        │       returns (liked: int, likeCount: long)
        │
        ├── try {
        │     DB sync (liked에 따라 INSERT/restore/soft-delete)
        │   } catch (DataAccessException) {
        │     compensate(postId, memberId, liked)     Redis 역연산
        │     dirtySet.add(postId)                    재조정 후보
        │     throw original                          API 실패 응답
        │   }
        │
        ▼
   PostLikeResponse(liked, likeCount)
```

### Lazy Bootstrap (race-safe)

```
1. exists(post:likes:{postId}:init)?
       └─ true → return (이미 bootstrap됨)
2. SET post:likes:{postId}:lock "1" NX EX 10
       └─ false (이미 다른 요청이 bootstrap 중) → init 마커가 생길 때까지 짧게 retry
       └─ retry 초과 → REDIS_BOOTSTRAP_TIMEOUT
       └─ true (락 획득)
3. DEL post:likes:{postId}
4. DB: SELECT member_id FROM post_like WHERE post_id=? AND deleted=false
5. SADD post:likes:{postId} memberId...  (배치)
6. SET post:likes:{postId}:init "1"
7. finally: DEL post:likes:{postId}:lock
```

`lock`과 `init`의 의미를 분리해야 bootstrap 중인 요청이 빈 Set에서 토글하지 않는다.

### Redis 장애 Fallback 모드

Lua/Redis 호출에서 `RedisConnectionFailureException` 또는 timeout 발생 시:

```
1. 토글 결정 → DB findByPostAndMember(post, member) 결과로 분기
       있고 deleted=false → soft-delete, liked=false
       없거나 deleted=true → INSERT/restore, liked=true
2. likeCount → postLikeRepository.countByPostIdAndDeletedFalse(postId)
3. log.warn (Redis down, fallback to DB) — alert에 연결
4. dirty 표시 불가 (Redis 자체가 다운) → 복구 후 일괄 bootstrap
```

**fallback 모드 한계**: DB만으로 토글하면 동시 요청 시 같은 사용자의 중복 좋아요는 unique constraint가 막지만, 토글 결과(liked true/false)가 흔들릴 수 있다. fallback은 가용성 우선이며 정합성은 Redis 복구 후 재조정으로 회복한다.

---

## Lua 스크립트

### 메인 토글: `post-like-toggle.lua`

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

`KEYS`와 `ARGV` 구분 이유: Redis Cluster 환경에서 KEYS만이 슬롯 라우팅 대상이다. 현재 단일 노드라도 클러스터 호환을 위해 분리한다.

Spring `DefaultRedisScript<List>`로 등록하면 첫 호출 시 `SCRIPT LOAD`로 SHA1 캐싱, 이후 `EVALSHA`로 자동 전환된다.

---

## 제거/변경 상세

### Post.java — 제거
```java
private int likeCount = 0;
public void increaseLikeCount() { ... }
public void decreaseLikeCount() { ... }
```

### schema.sql — 제거
```sql
like_count INT NOT NULL DEFAULT 0,
```

기존 인덱스 중 `idx_post_like_count`가 있다면 같이 제거.

### PostLikeRepository.java — 추가
```java
long countByPostIdAndDeletedFalse(Long postId);

@Query("SELECT pl.member.id FROM PostLike pl WHERE pl.post.id = :postId AND pl.deleted = false")
List<Long> findMemberIdsByPostId(Long postId);

// re-like 시 deleted row 복구용
@Query("SELECT pl FROM PostLike pl WHERE pl.post.id = :postId AND pl.member.id = :memberId")
Optional<PostLike> findIncludingDeleted(Long postId, Long memberId);
```

좋아요 회원이 매우 많은 게시글을 위해 향후 `findMemberIdsByPostId`를 페이징 버전으로 교체할 수 있게 한다 (현재 EatBusan 규모에선 단일 쿼리 OK).

### PostLikeController.java — 응답 변경
```java
record PostLikeResponse(boolean liked, long likeCount) {}
// ResponseEntity<Void> → ResponseEntity<PostLikeResponse>
```

API 호환성: 현재 프론트가 미구현이므로 영향 없음. 추후 프론트 연동 시 응답 스펙을 문서화한다.

### build.gradle — 추가
```groovy
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

### application.properties — 추가 예시
```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=200ms
```

---

## 운영 정책

### TTL

좋아요 Set 자체에는 TTL을 두지 않는다. 활성 게시글의 Set이 만료되면 매번 bootstrap 비용이 발생한다. 대신 Redis `maxmemory`와 `allkeys-lru` 정책으로 메모리 한도를 관리한다.

### 메모리 산정

```
1 좋아요 ≈ 8 byte (Long memberId, intset 인코딩 시)
좋아요 회원 1000명 게시글 → 약 8KB
게시글 1만 개, 평균 100 좋아요 → 약 8MB
```

학습/포트폴리오 규모에선 무시 가능.

### Eviction 정책 권장

`redis.conf`:
```
maxmemory 256mb
maxmemory-policy allkeys-lru
```

근거: 좋아요 Set은 모두 캐시 성격(원장은 DB). 자주 안 보는 게시글의 Set이 evict돼도 다음 조회 시 lazy bootstrap으로 복구된다.

### Redis Persistence

학습 환경: RDB only (기본값 유지). 데이터 손실 허용 — 원장이 DB이므로 Redis 손실은 다음 bootstrap으로 복구.
운영 환경: AOF + RDB 병행 고려.

---

## 의사결정 근거

### 왜 Lazy Bootstrap인가 (인스타처럼 항상 노출되는데도)

- 첫 조회 시 한 번만 DB에서 로드 → 이후는 Redis HIT
- Redis가 재시작되지 않는 한 활성 게시글의 Set은 메모리에 상주
- 비활성 게시글은 자연히 evict되어 메모리 효율적
- 서버 기동 시간 짧음 (모든 post_like를 한 번에 안 읽음)

서비스 초기 진입 시 cold cache로 인한 DB 부하가 우려되면, 핫 게시글만 사전 warming하는 방식이 별도로 가능.

### 왜 Redis First + 보상 패턴인가

대안 비교:

| 패턴 | 장점 | 단점 |
|---|---|---|
| **Redis first + 보상 (채택)** | Lua로 토글 race 차단, DB는 결과 반영 | DB 실패 시 보상 필요 |
| DB first + 이벤트 발행 | DB가 원장 역할 충실 | 토글 race는 DB lock 필요 |
| 양방향 트랜잭션 | 강한 정합성 | XA/2PC 복잡도 ↑↑ |

EatBusan 규모에서 Redis first는 토글 race 차단의 가장 단순한 방법. DB 실패는 드물고 보상 + 재조정으로 회복 가능.

---

## 테스트 전략

| 테스트 클래스 | 범위 | 도구 |
|---|---|---|
| `PostLikeCacheServiceTest` | Lua 토글, bootstrap SETNX 동작 | Testcontainers Redis or Embedded Redis |
| `PostLikeServiceIntegrationTest` | DB+Redis 통합, re-like(deleted 복구) | Testcontainers MySQL + Redis |
| `PostLikeConcurrencyTest` (수정) | 30 스레드 동시 토글, post_like 카운트 == SCARD | 기존 테스트 수정 |
| `PostLikeFallbackTest` | Redis 컨테이너 stop 후 API 응답 | Testcontainers (DockerClient로 stop) |
| `PostLikeCompensationTest` | DB INSERT 실패 mock → 보상 Lua가 SREM 호출 확인 | Mockito + Embedded Redis |

### Lua 단위 테스트

`redis-cli --eval scripts/post-like-toggle.lua post:likes:1 , 100` 형식으로 수동 검증 가능.
자동화는 `RedisTemplate.execute(script, keys, args)`를 통해 통합 테스트에서 다룬다.

### 기존 PostLikeConcurrencyTest 수정 포인트

```java
// 제거: int likeCount = postRepository.findById(postId).orElseThrow().getLikeCount();
// 추가: long actualPostLikeCount = postLikeRepository.countByPostIdAndDeletedFalse(postId);
//       long redisScard = postLikeCacheService.likeCount(postId);
//       assertThat(redisScard).isEqualTo(actualPostLikeCount);
```

---

## 완료 기준

- [x] 30 스레드 동시 토글에서 `SCARD == post_like COUNT`
- [x] Redis 연결 불가 상태에서도 좋아요 API가 5xx 없이 응답
- [x] DB INSERT 실패 mock 시 Redis 보상 연산 호출
- [x] `post.like_count` 컬럼 및 관련 코드가 schema/엔티티/서비스 어디에도 없음
- [x] Lua script가 SHA1로 캐싱되어 `EVALSHA`로 실행됨

---

## 미해결 / 추후 과제 (스펙 범위 밖)

- 좋아요 알림(`notification` 테이블) 연동 시점 결정
- 게시글 삭제 시 `post:likes:{postId}` 정리 트리거
- 회원 탈퇴 시 모든 게시글에서 해당 memberId 제거 (`SREM`)
- 좋아요 랭킹 보드 (`ZSET` 별도 설계)

---

## 참고 — 공식 문서

- Redis Sets: https://redis.io/docs/latest/develop/data-types/sets/
- Redis Lua scripting intro: https://redis.io/docs/latest/develop/interact/programmability/eval-intro/
- Redis eviction (maxmemory, LRU/LFU): https://redis.io/docs/latest/develop/reference/eviction/
- Spring Data Redis — Scripting: https://docs.spring.io/spring-data/redis/reference/redis/scripting.html
