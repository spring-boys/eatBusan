package com.ssafy.eatBusan.postlike.service;

import com.ssafy.eatBusan.postlike.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PostLikeCacheService {
    private final PostLikeRepository postLikeRepository;
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> postLikeToggleScript;

    public void ensureBootstrap(Long postId) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(initKey(postId), "1", Duration.ofSeconds(60));
        if (Boolean.FALSE.equals(acquired)) {
            return;
        }
        List<Long> memberIds = postLikeRepository.findMemberIdsByPostId(postId);
        if (!memberIds.isEmpty()) {
            String[] members = memberIds.stream().map(String::valueOf).toArray(String[]::new);
            redisTemplate.opsForSet().add(likeKey(postId), members);
        }
        redisTemplate.persist(initKey(postId));
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

    public boolean checkLiked(Long postId, Long memberId){
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(likeKey(postId), String.valueOf(memberId)));
    }

    public long likeCount(Long postId) {
        Long count = redisTemplate.opsForSet().size(likeKey(postId));
        return count != null ? count : 0L;
    }


    private String likeKey(Long postId) {
        return "post:likes:" + postId;
    }

    private String initKey(Long postId) {
        return "post:likes:" + postId + ":init";
    }
}
