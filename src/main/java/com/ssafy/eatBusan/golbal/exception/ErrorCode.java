package com.ssafy.eatBusan.golbal.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    //Member
    MEMBER_DUPLICATE(HttpStatus.CONFLICT, "이미 등록된 회원입니다");

    private final HttpStatus status;
    private final String message;

}
