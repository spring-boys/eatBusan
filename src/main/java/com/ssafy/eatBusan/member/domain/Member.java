package com.ssafy.eatBusan.member.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;

@Entity
@Getter
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String pw;

    @Builder
    public Member (String email, String pw){
        this.email = email;
        this.pw = pw;
    }

    protected Member() {}
}
