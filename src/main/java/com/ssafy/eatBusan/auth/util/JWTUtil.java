package com.ssafy.eatBusan.auth.util;

import com.ssafy.eatBusan.auth.domain.TokenType;
import com.ssafy.eatBusan.member.domain.Member;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JWTUtil{

    private final Long accessTokenTime;

    private final Long refreshTokenTime;

    private final SecretKey secretKey;

    public JWTUtil(
            @Value("${jwt.secret.key}") String secret,
            @Value("${jwt.access.time}") long accessTokenTime,
            @Value("${jwt.refresh.time}") long refreshTokenTime
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTime = accessTokenTime;
        this.refreshTokenTime = refreshTokenTime;
    }

    public String createToken(Member member, TokenType tokenType){

        Long timePeriod = 0L;

        if(TokenType.ACCESS.getType().equals(tokenType.getType())){
            timePeriod = accessTokenTime;
        }
        else{
            timePeriod = refreshTokenTime;
        }

        Date now = new Date();
        Date expire = new Date(now.getTime() + timePeriod);

        return Jwts.builder()
                .claim("id", member.getId())
                .claim("tokenType", tokenType.getType())
                .issuedAt(now)
                .expiration(expire)
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token, TokenType tokenType){
        try{
            Claims claims = getClaims(token);
            if(tokenType.getType().equals(claims.get("tokenType"))){
                return false;
            }
            //정상적인 토큰의 경우 만료가 되었는지 안되었는지만을 확인하면 됨
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token){
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getId(String token){
        token = token.substring(7);
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("id", Long.class);
    }

}
