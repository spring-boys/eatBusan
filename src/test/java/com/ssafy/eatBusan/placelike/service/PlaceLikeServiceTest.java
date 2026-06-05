package com.ssafy.eatBusan.placelike.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.ssafy.eatBusan.global.exception.EBException;
import com.ssafy.eatBusan.global.exception.ErrorCode;
import com.ssafy.eatBusan.placelike.dto.PlaceLikeResponseDto;
import com.ssafy.eatBusan.placelike.mapper.PlaceLikeMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaceLikeServiceTest {

    @Mock
    private PlaceLikeMapper placeLikeMapper;

    @InjectMocks
    private PlaceLikeService placeLikeService;

    @Test
    @DisplayName("좋아요_등록_성공")
    void createPlaceLike(){

        //given
        Long memberId = 123L;
        Long placeId = 232L;
        given(placeLikeMapper.existsByMemberIdAndPlaceId(memberId, placeId)).willReturn(false);

        //when
        PlaceLikeResponseDto placeLikeResponseDto = placeLikeService.createPlaceLike(memberId, placeId);

        //then
        assertSoftly(softly -> {
            softly.assertThat(placeLikeResponseDto.placeId()).isEqualTo(placeId);
            softly.assertThat(placeLikeResponseDto.memberId()).isEqualTo(memberId);
        });

        verify(placeLikeMapper).existsByMemberIdAndPlaceId(memberId, placeId);
        verify(placeLikeMapper).insertPlaceLike(any());
    }

    @Test
    @DisplayName("좋아요_등록_실패")
    void createPlaceLike_Duplicate(){

        //given
        Long memberId = 123L;
        Long placeId = 232L;
        given(placeLikeMapper.existsByMemberIdAndPlaceId(any(), any())).willReturn(true);

        //when
        EBException e = assertThrows(
                EBException.class, () -> placeLikeService.createPlaceLike(memberId, placeId));

        //then
        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PLACE_LIKE_DUPLICATE);
        verify(placeLikeMapper).existsByMemberIdAndPlaceId(memberId, placeId);
        verifyNoMoreInteractions(placeLikeMapper);
    }

}