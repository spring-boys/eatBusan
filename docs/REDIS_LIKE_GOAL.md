# Redis 기반 조회수 / COUNT 기반 좋아요 수 설계

## 결정 배경

post 테이블의 likeCount와 viewCount는 JPA Dirty Checking 방식으로 갱신되고 있었다.
이 방식은 read → modify → write 패턴으로, 동시 요청이 들어오면 Lost Update가 발생한다.

두 카운터의 성격이 달라 해결 방식도 다르게 가져간다.

| 카운터 | 성격 | 해결 방식 |
|---|---|---|
| likeCount | 사용자 명시적 행동, 빈도 낮음 | COUNT 쿼리 |
| viewCount | 페이지 열 때마다 발생, 빈도 높음 | Redis INCR |

---

## likeCount — COUNT 쿼리

### 결정 이유

좋아요는 사용자가 버튼을 직접 누르는 행동이라 조회 대비 빈도가 낮다.
post 테이블에 likeCount 컬럼을 두지 않고, 필요한 시점에 post_like 테이블을 직접 COUNT한다.
post_like INSERT는 각 스레드가 서로 다른 row에 독립적으로 수행하므로 race condition이 없다.

### 변경 내용

- post 테이블에서 like_count 컬럼 제거, schema.sql 반영
- Post 엔티티에서 likeCount 필드, increaseLikeCount(), decreaseLikeCount() 제거
- PostLikeService에서 post.increaseLikeCount(), post.decreaseLikeCount() 호출 제거
- 게시글 조회 응답에 likeCount가 필요한 경우 postLikeRepository.countByPostIdAndDeletedFalse(postId) 사용

### 주의사항

게시글 목록 조회에서 N+1이 발생하지 않도록 JOIN + COUNT를 한 쿼리로 묶어야 한다.
post_like(post_id, deleted) 인덱스가 있어야 COUNT가 빠르게 동작한다.

---

## viewCount — Redis INCR

### 결정 이유

조회수는 게시글을 열 때마다 증가하므로 likeCount보다 훨씬 빈번하다.
동시 접근이 많을수록 Lost Update 피해가 커지기 때문에 Redis의 원자 연산이 필요하다.
Redis INCR은 read → modify → write를 단일 원자 연산으로 처리하므로 race condition이 없다.

### Redis Key 설계

글별 조회수 Counter:

```text
post:view:{postId}:count
```

자료구조: String integer

용도:
- INCR: 조회수 증가
- GET: 조회수 빠른 반환

동기화 대상 Dirty Set:

```text
post:view:dirty
```

자료구조: Set\<Long postId\>

용도:
- 조회가 발생한 postId 기록
- 스케줄러가 이 Set을 읽어 DB와 동기화

### 동작 흐름

조회 처리:

```text
1. 게시글 조회 시 INCR post:view:{postId}:count
2. SADD post:view:dirty postId
3. 조회수 응답은 Redis 값 반환
```

DB 동기화 (Write Behind):

```text
1. post:view:dirty에서 postId 목록 조회
2. 각 postId의 Redis count를 DB post.viewCount에 UPDATE
3. 성공한 postId는 dirty set에서 제거
```

### Bootstrap 전략

서버 시작 또는 Redis에 값이 없을 때:

```text
1. DB post.viewCount 값을 Redis에 세팅
2. 이후 Redis 기준으로 카운팅
```

### Redis 장애 시 Fallback

- 조회 API는 DB post.viewCount로 fallback
- 조회수 INCR 실패 시 DB viewCount를 직접 UPDATE (Lost Update 감수)
- 조회수는 정확성보다 서비스 가용성 우선

---

## 구현 순서

1. post 테이블 like_count 컬럼 제거, schema.sql 반영
2. Post 엔티티 likeCount 관련 코드 제거
3. PostLikeService 카운터 갱신 로직 제거
4. 게시글 조회 응답에 COUNT 쿼리 기반 likeCount 반영
5. Redis 설정 추가
6. viewCount Redis INCR 구현
7. dirty set 기반 DB 동기화 스케줄러 구현
8. Redis 장애 시 DB fallback 구현
9. 동시성 테스트 추가 (viewCount)

---

## 완료 기준

- likeCount는 항상 post_like COUNT 쿼리 기준으로 정확한 값을 반환한다.
- viewCount 동시 요청에서 Lost Update가 발생하지 않는다.
- Redis와 DB viewCount가 최종적으로 일치한다.
- Redis 장애 시에도 조회수 기능이 동작한다.
