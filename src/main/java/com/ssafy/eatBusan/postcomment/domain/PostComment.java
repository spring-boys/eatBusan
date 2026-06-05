package com.ssafy.eatBusan.postcomment.domain;

import com.ssafy.eatBusan.global.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostComment extends BaseEntity {

    Long id;
    Long postId;
    String content;
    Long memberId;
}
