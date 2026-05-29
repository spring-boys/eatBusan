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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostLikeService {
    private final PostLikeCacheService postLikeCacheService;
    private final PostLikeRepository postLikeRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;

    @Transactional
    public PostLikeResponse like(Long postId, Long memberId) {
        Post post = postRepository.findByIdAndDeletedFalse(postId).orElseThrow(() -> new EBException(ErrorCode.POST_NOT_FOUND));
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new EBException(ErrorCode.MEMBER_NOT_FOUND));
        postLikeCacheService.ensureBootstrap(post.getId());

        long[] result = postLikeCacheService.toggle(post.getId(), member.getId());
        Optional<PostLike> exist = postLikeRepository.findIncludingDeleted(post.getId(), member.getId());
        if (result[0] == 1L) {
            try {
                if (exist.isPresent()) {
                    PostLike postLike = exist.get();
                    postLike.restore(); // soft-deleted 복구
                    return PostLikeResponse.of(result[0] == 1L, result[1]);
                }
                postLikeRepository.save(PostLike.of(post, member));
                return PostLikeResponse.of(result[0] == 1L, result[1]);
            } catch (Exception e) {
                postLikeCacheService.compensate(post.getId(), member.getId(), result[0] == 1L);
                return PostLikeResponse.of(result[0]==1L, result[1]);
            }
        }
        try {
            PostLike postLike = exist.get();
            postLike.delete();
        } catch (Exception e) {
            postLikeCacheService.compensate(post.getId(), member.getId(), result[0] == 1L);
        }
        return PostLikeResponse.of(result[0] == 1L, result[1]);
    }

    public boolean isLiked(Long postId, Long memberId) {
        return postLikeCacheService.checkLiked(postId,memberId);
    }
}
