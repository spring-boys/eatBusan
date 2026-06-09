package com.ssafy.eatBusan.postimage.domain;

import com.ssafy.eatBusan.global.entity.BaseEntity;
import com.ssafy.eatBusan.post.domain.Post;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;


    @Column(nullable = false, length = 500, name = "image_url")
    private String imageUrl;
    @Column(nullable = false, length = 255, name = "image_key")
    private String imageKey;
    @Column(nullable = false, name = "sort_order")
    private int sortOrder;

    @Builder
    private PostImage(Post post, String imageUrl, String imageKey, int sortOrder) {
        this.post = post;
        this.imageUrl = imageUrl;
        this.imageKey = imageKey;
        this.sortOrder = sortOrder;
    }
}
