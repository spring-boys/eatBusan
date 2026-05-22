package com.ssafy.eatBusan.auth.util;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    public void saveRefreshToken(String refreshToken, HttpServletResponse response){
        ResponseCookie cookie = ResponseCookie.from("EBRefreshToken", refreshToken)
                .httpOnly(true)
                .path("/")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void invalidateRefreshToken(HttpServletResponse response){
        ResponseCookie cookie = ResponseCookie.from("EBRefreshToken", null)
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

}
