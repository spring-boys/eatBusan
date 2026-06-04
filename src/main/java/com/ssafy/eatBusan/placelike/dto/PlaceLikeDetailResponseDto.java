package com.ssafy.eatBusan.placelike.dto;

public record PlaceLikeDetailResponseDto(
        Long placeLikeId,
        Long placeId,
        String code,
        String name,
        String address,
        String areaCode,
        String phone,
        String url
) {
}