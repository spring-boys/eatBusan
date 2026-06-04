package com.ssafy.eatBusan.postcomment.dto;

import java.util.List;

public record PostCommentPageResponse(
    List<PostCommentDto> items,
    Long nextCursor,
    boolean hasNext
) {

    public static PostCommentPageResponse of(List<PostCommentDto> items, Long nextCursor,
        boolean hasNext) {
        return new PostCommentPageResponse(items, nextCursor, hasNext);
    }
}
