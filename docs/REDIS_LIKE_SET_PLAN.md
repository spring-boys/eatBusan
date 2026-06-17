# Redis 좋아요 자료구조 선택 계획

## 결론

좋아요는 Redis String보다 Redis Set이 더 적합하다.

조회수는 단순 증가 카운터이므로 String `INCR`이 맞지만, 좋아요는 "어떤 회원이 어떤 게시글에 좋아요를 눌렀는지"를 함께 관리해야 한다. 따라서 중복 없는 회원 ID 집합을 표현할 수 있는 Set이 기능 요구사항과 더 잘 맞는다.

```text
조회수: post:view:{postId}:count        -> String
좋아요: post:likes:{postId}             -> Set
```

## String 방식 평가

String은 좋아요 수 하나만 저장하는 방식이다.

```text
post:like:{postId}:count = 17
```

사용 가능한 명령:

```text
INCR post:like:{postId}:count
DECR post:like:{postId}:count
GET post:like:{postId}:count
```

장점:

- 구현이 단순하다.
- 좋아요 수 조회가 빠르다.
- 카운터 증가/감소가 원자적으로 처리된다.

단점:

- 누가 좋아요를 눌렀는지 Redis만으로 알 수 없다.
- 같은 사용자의 중복 좋아요를 Redis 레벨에서 막기 어렵다.
- 좋아요 취소 시 해당 사용자가 실제로 좋아요를 누른 상태인지 확인하기 어렵다.
- DB와 Redis count가 어긋나면 Redis 값의 신뢰도가 낮다.

String 방식은 "좋아요 수만 빠르게 보여주면 되는 경우"에는 가능하지만, 현재 EatBusan의 좋아요 기능에는 부족하다.

## Set 방식 평가

Set은 게시글별 좋아요 회원 ID 집합을 저장하는 방식이다.

```text
post:likes:{postId} = {memberId1, memberId2, memberId3}
```

사용 가능한 명령:

```text
SADD post:likes:{postId} {memberId}
SREM post:likes:{postId} {memberId}
SISMEMBER post:likes:{postId} {memberId}
SCARD post:likes:{postId}
```

장점:

- 회원별 좋아요 여부를 바로 확인할 수 있다.
- Set 자체가 중복을 허용하지 않아 중복 좋아요 모델과 잘 맞는다.
- `SCARD`로 좋아요 수를 계산할 수 있다.
- `SADD`, `SREM`, `SISMEMBER`가 좋아요/취소/확인 기능과 자연스럽게 대응된다.

단점:

- String 카운터보다 메모리를 더 사용한다.
- 게시글별 좋아요 회원 수가 매우 커지면 Set 크기 관리가 필요하다.
- Redis를 원장으로 삼으면 Redis 장애나 초기화 시 데이터 유실 위험이 있다.

## EatBusan 추천 구조

현재 프로젝트에는 `post_like` 테이블이 있으므로 DB를 좋아요의 원장으로 유지한다.

```text
DB post_like = 정합성 원장
Redis Set    = 빠른 조회용 캐시
```

역할 분리:

| 대상 | 역할 |
|---|---|
| DB `post_like` | 최종 좋아요 상태 저장, unique constraint로 중복 방지 |
| Redis Set | 좋아요 여부와 좋아요 수 빠른 조회 |
| `post.likeCount` 컬럼 | 장기적으로 제거 또는 응답에서 사용하지 않음 |

## 추천 동작 흐름

### 좋아요

```text
1. DB에서 post, member 존재 확인
2. DB post_like에 row 생성 또는 deleted=false 복구
3. Redis SADD post:likes:{postId} {memberId}
4. 응답은 liked=true, likeCount=SCARD
```

### 좋아요 취소

```text
1. DB에서 활성 post_like 확인
2. DB post_like soft delete
3. Redis SREM post:likes:{postId} {memberId}
4. 응답은 liked=false, likeCount=SCARD
```

### 좋아요 여부 조회

```text
1. Redis SISMEMBER post:likes:{postId} {memberId}
2. Redis miss 또는 장애 시 DB exists 쿼리 fallback
```

### 좋아요 수 조회

```text
1. Redis SCARD post:likes:{postId}
2. Redis miss 또는 장애 시 DB COUNT 쿼리 fallback
```

## Bootstrap 전략

Redis Set이 없을 때 DB에서 해당 게시글의 좋아요 회원 ID 목록을 읽어 Set을 초기화한다.

```text
1. SELECT member_id FROM post_like WHERE post_id = ? AND deleted = false
2. SADD post:likes:{postId} memberId...
3. SCARD post:likes:{postId}
```

주의할 점:

- 좋아요가 0개인 게시글도 cache miss와 실제 0개를 구분해야 한다.
- 필요하면 별도 초기화 마커 키를 둔다.

```text
post:likes:{postId}:initialized = true
```

## 장애 처리

Redis 장애 시 좋아요 API가 실패하면 안 된다.

기본 원칙:

```text
쓰기 정합성: DB 기준
읽기 성능: Redis 우선
장애 대응: DB fallback
```

Redis 실패 시:

- 좋아요/취소 DB 트랜잭션은 정상 수행한다.
- Redis Set 갱신 실패는 로그만 남긴다.
- 응답의 likeCount는 DB COUNT 쿼리로 계산한다.
- 이후 Redis 복구 시 DB 기준으로 다시 bootstrap 한다.

## 구현 우선순위

1. `PostLikeRepository`에 count, exists, memberId 조회 쿼리 정리
2. `PostLikeService`에서 `post.likeCount` 직접 증가/감소 제거
3. Redis Set key 규칙 정의
4. Redis 좋아요 캐시 전용 서비스 분리
5. 좋아요/취소 후 Redis `SADD`, `SREM` 반영
6. 좋아요 수 응답은 Redis `SCARD`, 실패 시 DB `COUNT`
7. 내가 좋아요 눌렀는지 응답이 필요하면 Redis `SISMEMBER`, 실패 시 DB `exists`
8. Redis key bootstrap 구현
9. Redis 장애 fallback 테스트 추가
10. 동시 좋아요 테스트에서 DB unique constraint와 Redis Set 결과 검증

## 최종 판단

현재 EatBusan에는 Redis Set을 추천한다.

String은 단순 카운터라 조회수에는 적합하지만, 좋아요에는 "중복 방지", "좋아요 여부 확인", "취소 검증"이라는 도메인 요구가 있다. 이 요구는 Set이 더 직접적으로 해결한다.

단, Redis Set을 원장으로 두지는 않는다. 좋아요의 최종 상태는 DB `post_like`가 가지고, Redis Set은 빠른 조회와 카운트 계산을 위한 캐시로 사용한다.

## 참고 문서

- Redis Strings: https://redis.io/docs/latest/develop/data-types/strings/
- Redis INCR: https://redis.io/docs/latest/commands/incr
- Redis Sets: https://redis.io/docs/latest/develop/data-types/sets/
