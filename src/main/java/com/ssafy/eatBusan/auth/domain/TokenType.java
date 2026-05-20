package com.ssafy.eatBusan.auth.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenType {

    ACCESS("EB_AccessToken"),
    REFRESH("EB_RefreshToken");

    private final String type;

}
