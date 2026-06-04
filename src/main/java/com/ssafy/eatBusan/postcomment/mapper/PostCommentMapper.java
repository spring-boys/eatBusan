package com.ssafy.eatBusan.postcomment.mapper;

import com.ssafy.eatBusan.postcomment.dto.PostCommentDto;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostCommentMapper {

    int saveComment(@Param("memberId") Long memberId,
        @Param("postId") Long postId,
        @Param("content") String content);

    int deleteComment(@Param("postId") Long postId,
        @Param("memberId") Long memberId,
        @Param("commentId") Long commentId);

    int restoreComment(@Param("postId") Long postId,
        @Param("memberId") Long memberId,
        @Param("commentId") Long commentId);

    int updateComment(@Param("postId") Long postId,
        @Param("memberId") Long memberId,
        @Param("commentId") Long commentId,
        @Param("content") String content);

    List<PostCommentDto> findByPostId(@Param("postId") Long postId,
        @Param("cursor") Long cursor,
        @Param("size") int size);
}
