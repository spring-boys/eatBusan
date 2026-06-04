package com.ssafy.eatBusan.util;

import com.ssafy.eatBusan.member.domain.Member;
import com.ssafy.eatBusan.place.domain.Place;
import org.springframework.stereotype.Component;

@Component
public class TestUtil {

    public Member createTestMember(){
        Member member = new Member("test@eatBusa.ac.kr", "password");
        return member;
    }

    public Place createTestPlace(){
        Place place = new Place("641427", "와이제이식당", "부산특별시 강서구", "1111", "010-1234-5678", "https://eatBusan.ac.kr", 123.123, 456.456);
        return place;
    }

    public Place createTestPlace2(){
        Place place = new Place("641510", "에스에이치식당", "서울광역시 동내구", "2222", "010-5678-1234", "https://ssafyBUK.ac.kr", 456.456, 789.789);
        return place;
    }

}
