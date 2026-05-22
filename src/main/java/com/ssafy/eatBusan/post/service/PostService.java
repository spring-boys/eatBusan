package com.ssafy.eatBusan.post.service;

import com.ssafy.eatBusan.global.exception.EBException;
import com.ssafy.eatBusan.global.exception.ErrorCode;
import com.ssafy.eatBusan.member.domain.Member;
import com.ssafy.eatBusan.member.repository.MemberRepository;
import com.ssafy.eatBusan.place.Repository.PlaceRepository;
import com.ssafy.eatBusan.place.domain.Place;
import com.ssafy.eatBusan.post.domain.Post;
import com.ssafy.eatBusan.post.dto.PostRequireDto;
import com.ssafy.eatBusan.post.dto.PostResponseDto;
import com.ssafy.eatBusan.post.repository.PostRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final PlaceRepository placeRepository;

    public List<PostResponseDto> getAllPost() {
        return postRepository.findAllByDeletedFalse().stream()
                .map(p -> new PostResponseDto(
                        p.getId(),
                        p.getUser().getId(),
                        p.getPlace().getId(),
                        p.getUser().getEmail(),
                        p.getTitle(), p.getContent(),
                        p.getViewCount(), p.getLikeCount(), p.getCommentCount(),
                        p.getCreatedAt(), p.getUpdatedAt()))
                .toList();
    }


    @Transactional
    public PostResponseDto writePost(PostRequireDto req) {
        Member member = memberRepository.findMemberByEmail(req.email()).orElseThrow(() -> new EBException(ErrorCode.MEMBER_NOT_FOUND));
        Place place = placeRepository.findById(req.placeId()).orElseThrow(() -> new EBException(ErrorCode.MEMBER_NOT_FOUND));
        Post post = Post.builder().user(member).place(place).title(req.title()).content(req.content()).build();
        postRepository.save(post);
        return PostResponseDto.from(post);
    }

    @Transactional
    public PostResponseDto updatePost(PostRequireDto req, Long postId) {
        Post post = postRepository.findByIdAndDeletedFalse(postId).orElseThrow(() -> new EBException(ErrorCode.MEMBER_NOT_FOUND));
        post.update(req.title(), req.content());
        return PostResponseDto.from(post);
    }

    @Transactional
    public PostResponseDto getPost(Long id) {
        // TODO: MEMBER NOT FOUND -> POST NOT FOUND로 변경
        Post post = postRepository.findByIdAndDeletedFalse(id).orElseThrow(() -> new EBException(ErrorCode.MEMBER_NOT_FOUND));
        post.increaseViewCount();
        return PostResponseDto.from(post);
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findByIdAndDeletedFalse(postId).orElseThrow(() -> new EBException(ErrorCode.MEMBER_NOT_FOUND));
        post.delete();
    }
}
