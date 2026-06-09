package com.ssafy.eatBusan.postimage.dto;

public record PostImageDto(
    Long id,
    Long postId,
    String imageUrl,
    String imageKey,
    int sortOrder
) {
}
