package com.ssafy.eatBusan.placelike.controller;

import com.ssafy.eatBusan.auth.resolver.LoginMember;
import com.ssafy.eatBusan.member.dto.MemberDto;
import com.ssafy.eatBusan.placelike.dto.PlaceLikeResponseDto;
import com.ssafy.eatBusan.placelike.service.PlaceLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PlaceLikeController {

    private final PlaceLikeService placeLikeService;

    @PostMapping("/places/{placeId}/likes")
    public ResponseEntity<PlaceLikeResponseDto> likePlace(
            @LoginMember MemberDto memberDto,
            @PathVariable Long placeId
    ) {
        PlaceLikeResponseDto responseDto = placeLikeService.createPlaceLike(memberDto.id(), placeId);
        return ResponseEntity.ok(responseDto);
    }


}
