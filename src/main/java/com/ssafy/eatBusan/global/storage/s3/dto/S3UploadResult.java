package com.ssafy.eatBusan.global.storage.s3.dto;

public record S3UploadResult(
    String imageUrl,
    String imageKey
) {

}
