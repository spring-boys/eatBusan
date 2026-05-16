package com.ssafy.eatBusan.place.controller;

import com.ssafy.eatBusan.place.Service.PlaceService;
import com.ssafy.eatBusan.place.apiUtil.KakaoApiUtil;
import com.ssafy.eatBusan.place.dto.PlaceRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    private final KakaoApiUtil kakaoApiUtil;

    @GetMapping("/search")
    public void searchPlace(@RequestBody(required = false) PlaceRequestDto placeRequestDto){
        placeRequestDto = new PlaceRequestDto(129.0888, 35.2295);
        kakaoApiUtil.searchPlaces(placeRequestDto.x(), placeRequestDto.y()); //결과를 list에 넣어서 반환
    }

}
