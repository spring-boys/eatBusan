package com.ssafy.eatBusan.place.apiUtil.dto;

import java.util.List;

public record KakaoSearchResponse(
        List<KakaoPlaceResponse> documents
) {
}