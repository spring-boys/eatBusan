package com.ssafy.eatBusan.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    //Member
    MEMBER_DUPLICATE(HttpStatus.CONFLICT, "이미 등록된 회원입니다"),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "없는 회원입니다."),

    //auth
    AUTH_INVALID_LOGIN(HttpStatus.UNAUTHORIZED, "ID/PW가 틀렸습니다."),

    //jwt
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "토큰이 없습니다."),
    RTOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "토큰이 없습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    RTOKEN_INVALID(HttpStatus.UNAUTHORIZED, "토큰이 없습니다.");

    private final HttpStatus status;
    private final String message;

}
