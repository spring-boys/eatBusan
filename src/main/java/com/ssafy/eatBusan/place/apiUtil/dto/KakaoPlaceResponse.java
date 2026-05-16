package com.ssafy.eatBusan.place.apiUtil.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoPlaceResponse(

        @JsonProperty("id")
        String code,

        @JsonProperty("place_name")
        String name,

        @JsonProperty("place_url")
        String url,

        @JsonProperty("road_address_name")
        String address,

        String phone,

        double x,

        double y
){
}
