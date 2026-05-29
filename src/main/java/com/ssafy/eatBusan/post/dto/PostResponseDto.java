package com.ssafy.eatBusan.post.dto;

import com.ssafy.eatBusan.post.domain.Post;
import java.time.LocalDateTime;

public record PostResponseDto(Long postId, Long userId, Long placeId, String email, String title, String content,
                              long viewCount, long likeCount, int commentCount, LocalDateTime createdAt,
                              LocalDateTime updatedAt) {
    public static PostResponseDto from(Post post, long likeCount) {
        return new PostResponseDto(post.getId(), post.getMember().getId(), post.getPlace().getId(), post.getMember().getEmail(), post.getTitle(), post.getContent(), post.getViewCount(), likeCount, post.getCommentCount(), post.getCreatedAt(), post.getUpdatedAt());
    }
}
