package com.ssafy.eatBusan.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    public void saveRefreshToken(String refreshToken, HttpServletResponse response){
        Cookie cookie = new Cookie("EBRefreshToken", refreshToken);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

}
