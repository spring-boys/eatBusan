package com.ssafy.eatBusan.postimage.controller;

import com.ssafy.eatBusan.auth.resolver.LoginMember;
import com.ssafy.eatBusan.member.dto.MemberDto;
import com.ssafy.eatBusan.postimage.dto.PostImageDto;
import com.ssafy.eatBusan.postimage.service.PostImageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostImageController {

    private final PostImageService postImageService;

    @GetMapping("/{postId}/images")
    public ResponseEntity<List<PostImageDto>> getImages(@PathVariable Long postId) {
        return ResponseEntity.ok(postImageService.findImages(postId));
    }

    @PostMapping(value = "/{postId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<PostImageDto>> uploadImages(
        @PathVariable Long postId,
        @RequestPart("files") List<MultipartFile> files
    ) {
        return ResponseEntity.ok(postImageService.uploadImages(postId, files));
    }

    @DeleteMapping("/{postId}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
        @PathVariable Long postId,
        @PathVariable Long imageId,
        @LoginMember MemberDto memberDto
    ) {
        postImageService.deleteImage(postId, imageId, memberDto);
        return ResponseEntity.noContent().build();
    }
}
