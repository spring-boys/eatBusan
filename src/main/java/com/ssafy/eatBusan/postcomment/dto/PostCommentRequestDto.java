package com.ssafy.eatBusan.postcomment.dto;

import jakarta.validation.constraints.NotBlank;

public record PostCommentRequestDto(
    @NotBlank(message = "댓글은 공백이 될 수 없습니다.") String content
) {

}
