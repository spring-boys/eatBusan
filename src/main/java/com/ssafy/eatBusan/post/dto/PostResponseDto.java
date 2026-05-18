package com.ssafy.eatBusan.post.dto;

import com.ssafy.eatBusan.member.domain.Member;
import com.ssafy.eatBusan.post.domain.Post;

import java.time.LocalDateTime;

public record PostResponseDto(Long userId, String email, String title, String content, int viewCount, int likeCount,
                              int commentCount, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static PostResponseDto from(Post post) {
        return new PostResponseDto(post.getUser().getId(), post.getUser().getEmail(), post.getTitle(), post.getContent(), post.getViewCount(), post.getLikeCount(), post.getCommentCount(), post.getCreatedAt(), post.getUpdatedAt());
    }
}
