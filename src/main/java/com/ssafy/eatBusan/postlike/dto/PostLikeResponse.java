package com.ssafy.eatBusan.postlike.dto;

public record PostLikeResponse(
        boolean liked,
        long likeCount
) {
    public static PostLikeResponse of(boolean liked, long likeCount){
        return new PostLikeResponse(liked, likeCount);
    }
}
