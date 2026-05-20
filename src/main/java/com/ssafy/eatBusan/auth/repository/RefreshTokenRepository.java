package com.ssafy.eatBusan.auth.repository;

import com.ssafy.eatBusan.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Modifying
    @Query("Delete From RefreshToken rt where rt.member.id=:memberId")
    void deleteRefreshTokenByMemberId(Long memberId);

}
