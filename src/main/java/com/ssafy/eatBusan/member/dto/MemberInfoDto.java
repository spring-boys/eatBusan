package com.ssafy.eatBusan.member.dto;

import com.ssafy.eatBusan.member.domain.Member;

public record MemberInfoDto(
        String email
)
{
    public static MemberInfoDto from(Member member){
        return new MemberInfoDto(member.getEmail());
    }
}
