package com.ssafy.eatBusan.post.controller;

import com.ssafy.eatBusan.post.dto.PostRequireDto;
import com.ssafy.eatBusan.post.dto.PostResponseDto;
import com.ssafy.eatBusan.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    @GetMapping
    public ResponseEntity<List<PostResponseDto>> getPostList() {
        return ResponseEntity.ok(postService.getAllPost());
    }

    @PostMapping("/regist")
    public ResponseEntity<PostResponseDto> registPost(@RequestBody PostRequireDto req) {
        return ResponseEntity.ok(postService.writePost(req));
    }
}
