package com.ssafy.eatBusan.place.dto;

import com.ssafy.eatBusan.place.domain.Place;

public record PlaceResponseListDto(
        Long id,
        String address,
        String area_cde,
        String name,
        Long postCnt,
        Long likeCnt
) {

    public static PlaceResponseListDto from(Place place, Long postCnt, Long likeCnt){
        return new PlaceResponseListDto(
                place.getId(),
                place.getAddress(),
                place.getAreaCode(),
                place.getName(),
                postCnt,
                likeCnt
        );
    }

}
