package com.ssafy.eatBusan.place.apiUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
@RequiredArgsConstructor
public class KakaoApiUtil {

    private final RestClient kakaoClient;

    private final String uri = "/v2/local/search/keyword.json";

    public void searchPlaces() {
        ResponseEntity<String> response = kakaoClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(uri)
                        .queryParam("query", "음식점")
                        .queryParam("x", "129.0888")
                        .queryParam("y", "35.2295")
                        .build()
                )
                .accept(MediaType.ALL)
                .retrieve()
                .toEntity(String.class);
        log.info(response.toString());
    }


}
