package com.ssafy.eatBusan.placelike.service;

import com.ssafy.eatBusan.global.exception.EBException;
import com.ssafy.eatBusan.global.exception.ErrorCode;
import com.ssafy.eatBusan.placelike.dto.PlaceLikeRequestDto;
import com.ssafy.eatBusan.placelike.dto.PlaceLikeResponseDto;
import com.ssafy.eatBusan.placelike.mapper.PlaceLikeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceLikeService {

    private final PlaceLikeMapper placeLikeMapper;

    public PlaceLikeResponseDto createPlaceLike(Long memberId, Long placeId){

        if(placeLikeMapper.existsByMemberIdAndPlaceId(memberId, placeId)){
            throw new EBException(ErrorCode.PLACE_LIKE_DUPLICATE);
        }
        PlaceLikeRequestDto requestDto = new PlaceLikeRequestDto(memberId, placeId);
        placeLikeMapper.insertPlaceLike(requestDto);
        return new PlaceLikeResponseDto (requestDto.getId(), memberId, placeId);
    }

}
