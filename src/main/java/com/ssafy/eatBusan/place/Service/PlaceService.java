package com.ssafy.eatBusan.place.Service;

import com.ssafy.eatBusan.global.exception.EBException;
import com.ssafy.eatBusan.global.exception.ErrorCode;
import com.ssafy.eatBusan.place.Repository.PlaceRepository;
import com.ssafy.eatBusan.place.apiUtil.dto.KakaoSearchResponse;
import com.ssafy.eatBusan.place.domain.Place;
import com.ssafy.eatBusan.place.dto.PlaceResponseDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;

    private final PlaceAddressUtil placeAddressUtil;

    //TODO: 음식점 정보 (단순)저장

    //TODO: 음식점 조회
    public Page<PlaceResponseDto> findPlaceByAreaCode(String areaCode, Pageable pageable){
        return placeRepository.findPlaceByAreaCode(areaCode, pageable)
                .map(PlaceResponseDto::from);
    }

    //음식점 상세 조회
    public PlaceResponseDto getPlaceDetail(Long placeId){
        Place place = placeRepository.findPlaceById(placeId)
                .orElseThrow(() -> new EBException(ErrorCode.PLACE_NOT_FOUND));
        return PlaceResponseDto.from(place);
    }

    // 랜덤으로 부산 지역의 음식점을 가져오기
    public List<PlaceResponseDto> getRandomPlaces(){
        return placeRepository.getRandomPlaces(PageRequest.of(0, 20))
                .stream()
                .map(PlaceResponseDto::from)
                .toList();
    }

    @Transactional
    public void saveNewPlace(KakaoSearchResponse kakaoSearchResponse) {
        List<String> placeList = kakaoSearchResponse.documents().stream()
                .map(kakaoPlaceResponse -> kakaoPlaceResponse.code())
                .toList();

        List<String> existPlace = placeRepository.findPlacesByCodeList(placeList)
                .stream()
                .map(place -> place.getCode())
                .toList();

        List<Place> newPlaceList = kakaoSearchResponse.documents()
                .stream()
                .filter(response -> !existPlace.contains(response.code()))
                .map(response -> Place.builder()
                        .code(response.code())
                        .name(response.name())
                        .areaCode(placeAddressUtil.toAreaCode(response.address().split(" ")[1]))
                        .address(response.address())
                        .phone(response.phone())
                        .url(response.url())
                        .x(response.x())
                        .y(response.y())
                        .build()
                )
                .toList();

        placeRepository.saveAll(newPlaceList);
    }


}
