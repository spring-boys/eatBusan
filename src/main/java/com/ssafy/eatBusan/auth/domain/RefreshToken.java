package com.ssafy.eatBusan.auth.domain;

import com.ssafy.eatBusan.member.domain.Member;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Builder;
import lombok.Getter;

@Entity
@Getter
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private String token; // refreshToken

    @Builder
    public RefreshToken(Member member, String token){
        this.member = member;
        this.token = token;
    }

    protected RefreshToken(){}

}
