package com.ssafy.eatBusan.place.controller;

import com.ssafy.eatBusan.place.Service.PlaceService;
import com.ssafy.eatBusan.place.apiUtil.KakaoApiUtil;
import com.ssafy.eatBusan.place.dto.PlaceRequestDto;
import com.ssafy.eatBusan.place.dto.PlaceResponseDto;
import com.ssafy.eatBusan.place.dto.PlaceResponseListDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    @GetMapping("/area/{areaCode}")
    public ResponseEntity<Page<PlaceResponseListDto>> searchPlaceByAreaCode(
            @PathVariable String areaCode,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity.ok(placeService.findPlaceByAreaCode(areaCode, PageRequest.of(page, size)));
    }

    @PostMapping("/search")
    public ResponseEntity<List<PlaceResponseListDto>> searchPlace(@RequestBody(required = false) PlaceRequestDto placeRequestDto) {
        System.out.println(placeRequestDto);
        List<PlaceResponseListDto> placeList = placeService.searchPlace(placeRequestDto);
        return ResponseEntity.ok(placeList);
    }

    @GetMapping
    public ResponseEntity<List<PlaceResponseListDto>> getRandomPlaces(){
        List<PlaceResponseListDto> placeResponseList = placeService.getRandomPlaces();
        return ResponseEntity.ok(placeResponseList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlaceResponseDto> getPlaceDetail(@PathVariable Long id){
        PlaceResponseDto placeResponseDto = placeService.getPlaceDetail(id);
        return ResponseEntity.ok(placeResponseDto);
    }

}
