package com.ssafy.eatBusan.member.dto;

import com.ssafy.eatBusan.member.domain.Member;

public record MemberRequestDto(
        String email,
        String password
) {

    public Member toEntity(){
        return new Member(email, password);
    }
}
