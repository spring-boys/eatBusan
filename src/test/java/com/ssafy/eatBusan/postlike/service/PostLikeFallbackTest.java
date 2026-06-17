package com.ssafy.eatBusan.postlike.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ssafy.eatBusan.member.domain.Member;
import com.ssafy.eatBusan.member.repository.MemberRepository;
import com.ssafy.eatBusan.place.Repository.PlaceRepository;
import com.ssafy.eatBusan.place.domain.Place;
import com.ssafy.eatBusan.post.domain.Post;
import com.ssafy.eatBusan.post.repository.PostRepository;
import com.ssafy.eatBusan.postlike.dto.PostLikeResponse;
import com.ssafy.eatBusan.postlike.repository.PostLikeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PostLikeFallbackTest {

    @Autowired
    private PostLikeService postLikeService;
    @Autowired
    private PostLikeCacheService postLikeCacheService;
    @Autowired
    private PostLikeRepository postLikeRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PlaceRepository placeRepository;

    private Post post;
    private Member member;

    @BeforeEach
    void setUp() {
        Place place = placeRepository.save(Place.builder()
            .code("fallback-test-place")
            .name("테스트 가게")
            .address("부산")
            .phone("051-0000-0000")
            .url("https://place.example.com")
            .x(129.0).y(35.0)
            .build());

        Member author = memberRepository.save(Member.builder()
            .email("fallback-author@test.com").pw("1234").build());

        member = memberRepository.save(Member.builder()
            .email("fallback-liker@test.com").pw("1234").build());

        post = postRepository.save(Post.builder()
            .member(author).place(place)
            .title("테스트 게시글").content("내용")
            .build());
    }

    @AfterEach
    void tearDown() {
        postLikeRepository.deleteAll();
        postRepository.deleteAll();
        memberRepository.deleteAll();
        placeRepository.deleteAll();
    }

    @Test
    void Redis_장애_시_DB만으로_좋아요_토글과_조회가_동작한다() {
        PostLikeResponse liked = postLikeService.like(post.getId(), member.getId());

        assertThat(liked.liked()).isTrue();
        assertThat(liked.likeCount()).isEqualTo(1);
        assertThat(postLikeRepository.countByPostIdAndDeletedFalse(post.getId())).isEqualTo(1);
        assertThat(postLikeCacheService.likeCount(post.getId())).isEqualTo(1);
        assertThat(postLikeCacheService.checkLiked(post.getId(), member.getId())).isTrue();

        PostLikeResponse unliked = postLikeService.like(post.getId(), member.getId());

        assertThat(unliked.liked()).isFalse();
        assertThat(unliked.likeCount()).isZero();
        assertThat(postLikeRepository.countByPostIdAndDeletedFalse(post.getId())).isZero();
        assertThat(postLikeCacheService.likeCount(post.getId())).isZero();
        assertThat(postLikeCacheService.checkLiked(post.getId(), member.getId())).isFalse();
    }
}
