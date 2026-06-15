package com.ssafy.eatBusan.postimage.dto;

import java.util.List;

public record S3DeleteEvent(List<String> keys) {

}
