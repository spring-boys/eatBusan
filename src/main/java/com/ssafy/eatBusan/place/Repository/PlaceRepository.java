package com.ssafy.eatBusan.place.Repository;

import com.ssafy.eatBusan.place.domain.Place;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    @Query("select p from Place p where p.areaCode= :areaCode")
    Page<Place> findPlaceByAreaCode(String areaCode, Pageable pageable);

    @Query("select p from Place p where p.code in :codes")
    List<Place> findPlacesByCodeList(List<String> codes);

}
