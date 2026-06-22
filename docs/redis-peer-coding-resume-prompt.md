# Redis Peer Coding Resume Prompt

이 문서는 집이나 다른 환경에서 같은 맥락으로 이어 하기 위한 프롬프트다.
새 세션에서 이 파일을 읽힌 뒤 아래 목표를 `/goal`로 설정한다.

## Goal

사용자가 Redis를 이해하고 EatBusan 프로젝트에서 Redis 좋아요 캐시를 정합성 있게 구현, 검증할 수 있도록 튜터링과 피어 코딩을 병행한다.

## Collaboration Rules

- 사용자가 최대한 직접 코드를 수정한다.
- Codex는 먼저 개념, 이유, 실패 시나리오를 설명한다.
- Codex는 사용자가 요청하기 전까지 직접 파일을 수정하지 않는다.
- 직접 수정이 필요할 때는 어떤 파일의 어떤 메서드를 왜 바꾸는지 먼저 말한다.
- 한 번에 큰 리팩토링을 하지 말고 Redis 정합성 문제를 작은 단계로 나눠 진행한다.
- `@LoginMember` 적용은 별도 브랜치에서 진행하고, 현재 Redis 브랜치에서는 좋아요 캐시 정합성에 집중한다.

## Current Branch Context

- 브랜치: `feat/post-like-redis`
- 핵심 기능: 게시글 좋아요를 Redis Set으로 캐싱한다.
- Redis 좋아요 Set 키: `post:likes:{postId}`
- 완료 표시 키: `post:likes:{postId}:init`
- 작업 중 락 키: `post:likes:{postId}:lock`

## Current Learning Point

초기 구현은 `initKey` 하나로 두 의미를 동시에 표현했다.

```text
initKey = 누군가 bootstrap 중
initKey = bootstrap 완료됨
```

이렇게 하면 `lock != done` 문제가 생긴다.

```text
요청 A: initKey SETNX 성공, DB 조회 시작
요청 B: initKey SETNX 실패, 초기화 완료로 착각
요청 B: 빈 Redis Set에서 toggle
결과: DB와 Redis 상태가 갈라질 수 있음
```

따라서 키 의미를 분리해야 한다.

```text
lockKey = 누군가 bootstrap 중
initKey = bootstrap 완료됨
```

## Target Flow

`ensureBootstrap(postId)`는 다음 흐름으로 만든다.

```text
1. initKey가 있으면 return
2. lockKey SETNX 성공
   - DB active like memberIds 조회
   - Redis Set 재생성 또는 적재
   - initKey set
   - finally에서 lockKey delete
3. lockKey SETNX 실패
   - 다른 요청이 bootstrap 중이라는 뜻
   - initKey가 생길 때까지 짧게 retry
   - retry 초과 시 EBException(ErrorCode.REDIS_BOOTSTRAP_TIMEOUT)
```

## Why Retry Must Not Just Return

lock을 못 잡았을 때 바로 return하면 위험하다.

```text
Redis가 아직 비어 있는데 toggle()이 실행될 수 있음
DB에는 이미 좋아요가 있는데 Redis에는 없어서 새 좋아요로 판단할 수 있음
```

그래서 bootstrap이 완료되지 않았으면 Redis Set을 읽거나 토글하면 안 된다.

## Next Checklist

1. `PostLikeCacheService.ensureBootstrap()`에서 `setIfAbsent(initKey)`를 `setIfAbsent(lockKey)`로 바꾼다.
2. `waitUntilBootstrapped(postId)`를 완성한다.
3. `RuntimeException` 대신 `EBException(ErrorCode.REDIS_BOOTSTRAP_TIMEOUT)`을 사용한다.
4. `likeCount(postId)`와 `checkLiked(postId, memberId)`도 필요하면 `ensureBootstrap(postId)`를 먼저 거치게 한다.
5. Redis 키를 수동 초기화하고 integration.http로 동작을 확인한다.
6. `./gradlew test`를 실행한다.

## Useful Commands

```bash
redis-cli --scan --pattern "post:likes*"
redis-cli SMEMBERS post:likes:1
redis-cli SCARD post:likes:1
redis-cli DEL post:likes:1 post:likes:1:init post:likes:1:lock
```

```bash
/usr/bin/mysql --protocol=TCP -h 127.0.0.1 -P 3306 -u ssafy -pssafy --default-character-set=utf8mb4
```

## Suggested First Question For Next Session

`lockKey`를 못 잡은 요청이 바로 return하면 왜 Redis와 DB 정합성이 깨질 수 있는지 다시 설명해보자.
