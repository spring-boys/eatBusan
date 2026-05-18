package com.ssafy.eatBusan.place.Repository;

import com.ssafy.eatBusan.place.domain.Place;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {
    List<Place> getPlacesByCodeIsIn(Collection<String> codes);
}
