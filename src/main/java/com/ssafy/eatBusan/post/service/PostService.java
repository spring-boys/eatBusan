package com.ssafy.eatBusan.post.service;

import com.ssafy.eatBusan.golbal.exception.EBException;
import com.ssafy.eatBusan.golbal.exception.ErrorCode;
import com.ssafy.eatBusan.member.domain.Member;
import com.ssafy.eatBusan.member.repository.MemberRepository;
import com.ssafy.eatBusan.post.domain.Post;
import com.ssafy.eatBusan.post.dto.PostRequireDto;
import com.ssafy.eatBusan.post.dto.PostResponseDto;
import com.ssafy.eatBusan.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    public List<PostResponseDto> getAllPost() {
        return postRepository.findAllByDeletedFalse().stream()
                .map(p -> new PostResponseDto(
                        p.getUser().getId(),
                        p.getUser().getEmail(),
                        p.getTitle(), p.getContent(),
                        p.getViewCount(), p.getLikeCount(), p.getCommentCount(),
                        p.getCreatedAt(), p.getUpdatedAt()))
                .toList();
    }


    @Transactional
    public PostResponseDto writePost(PostRequireDto req) {
        Member member = memberRepository.findMemberByEmail(req.email()).orElseThrow(() -> new EBException(ErrorCode.MEMBER_NOT_FOUND));
        Post post = Post.builder().user(member).title(req.title()).content(req.content()).build();
        postRepository.save(post);
        return PostResponseDto.from(post);
    }
}