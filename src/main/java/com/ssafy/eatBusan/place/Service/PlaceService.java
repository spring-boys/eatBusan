package com.ssafy.eatBusan.place.Service;

import com.ssafy.eatBusan.place.Repository.PlaceRepository;
import com.ssafy.eatBusan.place.apiUtil.dto.KakaoSearchResponse;
import com.ssafy.eatBusan.place.domain.Place;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;

    //TODO: 음식점 정보 (단순)저장

    //TODO: 음식점 조회

    //TODO: 음식점 이름으로 조회


    @Transactional
    public void saveNewPlace(KakaoSearchResponse kakaoSearchResponse) {
        List<String> searchResult = kakaoSearchResponse.documents().stream()
                .map(kakaoPlaceResponse -> kakaoPlaceResponse.code())
                .toList();

        List<Place> placeList = kakaoSearchResponse.documents()
                .stream()
                .map(kakaoPlaceResponse -> Place.builder()
                        .code(kakaoPlaceResponse.code())
                        .name(kakaoPlaceResponse.name())
                        .address(kakaoPlaceResponse.address())
                        .phone(kakaoPlaceResponse.phone())
                        .url(kakaoPlaceResponse.url())
                        .x(kakaoPlaceResponse.x())
                        .y(kakaoPlaceResponse.y())
                        .build()
                )
                .toList();

        placeRepository.saveAll(placeList);

    }


}
