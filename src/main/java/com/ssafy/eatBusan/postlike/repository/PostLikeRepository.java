package com.ssafy.eatBusan.postlike.repository;

import com.ssafy.eatBusan.member.domain.Member;
import com.ssafy.eatBusan.post.domain.Post;
import com.ssafy.eatBusan.postlike.domain.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    @Query("SELECT pl FROM PostLike pl WHERE pl.post = :post AND pl.member = :member AND pl.deleted = false")
    Optional<PostLike> findByPostAndMemberDeletedFalse(Post post, Member member);
    boolean existsByPostAndMemberAndDeletedFalse(Post post, Member member);
}
