package com.ssafy.eatBusan.auth.repository;

import com.ssafy.eatBusan.auth.domain.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from RefreshToken rt where rt.member.id = :memberId")
    void deleteRefreshTokenByMemberId(@Param("memberId") Long memberId);

    // void → Optional로 변경
    @Query("select rt from RefreshToken rt where rt.member.id = :memberId")
    Optional<RefreshToken> findRefreshTokenByMemberId(@Param("memberId") Long memberId);
}
