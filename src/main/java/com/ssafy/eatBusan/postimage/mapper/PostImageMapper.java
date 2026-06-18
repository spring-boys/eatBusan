package com.ssafy.eatBusan.postimage.mapper;

import com.ssafy.eatBusan.postimage.dto.PostImageDto;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostImageMapper {

    int saveImage(
        @Param("postId") Long postId,
        @Param("imageUrl") String imageUrl,
        @Param("imageKey") String imageKey,
        @Param("sortOrder") int sortOrder
    );

    List<PostImageDto> findByPostId(@Param("postId") Long postId);

    List<PostImageDto> findByPostIds(@Param("postIds") List<Long> postIds);
}
