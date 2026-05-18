package com.ssafy.eatBusan.golbal.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EBException extends RuntimeException {

    private final ErrorCode errorCode;

}