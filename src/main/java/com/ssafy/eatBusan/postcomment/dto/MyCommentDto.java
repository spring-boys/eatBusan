package com.ssafy.eatBusan.postcomment.dto;

public record MyCommentDto(
    Long id,
    Long postId,
    String postTitle,
    String content,
    String createdAt
) {

}
