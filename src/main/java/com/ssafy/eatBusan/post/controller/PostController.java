package com.ssafy.eatBusan.post.controller;

import com.ssafy.eatBusan.auth.resolver.LoginMember;
import com.ssafy.eatBusan.member.dto.MemberDto;
import com.ssafy.eatBusan.post.dto.PostRequireDto;
import com.ssafy.eatBusan.post.dto.PostResponseDto;
import com.ssafy.eatBusan.post.service.PostService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    @GetMapping
    public ResponseEntity<List<PostResponseDto>> getPostList(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ResponseEntity.ok(postService.getAllPost(authorization));
    }

    @GetMapping("/my")
    public ResponseEntity<List<PostResponseDto>> getMyPost(@LoginMember MemberDto memberDto){
        return ResponseEntity.ok(postService.getMyPost(memberDto));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponseDto> getPost(
            @PathVariable Long postId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ResponseEntity.ok(postService.getPost(postId, authorization));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PostResponseDto> createPost(@RequestBody PostRequireDto req) {
        return ResponseEntity.ok(postService.writePost(req));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponseDto> createPostWithImages(
            @RequestPart("post") PostRequireDto req,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) {
        return ResponseEntity.ok(postService.writePostWithImages(req, files));
    }

    @PatchMapping("/{postId}")
    public ResponseEntity<PostResponseDto> updatePost(
            @PathVariable Long postId,
            @RequestBody PostRequireDto req,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return ResponseEntity.ok(postService.updatePost(req, postId, authorization));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
        postService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }
}
