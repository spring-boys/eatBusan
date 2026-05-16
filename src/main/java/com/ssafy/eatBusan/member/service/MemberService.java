package com.ssafy.eatBusan.member.service;

import com.ssafy.eatBusan.golbal.exception.EBException;
import com.ssafy.eatBusan.golbal.exception.ErrorCode;
import com.ssafy.eatBusan.member.domain.Member;
import com.ssafy.eatBusan.member.dto.MemberRequestDto;
import com.ssafy.eatBusan.member.dto.MemberResponseDto;
import com.ssafy.eatBusan.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberResponseDto join(MemberRequestDto memberRequestDto){
        memberRepository.findMemberByEmail(memberRequestDto.email())
                .orElseThrow(() -> new EBException(ErrorCode.MEMBER_DUPLICATE));

        Member member = Member.builder()
                .email(memberRequestDto.email())
                .pw(memberRequestDto.password())
                .build();

        return MemberResponseDto.from(member);
    }




}
