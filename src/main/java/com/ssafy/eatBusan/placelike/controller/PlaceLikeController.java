package com.ssafy.eatBusan.placelike.controller;

import com.ssafy.eatBusan.auth.resolver.LoginMember;
import com.ssafy.eatBusan.member.dto.MemberDto;
import com.ssafy.eatBusan.placelike.dto.PlaceLikeDetailResponseDto;
import com.ssafy.eatBusan.placelike.dto.PlaceLikeResponseDto;
import com.ssafy.eatBusan.placelike.service.PlaceLikeService;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PlaceLikeController {

    private final PlaceLikeService placeLikeService;

    // 좋아요 누르기
    @PostMapping("/places/{placeId}/likes")
    public ResponseEntity<Void> likePlace(
            @LoginMember MemberDto memberDto,
            @PathVariable Long placeId
    ) {
        PlaceLikeResponseDto responseDto = placeLikeService.createPlaceLike(memberDto.id(), placeId);
        return ResponseEntity.created(URI.create(String.format("/api/places/likes/%d", responseDto.id()))).build();
    }

    // placeId 기반 좋아요 지우기
    @DeleteMapping("/places/{placeId}/likes")
    public ResponseEntity<Void> cancelLikePlace(
            @LoginMember MemberDto memberDto,
            @PathVariable Long placeId
    ) {
        placeLikeService.cancelPlaceLike(memberDto.id(), placeId);
        return ResponseEntity.noContent().build();
    }

    // 좋아요 누른 식당 목록 조회
    @GetMapping("/places/likes/my")
    public ResponseEntity<List<PlaceLikeDetailResponseDto>> getPlaceLikes(
            @LoginMember MemberDto memberDto,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "10") int size
    ){
        List<PlaceLikeDetailResponseDto> result = placeLikeService.getPlaceLikes(memberDto.id(), lastId, size);
        return ResponseEntity.ok(result);
    }


}
