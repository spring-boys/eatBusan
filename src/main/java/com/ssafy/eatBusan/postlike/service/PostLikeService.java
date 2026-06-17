package com.ssafy.eatBusan.postlike.service;

import com.ssafy.eatBusan.global.exception.EBException;
import com.ssafy.eatBusan.global.exception.ErrorCode;
import com.ssafy.eatBusan.member.domain.Member;
import com.ssafy.eatBusan.member.repository.MemberRepository;
import com.ssafy.eatBusan.post.domain.Post;
import com.ssafy.eatBusan.post.repository.PostRepository;
import com.ssafy.eatBusan.postlike.domain.PostLike;
import com.ssafy.eatBusan.postlike.dto.PostLikeResponse;
import com.ssafy.eatBusan.postlike.repository.PostLikeRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class PostLikeService {

    private final PostLikeCacheService postLikeCacheService;
    private final PostLikeRepository postLikeRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;

    @Transactional
    public PostLikeResponse like(Long postId, Long memberId) {
        Post post = postRepository.findByIdAndDeletedFalse(postId)
            .orElseThrow(() -> new EBException(ErrorCode.POST_NOT_FOUND));
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new EBException(ErrorCode.MEMBER_NOT_FOUND));

        long[] result;
        try {
            // Redis Set이 DB 기준으로 초기화되어 있는지 먼저 보장한다.
            // ensureBootstrap은 DB -> Redis 로드만 하고, DB를 변경하지 않는다.
            postLikeCacheService.ensureBootstrap(post.getId());

            // Redis Lua script로 토글 결과를 먼저 결정한다.
            // result[0] == 1이면 좋아요 상태, 0이면 좋아요 취소 상태다.
            // result[1]은 Redis SCARD로 계산한 현재 좋아요 수다.
            result = postLikeCacheService.toggle(post.getId(), member.getId());
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis unavailable, using DB fallback. postId={} memberId={}",
                post.getId(), member.getId(), e);
            return fallbackToDb(post, member);
        }

        boolean liked = result[0] == 1L;
        long likeCount = result[1];

        // Redis는 이미 바뀐 상태이므로, DB 동기화 실패 시 Redis를 되돌린 뒤 예외를 다시 던진다.
        // 예외를 삼키고 성공 응답을 주면 클라이언트와 DB/Redis 상태가 서로 어긋난다.
        try {
            syncToDb(post, member, liked);
        } catch (Exception e) {
            log.warn("DB sync failed, compensating Redis. postId={} memberId={}",
                post.getId(), member.getId(), e);
            try {
                postLikeCacheService.compensate(post.getId(), member.getId(), liked);
            } catch (Exception compensationException) {
                e.addSuppressed(compensationException);
                log.error("Redis compensation failed. postId={} memberId={}",
                    post.getId(), member.getId(), compensationException);
            }
            throw e;
        }
        return PostLikeResponse.of(liked, likeCount);
    }

    private void syncToDb(Post post, Member member, boolean liked) {
        // deleted=true row도 찾아야 한다.
        // 좋아요 취소 후 다시 좋아요를 누르면 새 row insert가 아니라 기존 row restore가 맞다.
        Optional<PostLike> exist = postLikeRepository.findIncludingDeleted(post.getId(),
            member.getId());

        if (liked) {
            if (exist.isPresent()) {
                exist.get().restore();
            } else {
                postLikeRepository.save(PostLike.of(post, member));
            }
        } else {
            exist.filter(postLike -> !postLike.isDeleted()).ifPresent(PostLike::delete);
        }

        // JPA save/restore/delete는 SQL 실행을 트랜잭션 commit 시점까지 미룰 수 있다.
        // 여기서 flush해야 DB 예외를 like()의 catch에서 잡고 Redis compensate를 수행할 수 있다.
        postLikeRepository.flush();
    }

    private PostLikeResponse fallbackToDb(Post post, Member member) {
        boolean liked;
        Optional<PostLike> active = postLikeRepository.findByPostAndMemberDeletedFalse(post,
            member);
        if (active.isPresent()) {
            active.get().delete();
            liked = false;
        } else {
            Optional<PostLike> exist = postLikeRepository.findIncludingDeleted(
                post.getId(), member.getId());
            if (exist.isPresent()) {
                exist.get().restore();
            } else {
                postLikeRepository.save(PostLike.of(post, member));
            }
            liked = true;
        }
        postLikeRepository.flush();
        long likeCount = postLikeRepository.countByPostIdAndDeletedFalse(post.getId());
        return PostLikeResponse.of(liked, likeCount);
    }

    public boolean isLiked(Long postId, Long memberId) {
        return postLikeCacheService.checkLiked(postId, memberId);
    }
}
