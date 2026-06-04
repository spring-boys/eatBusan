package com.ssafy.eatBusan.placelike.mapper;

import com.ssafy.eatBusan.placelike.dto.PlaceLikeRequestDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PlaceLikeMapper {

    void insertPlaceLike(PlaceLikeRequestDto dto);

    boolean existsByMemberIdAndPlaceId(@Param("memberId") Long memberId, @Param("placeId") Long placeId);

}
