package com.ssafy.eatBusan.placelike.dto;

import lombok.Getter;

@Getter
public class PlaceLikeRequestDto{

    private Long id;
    private Long memberId;
    private Long placeId;

    public PlaceLikeRequestDto(Long memberId, Long placeId){
        this(null, memberId, placeId);
    }

    public PlaceLikeRequestDto(Long id, Long memberId, Long placeId){
        this.id  = id;
        this.memberId = memberId;
        this.placeId = placeId;
    }

}
