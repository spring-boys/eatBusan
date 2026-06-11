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
import com.ssafy.eatBusan.postimage.dto.PostImageDto;
import com.ssafy.eatBusan.postimage.mapper.PostImageMapper;
import com.ssafy.eatBusan.postimage.service.PostImageService;
import com.ssafy.eatBusan.postlike.service.PostLikeCacheService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final PlaceRepository placeRepository;
    private final PostLikeCacheService postLikeCacheService;
    private final PostImageMapper postImageMapper;
    private final PostImageService postImageService;

    public List<PostResponseDto> getAllPost() {
        return postRepository.findAllByDeletedFalse().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<PostResponseDto> getPostsByPlace(Long placeId) {
        return postRepository.findAllByPlace_IdAndDeletedFalseOrderByIdDesc(placeId).stream()
                .map(this::toResponse)
                .toList();
    }


    @Transactional
    public PostResponseDto writePost(PostRequireDto req) {
        Post post = savePost(req);
        return toResponse(post);
    }

    @Transactional
    public PostResponseDto writePostWithImages(PostRequireDto req, List<MultipartFile> files) {
        Post post = savePost(req);
        List<PostImageDto> images = uploadImagesIfPresent(post.getId(), files);
        return PostResponseDto.from(post, postLikeCacheService.likeCount(post.getId()), images);
    }

    @Transactional
    public PostResponseDto updatePost(PostRequireDto req, Long postId) {
        Post post = postRepository.findByIdAndDeletedFalse(postId).orElseThrow(() -> new EBException(ErrorCode.MEMBER_NOT_FOUND));
        post.update(req.title(), req.content());
        return toResponse(post);
    }

    @Transactional
    public PostResponseDto getPost(Long id) {
        // TODO: MEMBER NOT FOUND -> POST NOT FOUND로 변경
        Post post = postRepository.findByIdAndDeletedFalse(id).orElseThrow(() -> new EBException(ErrorCode.MEMBER_NOT_FOUND));
        post.increaseViewCount();
        return toResponse(post);
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findByIdAndDeletedFalse(postId).orElseThrow(() -> new EBException(ErrorCode.MEMBER_NOT_FOUND));
        post.delete();
    }

    private Post savePost(PostRequireDto req) {
        Member member = memberRepository.findMemberByEmail(req.email())
            .orElseThrow(() -> new EBException(ErrorCode.MEMBER_NOT_FOUND));
        Place place = placeRepository.findById(req.placeId())
            .orElseThrow(() -> new EBException(ErrorCode.PLACE_NOT_FOUND));
        Post post = Post.builder()
            .member(member)
            .place(place)
            .title(req.title())
            .content(req.content())
            .build();
        return postRepository.save(post);
    }

    private PostResponseDto toResponse(Post post) {
        List<PostImageDto> images = postImageMapper.findByPostId(post.getId());
        return PostResponseDto.from(post, postLikeCacheService.likeCount(post.getId()), images);
    }

    private List<PostImageDto> uploadImagesIfPresent(Long postId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        return postImageService.uploadImages(postId, files);
    }
}
