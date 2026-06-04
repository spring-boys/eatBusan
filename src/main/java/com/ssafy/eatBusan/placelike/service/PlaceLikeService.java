package com.ssafy.eatBusan.placelike.service;

import com.ssafy.eatBusan.global.exception.EBException;
import com.ssafy.eatBusan.global.exception.ErrorCode;
import com.ssafy.eatBusan.placelike.dto.PlaceLikeDetailResponseDto;
import com.ssafy.eatBusan.placelike.dto.PlaceLikeRequestDto;
import com.ssafy.eatBusan.placelike.dto.PlaceLikeResponseDto;
import com.ssafy.eatBusan.placelike.mapper.PlaceLikeMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PlaceLikeService {

    private final PlaceLikeMapper placeLikeMapper;

    @Transactional
    public PlaceLikeResponseDto createPlaceLike(Long memberId, Long placeId){

        if(placeLikeMapper.existsByMemberIdAndPlaceId(memberId, placeId)){
            throw new EBException(ErrorCode.PLACE_LIKE_DUPLICATE);
        }
        PlaceLikeRequestDto requestDto = new PlaceLikeRequestDto(memberId, placeId);
        placeLikeMapper.insertPlaceLike(requestDto);
        return new PlaceLikeResponseDto (requestDto.getId(), memberId, placeId);
    }

    @Transactional
    public void cancelPlaceLike(Long memberId, Long placeId){
        placeLikeMapper.deletePlaceLikeByPlaceIdAndMemberId(memberId, placeId);
    }

    public List<PlaceLikeDetailResponseDto> getPlaceLikes(Long memberId, Long lastId, int size) {
        return placeLikeMapper.findPlaceLikesByMemberId(memberId, lastId, size);
    }



}
