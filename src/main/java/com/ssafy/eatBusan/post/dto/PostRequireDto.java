package com.ssafy.eatBusan.post.dto;

public record PostRequireDto(
        Long userId,
        Long placeId,
        String email,
        String title,
        String content

) {
}
