package com.ssafy.eatBusan.postlike.controller;

import com.ssafy.eatBusan.postlike.service.PostLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/postlike")
@RequiredArgsConstructor
public class PostLikeController {
    private final PostLikeService postLikeService;

    @PostMapping("/{postId}/likes")
    public ResponseEntity<Void> like(@PathVariable Long postId, @RequestParam Long memberId) {
        if (postLikeService.like(postId, memberId)) {
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
