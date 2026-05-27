package com.ssafy.eatBusan.auth.dto;

import com.ssafy.eatBusan.auth.domain.RefreshToken;

public record RefreshTokenResponseDto(
        String refreshToken
) {

    public static RefreshTokenResponseDto from(RefreshToken refreshToken){
        return new RefreshTokenResponseDto(refreshToken.getToken());
    }

}
