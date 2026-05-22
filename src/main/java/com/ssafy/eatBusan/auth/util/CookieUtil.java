package com.ssafy.eatBusan.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;
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

    public Optional<String> getRefreshToken(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();

        if(cookies == null) return Optional.empty();

        for(Cookie cookie : cookies) {
            if (cookie.getName().equals("EBRefreshToken")) {
                return Optional.of(cookie.getValue());
            }
        }

        return Optional.empty();
    }

}
