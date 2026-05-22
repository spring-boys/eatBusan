package com.ssafy.eatBusan.postlike.repository;

import com.ssafy.eatBusan.member.domain.Member;
import com.ssafy.eatBusan.post.domain.Post;
import com.ssafy.eatBusan.postlike.domain.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByPostIdAndMemberId(Long postId, Long memberId);
    boolean existsByPostIdAndMemberIdAndDeletedFalse(Long postId, Long memberId);
    Long countByPostIdAndDeletedFalse(Long postId);
}
