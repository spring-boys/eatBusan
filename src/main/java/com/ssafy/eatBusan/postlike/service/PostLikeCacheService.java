package com.ssafy.eatBusan.postlike.service;

import com.ssafy.eatBusan.global.exception.EBException;
import com.ssafy.eatBusan.global.exception.ErrorCode;
import com.ssafy.eatBusan.postlike.repository.PostLikeRepository;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostLikeCacheService {

    private static final Duration CACHE_INIT_TTL = Duration.ofMinutes(30);

    private final PostLikeRepository postLikeRepository;
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> postLikeToggleScript;
    private final DefaultRedisScript<Long> postLikeInvalidateScript;

    public void ensureBootstrap(Long postId) {
        // initKey는 "DB -> Redis 적재가 끝났다"는 완료 표시다.
        // 이 키가 있으면 Redis Set을 바로 읽거나 토글해도 된다.
        String initKey = initKey(postId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(initKey))) {
            Long ttl = redisTemplate.getExpire(initKey);
            if (ttl == null || ttl >= 0) {
                return;
            }
            redisTemplate.delete(initKey);
        }

        // lockKey는 "누군가 지금 bootstrap 중이다"는 작업 중 표시다.
        // setIfAbsent는 Redis SET NX와 같아서, 여러 요청 중 하나만 lock을 잡는다.
        Boolean lockAcquired = redisTemplate.opsForValue()
            .setIfAbsent(lockKey(postId), "1", Duration.ofSeconds(10));

        // lock을 못 잡았다는 건 다른 요청이 DB -> Redis 적재 중이라는 뜻이다.
        // 여기서 그냥 return하면 빈 Redis Set에서 toggle이 실행될 수 있으므로 완료를 기다린다.
        if (!Boolean.TRUE.equals(lockAcquired)) {
            waitUntilBootstrapped(postId);
            return;
        }

        try {
            // 이전 bootstrap 실패로 일부 memberId만 남아 있을 수 있으므로,
            // DB를 기준으로 다시 만들기 전에 좋아요 Set 본체를 먼저 비운다.
            redisTemplate.delete(likeKey(postId));

            List<Long> memberIds = postLikeRepository.findMemberIdsByPostId(postId);
            if (!memberIds.isEmpty()) {
                String[] members = memberIds.stream().map(String::valueOf).toArray(String[]::new);
                redisTemplate.opsForSet().add(likeKey(postId), members);
            }

            // 좋아요가 0개여도 bootstrap 완료 상태는 표시해야 한다.
            // Redis에서 빈 Set은 key 없음과 구분이 안 되기 때문에 initKey가 필요하다.
            redisTemplate.opsForValue().set(initKey, "1", CACHE_INIT_TTL);
        } finally {
            // bootstrap 성공/실패와 상관없이 작업 중 표시는 반드시 해제한다.
            // initKey는 성공한 경우에만 위에서 세팅한다.
            redisTemplate.delete(lockKey(postId));
        }
    }

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

    public void compensate(Long postId, Long memberId, boolean liked) {
        if (liked) {
            redisTemplate.opsForSet().remove(likeKey(postId), String.valueOf(memberId));
        } else {
            redisTemplate.opsForSet().add(likeKey(postId), String.valueOf(memberId));
        }
    }

    public boolean checkLiked(Long postId, Long memberId) {
        try {
            ensureBootstrap(postId);
            return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(likeKey(postId), String.valueOf(memberId)));
        } catch (RedisConnectionFailureException e) {
            return postLikeRepository.existsByPostIdAndMemberIdAndDeletedFalse(postId, memberId);
        }
    }

    public long likeCount(Long postId) {
        try {
            ensureBootstrap(postId);
            Long count = redisTemplate.opsForSet().size(likeKey(postId));
            return count != null ? count : 0L;
        } catch (RedisConnectionFailureException e) {
            return postLikeRepository.countByPostIdAndDeletedFalse(postId);
        }
    }

    public void invalidateAfterMemberWithdrawal(
        List<Long> likedPostIds,
        List<Long> ownedPostIds
    ) {
        Set<Long> affectedPostIds = new LinkedHashSet<>(likedPostIds);
        affectedPostIds.addAll(ownedPostIds);

        for (Long postId : affectedPostIds) {
            Long invalidated = redisTemplate.execute(
                postLikeInvalidateScript,
                List.of(likeKey(postId), initKey(postId), lockKey(postId)),
                new Object[0]
            );
            if (!Long.valueOf(1L).equals(invalidated)) {
                throw new EBException(ErrorCode.CACHE_BOOTSTRAP_IN_PROGRESS);
            }
        }
    }

    private String likeKey(Long postId) {
        return "post:likes:" + postId;
    }

    private String initKey(Long postId) {
        return "post:likes:" + postId + ":init";
    }

    private String lockKey(Long postId) {
        return "post:likes:" + postId + ":lock";
    }

    private void waitUntilBootstrapped(Long postId) {
        for (int i = 0; i < 5; i++) {
            try {
                Thread.sleep(100);
                // 다른 요청이 initKey를 세팅했다면 bootstrap이 끝난 것이므로 통과한다.
                if (Boolean.TRUE.equals(redisTemplate.hasKey(initKey(postId)))) {
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new EBException(ErrorCode.REDIS_BOOTSTRAP_TIMEOUT);
            }
        }

        // 끝까지 initKey가 생기지 않으면 Redis Set을 신뢰할 수 없다.
        // 이 상태에서 toggle을 계속하면 DB와 Redis가 갈라질 수 있으므로 실패시킨다.
        throw new EBException(ErrorCode.REDIS_BOOTSTRAP_TIMEOUT);
    }

}
