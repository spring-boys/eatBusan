package com.ssafy.eatBusan.member.dto;

import com.ssafy.eatBusan.member.domain.Member;

public record MemberResponseDto(
        Long id,
        String email
) {
    public static MemberResponseDto from(Member member){
        return new MemberResponseDto(member.getId(), member.getEmail());
    }
}
