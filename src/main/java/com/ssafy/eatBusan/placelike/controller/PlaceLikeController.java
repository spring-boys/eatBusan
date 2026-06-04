package com.ssafy.eatBusan.placelike.controller;

import com.ssafy.eatBusan.placelike.service.PlaceLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PlaceLikeController {

    private final PlaceLikeService placeLikeService;

    @GetMapping("/test")
    public ResponseEntity<Integer> test() {
        int result = placeLikeService.test();
        return ResponseEntity.ok(result);
    }


}
