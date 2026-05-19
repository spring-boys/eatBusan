package com.ssafy.eatBusan.place.dto;

public record PlaceRequestDto (
        Double x,
        Double y,

        //거리, m단위
        Integer radius
){
}
