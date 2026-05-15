package com.ssafy.eatBusan.place.controller;

import com.ssafy.eatBusan.place.Service.PlaceService;
import com.ssafy.eatBusan.place.apiUtil.KakaoApiUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    private final KakaoApiUtil kakaoApiUtil;

    @GetMapping("/search")
    public void serchPlace(){
        System.out.println("search");
        kakaoApiUtil.searchPlaces();
    }

}
