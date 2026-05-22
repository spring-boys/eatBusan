package com.ssafy.eatBusan.member.controller;

import com.ssafy.eatBusan.auth.resolver.LoginMember;
import com.ssafy.eatBusan.auth.service.RefreshTokenService;
import com.ssafy.eatBusan.member.dto.LoginRequestDto;
import com.ssafy.eatBusan.member.dto.MemberDto;
import com.ssafy.eatBusan.member.dto.MemberRequestDto;
import com.ssafy.eatBusan.member.dto.MemberResponseDto;
import com.ssafy.eatBusan.member.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/join")
    public ResponseEntity<MemberResponseDto> join(@RequestBody MemberRequestDto memberRequestDto){
        MemberResponseDto memberResponseDto = memberService.join(memberRequestDto);
        return ResponseEntity.created(URI.create(String.format("/api/members/%d", memberResponseDto.id()))).build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @Valid @RequestBody LoginRequestDto requestDto,
            HttpServletResponse httpResponse
    ){
       memberService.login(requestDto, httpResponse);
       return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@LoginMember MemberDto member, HttpServletResponse httpResponse){
        memberService.logout(member, httpResponse);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshToken(HttpServletRequest request, HttpServletResponse response){
        memberService.refreshToken(request, response);
        return ResponseEntity.noContent().build();
    }

}
