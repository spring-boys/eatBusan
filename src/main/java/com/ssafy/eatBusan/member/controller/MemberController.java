package com.ssafy.eatBusan.member.controller;

import com.ssafy.eatBusan.member.dto.MemberRequestDto;
import com.ssafy.eatBusan.member.dto.MemberResponseDto;
import com.ssafy.eatBusan.member.service.MemberService;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    //TODO: 회원 가입
    @PostMapping("/join")
    public ResponseEntity<MemberResponseDto> join(@RequestBody MemberRequestDto memberRequestDto){
        MemberResponseDto memberResponseDto = memberService.join(memberRequestDto);
        return ResponseEntity.created(URI.create(String.format("/api/members/%d", memberResponseDto.id()))).build();
    }

    //TODO : 로그인
    public void login(@Valid @RequestBody LoginRequestDto requestDto){
        memberService.
    }

    //TODO : 로그아웃
    public void logout(){

    }


}
