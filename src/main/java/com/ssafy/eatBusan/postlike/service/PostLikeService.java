package com.ssafy.eatBusan.postlike.service;

import com.ssafy.eatBusan.golbal.exception.EBException;
import com.ssafy.eatBusan.golbal.exception.ErrorCode;
import com.ssafy.eatBusan.member.domain.Member;
import com.ssafy.eatBusan.member.repository.MemberRepository;
import com.ssafy.eatBusan.post.domain.Post;
import com.ssafy.eatBusan.post.repository.PostRepository;
import com.ssafy.eatBusan.postlike.domain.PostLike;
import com.ssafy.eatBusan.postlike.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;

    /**
     * FIXME: [Race Condition] post.likeCount 갱신은 JPA dirty checking 기반으로
     *   동시 요청 시 Lost Update 발생 가능 — Redis INCR(원자 연산)로 해결 예정.
     *   docs/REDIS_LIKE_GOAL.md 참고.
     */
    @Transactional
    public boolean like(Long postId, Long memberId) {
        Post post = postRepository.findByIdAndDeletedFalse(postId).orElseThrow(() -> new EBException(ErrorCode.POST_NOT_FOUND));
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new EBException(ErrorCode.MEMBER_NOT_FOUND));

        Optional<PostLike> exist = postLikeRepository.findByPostIdAndMemberId(postId, memberId);
        if (exist.isPresent()) {
            PostLike postLike = exist.get();
            if (!postLike.isDeleted()) {
                postLike.delete();
                post.decreaseLikeCount();
                return false;
            }
            postLike.restore();
            post.increaseLikeCount();
            return true;
        }

        postLikeRepository.save(PostLike.of(post, member));
        post.increaseLikeCount();
        return true;
    }

    public boolean isLiked(Long postId, Long memberId) {
        return postLikeRepository.existsByPostIdAndMemberIdAndDeletedFalse(postId, memberId);
    }
}
