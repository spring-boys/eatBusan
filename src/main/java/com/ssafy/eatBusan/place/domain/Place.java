package com.ssafy.eatBusan.place.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;

@Entity
@Getter
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    private String name;

    private String address;

    private String areaCode;

    private String phone;

    private String url;

    private double x;

    private double y;

    @Builder
    public Place(String code, String name, String address, String areaCode, String phone, String url, double x, double y) {
        this.code = code;
        this.name = name;
        this.address = address;
        this.areaCode = areaCode;
        this.phone = phone;
        this.url = url;
        this.x = x;
        this.y = y;
    }

    protected Place() {}

}
