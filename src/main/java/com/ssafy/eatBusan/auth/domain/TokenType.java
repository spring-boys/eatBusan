package com.ssafy.eatBusan.auth.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenType {

    ACCESS("EBAccessToken"),
    REFRESH("EBRefreshToken");

    private final String type;

}
