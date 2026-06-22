package com.ssafy.eatBusan.postlike.repository;

import com.ssafy.eatBusan.member.domain.Member;
import com.ssafy.eatBusan.post.domain.Post;
import com.ssafy.eatBusan.postlike.domain.PostLike;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    @Query("SELECT pl FROM PostLike pl WHERE pl.post = :post AND pl.member = :member AND pl.deleted = false")
    Optional<PostLike> findByPostAndMemberDeletedFalse(Post post, Member member);

    boolean existsByPostIdAndMemberIdAndDeletedFalse(Long postId, Long memberId);

    long countByPostIdAndDeletedFalse(Long postId);

    @Query("SELECT pl.member.id FROM PostLike pl WHERE pl.post.id = :postId AND pl.deleted=false")
    List<Long> findMemberIdsByPostId(@Param("postId") Long postId);

    @Query("SELECT pl FROM PostLike pl WHERE pl.post.id = :postId AND  pl.member.id = :memberId")
    Optional<PostLike> findIncludingDeleted(@Param("postId") Long postId,
        @Param("memberId") Long memberId);

    @Query("SELECT pl.post FROM PostLike pl JOIN FETCH pl.post.member WHERE pl.member.id = :memberId AND pl.post.deleted = false AND pl.deleted = false ORDER BY pl.id DESC")
    List<Post> findLikedPostsByMemberId(@Param("memberId") Long memberId);
}
