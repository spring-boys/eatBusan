package com.ssafy.eatBusan.postcomment.controller;

import com.ssafy.eatBusan.auth.resolver.LoginMember;
import com.ssafy.eatBusan.member.dto.MemberDto;
import com.ssafy.eatBusan.postcomment.dto.PostCommentPageResponse;
import com.ssafy.eatBusan.postcomment.dto.PostCommentRequestDto;
import com.ssafy.eatBusan.postcomment.service.PostCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostCommentController {

    private final PostCommentService postCommentService;

    @GetMapping("/{postId}/comments")
    public ResponseEntity<PostCommentPageResponse> getCommentsPage(@PathVariable Long postId,
        @RequestParam(required = false) Long cursor,
        @RequestParam(defaultValue = "10") int size) {
        PostCommentPageResponse comments = postCommentService.findByPostId(postId, cursor, size);
        return ResponseEntity.status(HttpStatus.OK).body(comments);
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<Void> postComments(@PathVariable Long postId,
        @RequestBody @Valid PostCommentRequestDto requestDto,
        @LoginMember MemberDto member) {
        postCommentService.save(postId, requestDto.content(), member.id());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long postId,
        @PathVariable Long commentId, @LoginMember MemberDto member) {
        postCommentService.delete(postId, commentId, member.id());
        return ResponseEntity.status(HttpStatus.OK).build();
    }


    @PatchMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> updateComment(@PathVariable Long postId,
        @PathVariable Long commentId,
        @RequestBody @Valid PostCommentRequestDto requestDto,
        @LoginMember MemberDto member) {
        postCommentService.update(postId, commentId, requestDto.content(), member.id());
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
