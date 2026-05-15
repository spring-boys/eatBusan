package com.ssafy.eatBusan.place.Repository;

import com.ssafy.eatBusan.place.domain.Place;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {
}
