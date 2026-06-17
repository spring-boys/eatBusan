package com.ssafy.eatBusan.post.dto;

import com.ssafy.eatBusan.post.domain.Post;

public record MyLikedPostDto(Long postId,
                             String title,
                             boolean liked) {

    public static MyLikedPostDto from(Post post) {
        return new MyLikedPostDto(post.getId(), post.getTitle(), true);
    }
}
