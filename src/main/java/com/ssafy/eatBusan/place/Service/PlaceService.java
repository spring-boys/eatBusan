package com.ssafy.eatBusan.place.Service;

import com.ssafy.eatBusan.global.exception.EBException;
import com.ssafy.eatBusan.global.exception.ErrorCode;
import com.ssafy.eatBusan.place.Repository.PlaceRepository;
import com.ssafy.eatBusan.place.apiUtil.KakaoApiUtil;
import com.ssafy.eatBusan.place.apiUtil.dto.KakaoPlaceResponse;
import com.ssafy.eatBusan.place.apiUtil.dto.KakaoSearchResponse;
import com.ssafy.eatBusan.place.domain.Place;
import com.ssafy.eatBusan.place.dto.PlaceRequestDto;
import com.ssafy.eatBusan.place.dto.PlaceResponseDto;
import com.ssafy.eatBusan.place.dto.PlaceResponseListDto;
import com.ssafy.eatBusan.placelike.dto.PlaceLikeCntDto;
import com.ssafy.eatBusan.placelike.mapper.PlaceLikeMapper;
import com.ssafy.eatBusan.post.dto.PostCntDto;
import com.ssafy.eatBusan.post.repository.PostRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final PlaceLikeMapper placeLikeMapper;
    private final PostRepository postRepository;
    private final KakaoApiUtil kakaoApiUtil;

    private final PlaceAddressUtil placeAddressUtil;

    //TODO: 음식점 조회(좋아요 수, post 개수 포함)
    public Page<PlaceResponseListDto> findPlaceByAreaCode(String areaCode, Pageable pageable) {

        Page<Place> placePage = placeRepository.findPlaceByAreaCode(areaCode, pageable);

        List<Long> placeIds = placePage.map(Place::getId).toList();

        if(placeIds.isEmpty()) return placePage.map(place -> PlaceResponseListDto.from(place, 0L, 0L));

        Map<Long, Long> likeCnt = getPlacesLikeCnt(placeIds);
        Map<Long, Long> postCnt = getPostCnt(placeIds);

        return placePage.map(
                place -> PlaceResponseListDto.from(
                        place,
                        postCnt.getOrDefault(place.getId(), 0L),
                        likeCnt.getOrDefault(place.getId(), 0L))
        );

    }

    // 랜덤으로 부산 지역의 음식점을 가져오기
    @Transactional
    public List<PlaceResponseListDto> searchPlace(PlaceRequestDto placeRequestDto) {
        KakaoSearchResponse placeResponses = kakaoApiUtil.searchPlaces(placeRequestDto);
        saveNewPlace(placeResponses);

        List<String> placesCode = placeResponses.documents().stream().map(KakaoPlaceResponse::code).toList();
        List<Place> placeList = placeRepository.findPlacesByCodeList(placesCode);
        return toPlaceResponseListDtos(placeList);
    }

    //음식점 상세 조회
    public PlaceResponseDto getPlaceDetail(Long placeId) {
        Place place = placeRepository.findPlaceById(placeId)
                .orElseThrow(() -> new EBException(ErrorCode.PLACE_NOT_FOUND));
        return PlaceResponseDto.from(place);
    }

    // 랜덤으로 부산 지역의 음식점을 가져오기
    public List<PlaceResponseListDto> getRandomPlaces() {
        List<Place> randomPlaceList = placeRepository.getRandomPlaces(PageRequest.of(0, 20));
        return toPlaceResponseListDtos(randomPlaceList);
    }

    private List<PlaceResponseListDto> toPlaceResponseListDtos(List<Place> placeList) {
        List<Long> placeIds = placeList.stream().map(Place::getId).toList();
        Map<Long, Long> likeCnt = getPlacesLikeCnt(placeIds);
        Map<Long, Long> postCnt = getPostCnt(placeIds);

        return placeList.stream()
                .map(place -> PlaceResponseListDto.from(
                        place,
                        postCnt.getOrDefault(place.getId(), 0L),
                        likeCnt.getOrDefault(place.getId(), 0L)))
                .toList();
    }

    private Map<Long, Long> getPlacesLikeCnt(List<Long> placeIds) {
        Map<Long, Long> placeLikeMap = new HashMap<>();
        List<PlaceLikeCntDto> placeLikeCntDtoList = placeLikeMapper.countPlaceLikesByPlaceIds(placeIds);
        for (PlaceLikeCntDto cntDto : placeLikeCntDtoList) {
            placeLikeMap.put(cntDto.placeId(), cntDto.cnt());
        }
        return placeLikeMap;
    }

    private Map<Long, Long> getPostCnt(List<Long> placeIds) {
        Map<Long, Long> postMap = new HashMap<>();
        List<PostCntDto> postCntDtos = postRepository.countPostByIds(placeIds);
        for (PostCntDto cntDto : postCntDtos) {
            postMap.put(cntDto.placeId(), cntDto.cnt());
        }
        return postMap;
    }

    private void saveNewPlace(KakaoSearchResponse kakaoSearchResponse) {
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
                        .areaCode(placeAddressUtil.toAreaCode(response.address()))
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
