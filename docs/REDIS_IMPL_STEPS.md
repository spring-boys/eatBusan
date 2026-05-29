# Redis 좋아요 구현 단계별 가이드

설계 문서: `docs/superpowers/specs/2026-05-28-redis-like-design.md`

---

## 현재 상태 요약

- `Post.java` — `likeCount` 필드 + `increaseLikeCount()` / `decreaseLikeCount()` 있음 → **제거 대상**
- `PostLikeService.java` — `post.increaseLikeCount()` 호출 → **Redis 방식으로 교체**
- `PostLikeController.java` — `ResponseEntity<Void>` 반환 → **`PostLikeResponse` 반환으로 변경**
- Redis 의존성 없음 → **추가 필요**
- `schema.sql` 없음 (ddl-auto=create) → JPA가 자동 생성하므로 **Post에서 필드 제거하면 컬럼도 사라짐**

---

## Step 1 — build.gradle에 Redis 의존성 추가

파일: `build.gradle`

`dependencies` 블록 안에 아래 한 줄 추가:

```groovy
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

---

## Step 2 — application.properties에 Redis 설정 추가

파일: `src/main/resources/application.properties`

아래 3줄 추가 (어디든 OK, kakao 설정 위에 두면 깔끔):

```properties
# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=200ms
```

---

## Step 3 — BaseEntity에 restore() 추가

파일: `src/main/java/com/ssafy/eatBusan/golbal/entity/BaseEntity.java`

기존 `delete()` 아래에 추가:

```java
public void restore() {
    this.deleted = false;
}
```

> **이유**: 좋아요 취소 후 다시 좋아요를 누를 때, soft-delete된 row를 복구해야 함.

---

## Step 4 — Post.java에서 likeCount 제거

파일: `src/main/java/com/ssafy/eatBusan/post/domain/Post.java`

**제거할 것:**

```java
@Column(nullable = false)
@Builder.Default
private int likeCount = 0;

public void increaseLikeCount() {
    this.likeCount++;
}

public void decreaseLikeCount() {
    this.likeCount--;
}
```

> **이유**: likeCount는 이제 Redis SCARD로 계산. Post 엔티티가 직접 관리하지 않음.

---

## Step 5 — PostLikeRepository에 쿼리 3개 추가

파일: `src/main/java/com/ssafy/eatBusan/postlike/repository/PostLikeRepository.java`

아래 3개 추가:

```java
// 좋아요 수 COUNT (DB fallback용)
long countByPostIdAndDeletedFalse(Long postId);

// bootstrap 시 DB → Redis 로드용
@Query("SELECT pl.member.id FROM PostLike pl WHERE pl.post.id = :postId AND pl.deleted = false")
List<Long> findMemberIdsByPostId(@Param("postId") Long postId);

// re-like 시 deleted row 복구용 (deleted 포함 전체 조회)
@Query("SELECT pl FROM PostLike pl WHERE pl.post.id = :postId AND pl.member.id = :memberId")
Optional<PostLike> findIncludingDeleted(@Param("postId") Long postId, @Param("memberId") Long memberId);
```

import도 추가 필요:
```java
import org.springframework.data.repository.query.Param;
import java.util.List;
```

---

## Step 6 — Lua 스크립트 작성

파일 생성: `src/main/resources/scripts/post-like-toggle.lua`

```lua
local key    = KEYS[1]   -- post:likes:{postId}
local member = ARGV[1]   -- memberId (문자열)

if redis.call('SISMEMBER', key, member) == 1 then
    redis.call('SREM', key, member)
    return {0, redis.call('SCARD', key)}
else
    redis.call('SADD', key, member)
    return {1, redis.call('SCARD', key)}
end
```

> **동작**: 이미 좋아요면 취소(SREM), 아니면 추가(SADD). `{liked(0/1), 현재count}` 반환.
> KEYS/ARGV 분리 이유: Redis Cluster 슬롯 라우팅 호환.

---

## Step 7 — RedisConfig.java 생성

파일 생성: `src/main/java/com/ssafy/eatBusan/golbal/config/RedisConfig.java`

```java
package com.ssafy.eatBusan.golbal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    @Bean
    public DefaultRedisScript<List> postLikeToggleScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/post-like-toggle.lua"));
        script.setResultType(List.class);
        return script;
    }
}
```

> `DefaultRedisScript`는 첫 실행 시 SHA1로 캐싱되어 이후 `EVALSHA`로 자동 전환됨.

---

## Step 8 — PostLikeCacheService.java 생성

파일 생성: `src/main/java/com/ssafy/eatBusan/postlike/service/PostLikeCacheService.java`

```java
package com.ssafy.eatBusan.postlike.service;

