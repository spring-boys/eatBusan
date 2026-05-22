package com.ssafy.eatBusan.auth.service;

import com.ssafy.eatBusan.auth.domain.RefreshToken;
import com.ssafy.eatBusan.auth.dto.RefreshTokenResponseDto;
import com.ssafy.eatBusan.auth.repository.RefreshTokenRepository;
import com.ssafy.eatBusan.member.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    //TODO: refreshToken 저장 시, 암호화해서 저장해보기?,,
    public RefreshTokenResponseDto saveRefreshToken(Member member, String refreshToken){
        refreshTokenRepository.deleteRefreshTokenByMemberId(member.getId());
        RefreshToken token = refreshTokenRepository.save(new RefreshToken(member, refreshToken));
        return RefreshTokenResponseDto.from(token);
    }

    public void deleteRefreshTokenByMemberId(Long memberId){
        refreshTokenRepository.deleteRefreshTokenByMemberId(memberId);
    }



}
