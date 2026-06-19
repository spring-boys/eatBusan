package com.ssafy.eatBusan.postcomment.dto;

public record PostCommentDto(
    Long id,
    String content,
    String createdAt,
    String email
) {

}
