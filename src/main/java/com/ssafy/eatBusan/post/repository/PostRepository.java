package com.ssafy.eatBusan.post.repository;

import com.ssafy.eatBusan.post.domain.Post;
import com.ssafy.eatBusan.post.dto.PostCntDto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByDeletedFalse();

    Optional<Post> findByIdAndDeletedFalse(Long id);

    @Query("""
                select new com.ssafy.eatBusan.post.dto.PostCntDto(p.place.id, count(p))
                from Post p
                where p.place.id in :placeIds
                and p.deleted = false 
                group by p.place.id
            """)
    List<PostCntDto> countPostByIds(List<Long> placeIds);
}