import com.ssafy.eatBusan.postlike.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostLikeCacheService {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> postLikeToggleScript;
    private final PostLikeRepository postLikeRepository;

    private String likeKey(Long postId) {
        return "post:likes:" + postId;
    }

    private String initKey(Long postId) {
        return "post:likes:" + postId + ":init";
    }

    /** Lazy Bootstrap — 첫 요청 시 DB 데이터를 Redis Set으로 로드 */
    public void ensureBootstrap(Long postId) {
        // SETNX로 동시 bootstrap 방지 (60초 임대)
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(initKey(postId), "1", Duration.ofSeconds(60));
        if (Boolean.FALSE.equals(acquired)) {
            return; // 이미 bootstrap됨 or 다른 스레드가 진행 중
        }
        // DB에서 좋아요 회원 ID 목록 로드
        List<Long> memberIds = postLikeRepository.findMemberIdsByPostId(postId);
        if (!memberIds.isEmpty()) {
            String[] members = memberIds.stream()
                    .map(String::valueOf)
                    .toArray(String[]::new);
            redisTemplate.opsForSet().add(likeKey(postId), members);
        }
        // 영구 마커로 전환 (TTL 제거)
        redisTemplate.persist(initKey(postId));
    }

    /** Lua 원자 토글 — {liked(0/1), likeCount} 반환 */
    public long[] toggle(Long postId, Long memberId) {
        List<?> result = redisTemplate.execute(
                postLikeToggleScript,
                List.of(likeKey(postId)),
                String.valueOf(memberId)
        );
        long liked = ((Number) result.get(0)).longValue();
        long count = ((Number) result.get(1)).longValue();
        return new long[]{liked, count};
    }

    /** DB 실패 시 Redis 역연산 보상 */
    public void compensate(Long postId, Long memberId, boolean liked) {
        if (liked) {
            // 좋아요 추가 후 DB 실패 → 다시 제거
            redisTemplate.opsForSet().remove(likeKey(postId), String.valueOf(memberId));
        } else {
            // 좋아요 취소 후 DB 실패 → 다시 추가
            redisTemplate.opsForSet().add(likeKey(postId), String.valueOf(memberId));
        }
    }

    /** 좋아요 수 조회 (SCARD) */
    public long likeCount(Long postId) {
        Long count = redisTemplate.opsForSet().size(likeKey(postId));
        return count != null ? count : 0L;
    }
}
```

---

## Step 9 — PostLikeResponse DTO 생성

파일 생성: `src/main/java/com/ssafy/eatBusan/postlike/dto/PostLikeResponse.java`

```java
package com.ssafy.eatBusan.postlike.dto;

public record PostLikeResponse(boolean liked, long likeCount) {
}
```

---

## Step 10 — PostLikeService.java 재작성

파일 전체 교체: `src/main/java/com/ssafy/eatBusan/postlike/service/PostLikeService.java`

```java
package com.ssafy.eatBusan.postlike.service;

import com.ssafy.eatBusan.golbal.exception.EBException;
import com.ssafy.eatBusan.golbal.exception.ErrorCode;
import com.ssafy.eatBusan.member.domain.Member;
import com.ssafy.eatBusan.member.repository.MemberRepository;
import com.ssafy.eatBusan.post.domain.Post;
import com.ssafy.eatBusan.post.repository.PostRepository;
import com.ssafy.eatBusan.postlike.domain.PostLike;
import com.ssafy.eatBusan.postlike.dto.PostLikeResponse;
import com.ssafy.eatBusan.postlike.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final PostLikeCacheService postLikeCacheService;

    @Transactional
    public PostLikeResponse like(Long postId, Long memberId) {
        Post post = postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new EBException(ErrorCode.MEMBER_NOT_FOUND));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EBException(ErrorCode.MEMBER_NOT_FOUND));

        try {
            // 1. Lazy bootstrap (첫 요청 시 DB → Redis 로드)
            postLikeCacheService.ensureBootstrap(postId);

            // 2. Lua 원자 토글
            long[] result = postLikeCacheService.toggle(postId, memberId);
            boolean liked = result[0] == 1L;
            long likeCount = result[1];

            // 3. DB 동기화
            try {
                syncToDb(post, member, liked);
            } catch (DataAccessException e) {
                log.warn("DB sync failed postId={} memberId={}, compensating Redis", postId, memberId, e);
                postLikeCacheService.compensate(postId, memberId, liked);
                throw e;
            }

            return new PostLikeResponse(liked, likeCount);

        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable, DB fallback postId={} memberId={}", postId, memberId, e);
            return fallbackToDb(post, member, postId);
        }
    }

    private void syncToDb(Post post, Member member, boolean liked) {
        if (liked) {
            // 좋아요: 기존 deleted row 복구 or 새 row 생성
            Optional<PostLike> existing =
                    postLikeRepository.findIncludingDeleted(post.getId(), member.getId());
            if (existing.isPresent()) {
                existing.get().restore();
            } else {
                postLikeRepository.save(PostLike.of(post, member));
            }
        } else {
            // 취소: soft delete
            postLikeRepository.findByPostAndMemberDeletedFalse(post, member)
                    .ifPresent(PostLike::delete);
        }
    }

    /** Redis 장애 시 DB만으로 토글 (가용성 우선) */
    private PostLikeResponse fallbackToDb(Post post, Member member, Long postId) {
        boolean liked;
        Optional<PostLike> active =
                postLikeRepository.findByPostAndMemberDeletedFalse(post, member);
        if (active.isPresent()) {
            active.get().delete();
            liked = false;
        } else {
            Optional<PostLike> deleted =
                    postLikeRepository.findIncludingDeleted(post.getId(), member.getId());
            if (deleted.isPresent()) {
                deleted.get().restore();
            } else {
                postLikeRepository.save(PostLike.of(post, member));
            }
            liked = true;
        }
        long likeCount = postLikeRepository.countByPostIdAndDeletedFalse(postId);
        return new PostLikeResponse(liked, likeCount);
    }
}
```

---

## Step 11 — PostLikeController.java 응답 변경

파일: `src/main/java/com/ssafy/eatBusan/postlike/controller/PostLikeController.java`

**import 추가:**
```java
import com.ssafy.eatBusan.postlike.dto.PostLikeResponse;
```

**메서드 시그니처 변경:**
```java
// 변경 전
public ResponseEntity<Void> like(...)

