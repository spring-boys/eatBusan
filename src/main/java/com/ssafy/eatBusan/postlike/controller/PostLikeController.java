package com.ssafy.eatBusan.postlike.controller;

import com.ssafy.eatBusan.auth.resolver.LoginMember;
import com.ssafy.eatBusan.member.dto.MemberDto;
import com.ssafy.eatBusan.postlike.dto.PostLikeResponse;
import com.ssafy.eatBusan.postlike.service.PostLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostLikeController {

    private final PostLikeService postLikeService;

    @PostMapping("/{postId}/likes")
    public ResponseEntity<PostLikeResponse> like(
        @PathVariable Long postId,
        @LoginMember MemberDto loginMember
    ) {
        PostLikeResponse postLikeResponse = postLikeService.like(postId, loginMember.id());
        // 토글의 경우 200으로 일괄 처리하되, ResponseDTO를 통해 현재 상태를 넘겨줌
        return ResponseEntity.status(HttpStatus.OK).body(postLikeResponse);
    }
}
