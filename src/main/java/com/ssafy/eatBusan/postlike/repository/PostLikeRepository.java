package com.ssafy.eatBusan.postlike.repository;

import com.ssafy.eatBusan.postlike.domain.PostLike;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByPostIdAndMemberId(Long postId, Long memberId);
    boolean existsByPostIdAndMemberIdAndDeletedFalse(Long postId, Long memberId);
    Long countByPostIdAndDeletedFalse(Long postId);
}
