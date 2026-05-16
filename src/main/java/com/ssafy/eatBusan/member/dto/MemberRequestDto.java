package com.ssafy.eatBusan.member.dto;

import com.ssafy.eatBusan.member.domain.Member;

public record MemberRequestDto(
        String email,
        String password
) {
}
