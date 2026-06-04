package com.ssafy.eatBusan.placelike.service;

import com.ssafy.eatBusan.placelike.mapper.PlaceLikeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceLikeService {

    private final PlaceLikeMapper placeLikeMapper;

    public int test(){
        return placeLikeMapper.test();
    }

}
