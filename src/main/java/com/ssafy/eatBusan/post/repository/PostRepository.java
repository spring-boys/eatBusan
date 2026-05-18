package com.ssafy.eatBusan.post.repository;

import com.ssafy.eatBusan.post.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByDeletedFalse();
}
