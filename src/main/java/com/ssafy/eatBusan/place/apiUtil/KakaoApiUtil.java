package com.ssafy.eatBusan.place.apiUtil;

import com.ssafy.eatBusan.place.Service.PlaceService;
import com.ssafy.eatBusan.place.apiUtil.dto.KakaoSearchResponse;
import com.ssafy.eatBusan.place.dto.PlaceRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
@RequiredArgsConstructor
public class KakaoApiUtil {

    private final RestClient kakaoClient;

    private final PlaceService placeService;

    private final String uri = "/v2/local/search/category.json";

    public void searchPlaces(PlaceRequestDto placeRequestDto) {
        ResponseEntity<KakaoSearchResponse> response = kakaoClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(uri)
                        .queryParam("category_group_code", "FD6")
                        .queryParam("radius", placeRequestDto.radius())
                        .queryParam("x", placeRequestDto.x())
                        .queryParam("y", placeRequestDto.y())
                        .build()
                )
                .accept(MediaType.ALL).retrieve()
                .toEntity(KakaoSearchResponse.class);

        placeService.saveNewPlace(response.getBody());

        System.out.println(response);
    }


}
