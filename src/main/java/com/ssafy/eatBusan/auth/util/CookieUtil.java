package com.ssafy.eatBusan.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "EBRefreshToken";
    private static final String COOKIE_PATH = "/";

    public void saveRefreshToken(String refreshToken, HttpServletResponse response){
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .path(COOKIE_PATH)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void invalidateRefreshToken(HttpServletResponse response){
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public Optional<String> getRefreshToken(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();

        if(cookies == null) return Optional.empty();

        for(Cookie cookie : cookies) {
            if (cookie.getName().equals(REFRESH_TOKEN_COOKIE_NAME)) {
                return Optional.of(cookie.getValue());
            }
        }

        return Optional.empty();
    }

}
