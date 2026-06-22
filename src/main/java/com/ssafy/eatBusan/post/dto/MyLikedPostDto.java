package com.ssafy.eatBusan.post.dto;

import com.ssafy.eatBusan.post.domain.Post;
import java.time.LocalDateTime;

public record MyLikedPostDto(Long postId,
                             String title,
                             String content,
                             String email,
                             int commentCount,
                             String thumbnailUrl,
                             boolean liked,
                             LocalDateTime createdAt) {

    // liked 는 정의상 항상 true(내가 좋아요한 목록). likeCount 는 상세 조회에서 제공한다.
    public static MyLikedPostDto from(Post post, String thumbnailUrl) {
        return new MyLikedPostDto(
            post.getId(),
            post.getTitle(),
            post.getContent(),
            post.getMember().getEmail(),
            post.getCommentCount(),
            thumbnailUrl,
            true,
            post.getCreatedAt());
    }
}
