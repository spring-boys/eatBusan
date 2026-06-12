package com.ssafy.eatBusan.place.dto;

import com.ssafy.eatBusan.place.domain.Place;

public record PlaceResponseDto(
        Long id,
        String address,
        String area_code,
        String name,
        String phone,
        String url,
        Long likeCnt,
        boolean myLike
) {

    public static PlaceResponseDto from(Place place, Long likeCnt, boolean myLike){
        return new PlaceResponseDto(
                place.getId(),
                place.getAddress(),
                place.getAreaCode(),
                place.getName(),
                place.getPhone(),
                place.getUrl(),
                likeCnt,
                myLike
        );
    }

}