// 변경 후
public ResponseEntity<PostLikeResponse> like(...)
```

**응답 반환 변경:**
```java
// 변경 전
boolean like = postLikeService.like(postId, memberId);
return like ? ResponseEntity.status(HttpStatus.CREATED).build() :
        ResponseEntity.status(HttpStatus.OK).build();

// 변경 후
PostLikeResponse response = postLikeService.like(postId, memberId);
return response.liked()
        ? ResponseEntity.status(HttpStatus.CREATED).body(response)
        : ResponseEntity.ok(response);
```

불필요한 import도 제거:
```java
// 삭제
import com.ssafy.eatBusan.postlike.domain.PostLike;
import java.util.List;
```

---

## Step 12 — PostLikeConcurrencyTest.java 수정

파일: `src/test/java/com/ssafy/eatBusan/postlike/service/PostLikeConcurrencyTest.java`

**`@Autowired` 추가:**
```java
@Autowired private PostLikeCacheService postLikeCacheService;
```

**`like_토글_좋아요_취소_정상_동작` 테스트 수정:**

제거:
```java
int likeCount = postRepository.findById(postId).orElseThrow().getLikeCount();
assertThat(likeCount).isEqualTo(1);
```

추가:
```java
long dbCount = postLikeRepository.countByPostIdAndDeletedFalse(postId);
long redisCount = postLikeCacheService.likeCount(postId);
assertThat(dbCount).isEqualTo(1);
assertThat(redisCount).isEqualTo(dbCount);
```

**`DB_한계_증명_동시_좋아요_시_likeCount_Lost_Update_발생` 테스트 수정:**

제거:
```java
int likeCountInPost = postRepository.findById(postId).orElseThrow().getLikeCount();
assertThat(likeCountInPost).isEqualTo((int) actualPostLikeCount);
```

추가 (테스트 목적 변경 — 이제 Redis로 race condition이 해결되었음을 검증):
```java
long redisScard = postLikeCacheService.likeCount(postId);
assertThat(redisScard).isEqualTo(actualPostLikeCount); // Redis == DB여야 함
```

테스트 이름도 변경:
```java
// 변경 전
void DB_한계_증명_동시_좋아요_시_likeCount_Lost_Update_발생()

// 변경 후
void 동시_좋아요_30명_Redis_SCARD_와_DB_COUNT_일치()
```

---

## 구현 순서 (권장)

```
Step 1  → build.gradle
Step 2  → application.properties
Step 3  → BaseEntity.java (restore 추가)
Step 4  → Post.java (likeCount 제거)
Step 5  → PostLikeRepository.java (쿼리 추가)
Step 6  → scripts/post-like-toggle.lua (신규)
Step 7  → RedisConfig.java (신규)
Step 8  → PostLikeCacheService.java (신규)
Step 9  → PostLikeResponse.java (신규 DTO)
Step 10 → PostLikeService.java (재작성)
Step 11 → PostLikeController.java (응답 변경)
Step 12 → PostLikeConcurrencyTest.java (수정)
```

## 중간 확인 체크포인트

- Step 7 완료 후: `./gradlew build` — 컴파일 오류 없으면 OK
- Step 10 완료 후: `./gradlew build` — 전체 컴파일 확인
- Step 12 완료 후: `./gradlew test` — 테스트 통과 확인 (Redis 실행 필요)

## 완료 기준 (설계 문서 기준)

- [ ] `post.like_count` 컬럼 및 관련 코드가 어디에도 없음
- [ ] 30 스레드 동시 좋아요 시 `SCARD == post_like COUNT`
- [ ] Redis 없이도 DB fallback으로 API 응답 가능
