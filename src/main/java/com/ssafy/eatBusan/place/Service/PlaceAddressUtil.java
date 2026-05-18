package com.ssafy.eatBusan.place.Service;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PlaceAddressUtil {

    private final Map<String, String> areaMap = Map.ofEntries(
            Map.entry("중구","26110"), Map.entry("서구","26140"), Map.entry("동구","26170"),
            Map.entry("영도구","26200"), Map.entry("부산진구","26230"), Map.entry("동래구","26260"),
            Map.entry("남구","26290"), Map.entry("북구","26320"), Map.entry("해운대구","26350"),
            Map.entry("사하구","26380"), Map.entry("금정구","26410"), Map.entry("강서구","26440"),
            Map.entry("연제구","26470"), Map.entry("수영구","26500"), Map.entry("사상구","26530"),
            Map.entry("기장군","26710")
    );

    public String toAreaCode(String gugun){
        System.out.println(gugun);
        return areaMap.getOrDefault(gugun, "기타");
    }

}
