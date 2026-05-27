package com.ssafy.eatBusan.member.dto;

import jakarta.validation.constraints.Email;

public record LoginRequestDto(
        @Email
        String email,

        String password
)
{ }
