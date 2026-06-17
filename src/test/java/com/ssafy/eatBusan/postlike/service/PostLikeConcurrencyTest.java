package com.ssafy.eatBusan.postlike.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ssafy.eatBusan.member.domain.Member;
import com.ssafy.eatBusan.member.repository.MemberRepository;
import com.ssafy.eatBusan.place.Repository.PlaceRepository;
import com.ssafy.eatBusan.place.domain.Place;
import com.ssafy.eatBusan.post.domain.Post;
import com.ssafy.eatBusan.post.repository.PostRepository;
import com.ssafy.eatBusan.postlike.domain.PostLike;
import com.ssafy.eatBusan.postlike.repository.PostLikeRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:mysql://localhost:3306/eatbusan_test?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PostLikeConcurrencyTest {

    private static final int THREAD_COUNT = 30;

    @Autowired
    private PostLikeService postLikeService;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PlaceRepository placeRepository;
    @Autowired
    private PostLikeRepository postLikeRepository;
    @Autowired
    private PostLikeCacheService postLikeCacheService;
    @Autowired
    private StringRedisTemplate redisTemplate;

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
            .member(author).place(place)
            .title("테스트 게시글").content("내용")
            .build());

        deleteRedisKeys(post.getId());
    }

    @AfterEach
    void tearDown() {
        deleteRedisKeys(post.getId());

        // @Transactional 롤백이 없으므로 FK 순서에 맞게 직접 정리
        postLikeRepository.deleteAll();
        postRepository.deleteAll();
        memberRepository.deleteAll();
        placeRepository.deleteAll();
    }

    @Test
    void 동시_좋아요_30명_Redis_SCARD_와_DB_COUNT_일치() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        Queue<Throwable> failures = new ConcurrentLinkedQueue<>();

        Long postId = post.getId();

        for (Member member : members) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    postLikeService.like(postId, member.getId());
                } catch (Throwable e) {
                    failures.add(e);
                }
            });
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        assertThat(failures).isEmpty();

        long dbCount = postLikeRepository.countByPostIdAndDeletedFalse(postId);
        long redisScard = postLikeCacheService.likeCount(postId);

        assertThat(dbCount).isEqualTo(THREAD_COUNT);
        assertThat(redisScard).isEqualTo(dbCount);
    }

    @Test
    void Redis_키가_비어_있으면_DB_기준으로_다시_bootstrap한다() {
        Member member = members.get(0);
        postLikeRepository.saveAndFlush(PostLike.of(post, member));
        deleteRedisKeys(post.getId());

        assertThat(postLikeCacheService.checkLiked(post.getId(), member.getId())).isTrue();
        assertThat(postLikeCacheService.likeCount(post.getId())).isEqualTo(1);
    }

    private void deleteRedisKeys(Long postId) {
        redisTemplate.delete(List.of(
            "post:likes:" + postId,
            "post:likes:" + postId + ":init",
            "post:likes:" + postId + ":lock"
        ));
    }
}
