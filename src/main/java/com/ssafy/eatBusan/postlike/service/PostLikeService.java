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

    @Transactional
    public boolean like(Long postId, Long memberId) {
        Post post = postRepository.findByIdAndDeletedFalse(postId).orElseThrow(() -> new EBException(ErrorCode.MEMBER_NOT_FOUND));
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new EBException(ErrorCode.MEMBER_NOT_FOUND));

        Optional<PostLike> existing = postLikeRepository.findByPostAndMemberDeletedFalse(post, member);
        if(existing.isPresent()) {
            existing.get().delete();
            post.decreaseLikeCount();
            return false;
        }
        postLikeRepository.save(PostLike.of(post, member));
        post.increaseLikeCount();
        return true;
    }
}
