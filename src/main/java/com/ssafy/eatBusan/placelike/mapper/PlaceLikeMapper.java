package com.ssafy.eatBusan.placelike.mapper;

import com.ssafy.eatBusan.placelike.dto.PlaceLikeCntDto;
import com.ssafy.eatBusan.placelike.dto.PlaceLikeDetailResponseDto;
import com.ssafy.eatBusan.placelike.dto.PlaceLikeRequestDto;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PlaceLikeMapper {

    void insertPlaceLike(PlaceLikeRequestDto dto);

    boolean existsByMemberIdAndPlaceId(@Param("memberId") Long memberId, @Param("placeId") Long placeId);

    void deletePlaceLikeByPlaceIdAndMemberId(@Param("memberId") Long memberId, @Param("placeId") Long placeId);

    List<PlaceLikeDetailResponseDto> findPlaceLikesByMemberId(
            @Param("memberId") Long memberId,
            @Param("lastId") Long lastId,
            @Param("size") int size
    );

    List<PlaceLikeCntDto> countPlaceLikesByPlaceIds(@Param("placeIds") List<Long> placeIds);

    Long countPlaceLikesByPlaceId(@Param("placeId") Long placeId);

}
