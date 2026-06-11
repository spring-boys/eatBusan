package com.ssafy.eatBusan.post.controller;

import com.ssafy.eatBusan.post.dto.PostResponseDto;
import com.ssafy.eatBusan.post.service.PostService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 가게 상세 화면용 가게별 후기 조회 (post 도메인 소유)
@RestController
@RequestMapping("/api/places/{placeId}/posts")
@RequiredArgsConstructor
public class PlacePostController {
    private final PostService postService;

    @GetMapping
    public ResponseEntity<List<PostResponseDto>> getPostsByPlace(@PathVariable Long placeId) {
        return ResponseEntity.ok(postService.getPostsByPlace(placeId));
    }
}
