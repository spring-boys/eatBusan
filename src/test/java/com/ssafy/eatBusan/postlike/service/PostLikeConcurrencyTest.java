package com.ssafy.eatBusan.postlike.service;

import com.ssafy.eatBusan.member.domain.Member;
import com.ssafy.eatBusan.member.repository.MemberRepository;
import com.ssafy.eatBusan.place.Repository.PlaceRepository;
import com.ssafy.eatBusan.place.domain.Place;
import com.ssafy.eatBusan.post.domain.Post;
import com.ssafy.eatBusan.post.repository.PostRepository;
import com.ssafy.eatBusan.postlike.repository.PostLikeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/eatbusan_test?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PostLikeConcurrencyTest {

    private static final int THREAD_COUNT = 30;

    @Autowired private PostLikeService postLikeService;
    @Autowired private PostRepository postRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PlaceRepository placeRepository;
    @Autowired private PostLikeRepository postLikeRepository;

    private Post post;
    private List<Member> members;

    @BeforeEach
    void setUp() {
        members = new ArrayList<>();

        Place place = placeRepository.save(Place.builder()
                .code("like-test-place")
                .name("테스트 가게")
                .address("부산")
                .phone("051-0000-0000")
                .url("https://place.example.com")
                .x(129.0).y(35.0)
                .build());

        Member author = memberRepository.save(Member.builder()
                .email("author@test.com").pw("1234").build());

        for (int i = 0; i < THREAD_COUNT; i++) {
            members.add(memberRepository.save(Member.builder()
                    .email("liker-" + i + "@test.com").pw("1234").build()));
        }

        post = postRepository.save(Post.builder()
                .user(author).place(place)
                .title("테스트 게시글").content("내용")
                .build());
    }

    @AfterEach
    void tearDown() {
        // @Transactional 롤백이 없으므로 FK 순서에 맞게 직접 정리
        postLikeRepository.deleteAll();
        postRepository.deleteAll();
        memberRepository.deleteAll();
        placeRepository.deleteAll();
    }

    @Test
    void like_토글_좋아요_취소_정상_동작() {
        Long postId = post.getId();
        Long memberId = members.get(0).getId();

        boolean first  = postLikeService.like(postId, memberId); // 좋아요
        boolean second = postLikeService.like(postId, memberId); // 취소
        boolean third  = postLikeService.like(postId, memberId); // 다시 좋아요

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(third).isTrue();

        int likeCount = postRepository.findById(postId).orElseThrow().getLikeCount();
        assertThat(likeCount).isEqualTo(1);
    }

    @Test
    void DB_한계_증명_동시_좋아요_시_likeCount_Lost_Update_발생() throws InterruptedException {
        // 이 테스트는 실패해야 정상 — Race Condition(Lost Update)이 발생함을 증명한다.
        // post_like 실제 레코드 수(정확) != post.likeCount(부정확) → Redis INCR 필요
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        Long postId = post.getId();

        for (int i = 0; i < THREAD_COUNT; i++) {
            Long memberId = members.get(i).getId();
            executor.submit(() -> {
                latch.countDown();   // 준비 완료 신호
                try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                postLikeService.like(postId, memberId);
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        long actualPostLikeCount = postLikeRepository.countByPostIdAndDeletedFalse(postId);
        int likeCountInPost = postRepository.findById(postId).orElseThrow().getLikeCount();

        // 두 값이 같으면 Race Condition이 안 터진 것 (운 좋게 통과) — 보통은 다르다
        assertThat(likeCountInPost).isEqualTo((int) actualPostLikeCount);
    }
}
