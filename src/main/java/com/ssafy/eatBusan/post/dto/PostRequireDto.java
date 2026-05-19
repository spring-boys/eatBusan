package com.ssafy.eatBusan.post.dto;

import java.time.LocalDateTime;

public record PostRequireDto(
        Long userId,
        Long placeId,
        String email,
        String title,
        String content

) {
}
