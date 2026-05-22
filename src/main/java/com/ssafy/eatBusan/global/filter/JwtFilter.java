package com.ssafy.eatBusan.global.filter;

import com.ssafy.eatBusan.auth.util.JWTUtil;
import com.ssafy.eatBusan.global.exception.ErrorCode;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

//JWT 토큰을 통한 로그인 필터
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtFilter implements Filter {

    private final JWTUtil jwtUtil;

    private static final String [] WHITE_LIST = {
            "/api/members/login",
            "/api/members/join",
            "/api/posts/**"
    };

    private boolean isWhiteListed(String uri){
        for(String pattern : WHITE_LIST){
            if(pattern.endsWith("/**")){
                String prefix = pattern.substring(0, pattern.length() - 3);
                if (uri.startsWith(prefix)) return true;
            }
            else {
                if(uri.equals(pattern)) return true;
            }
        }
        return false;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        String uri = httpServletRequest.getRequestURI();
        if(isWhiteListed(uri)){
            chain.doFilter(request, response);
            return;
        }

        String token = httpServletRequest.getHeader("Authorization");
        if(token == null || !token.startsWith("Bearer ")){
            sendErrorResponse(httpServletResponse, ErrorCode.TOKEN_NOT_FOUND); // 토큰이 없는 경우
            return;
        }

        token = token.substring(7);
        System.out.println(token);
        if (!jwtUtil.validateToken(token)) {
            sendErrorResponse(httpServletResponse, ErrorCode.TOKEN_INVALID); // 토큰이 유효하지 않은 경우
            return;
        }

        chain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"" + errorCode.getMessage() + "\"}");
    }

}
