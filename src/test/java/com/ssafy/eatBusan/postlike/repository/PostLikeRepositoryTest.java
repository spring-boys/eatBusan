package com.ssafy.eatBusan.postlike.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.ssafy.eatBusan.member.domain.Member;
import com.ssafy.eatBusan.place.domain.Place;
import com.ssafy.eatBusan.post.domain.Post;
import com.ssafy.eatBusan.postlike.domain.PostLike;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * PostLikeRepository.findLikedPostsByMemberId 슬라이스 테스트.
 *
 * <p>외부 인프라(MySQL/Redis/S3) 없이 H2(application.properties)만으로 동작한다.
 * Redis/S3 빈을 로딩하는 @SpringBootTest 대신 JPA 슬라이스(@DataJpaTest)로 격리한다.
 * @AutoConfigureTestDatabase(replace = NONE) 로 test resources 의 H2(MySQL 모드)를 그대로 사용한다.
 * BaseEntity 의 createdAt/updatedAt 은 Hibernate @CreationTimestamp/@UpdateTimestamp 로 채워지므로
 * JPA Auditing 설정은 불필요하다. (@DataJpaTest 가 메인 설정의 @EnableJpaAuditing 을 이미 로드하므로
 * 테스트에서 중복 활성화하면 jpaAuditingHandler 빈 충돌이 난다.)
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostLikeRepositoryTest {

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private EntityManager em;

    private Member memberA;
    private Member memberB;
    private Place place;

    @BeforeEach
    void setUp() {
        memberA = persist(Member.builder().email("a@test.com").pw("pw").build());
        memberB = persist(Member.builder().email("b@test.com").pw("pw").build());
        place = persist(Place.builder()
                .code("place-code")
                .name("테스트 가게")
                .address("부산")
                .areaCode("1111")
                .phone("051-0000-0000")
                .url("https://place.example.com")
                .x(129.0)
                .y(35.0)
                .build());
    }

    @Test
    @DisplayName("정상: 좋아요한 글 2개를 최신 좋아요(pl.id DESC) 순으로 반환한다")
    void findLikedPostsByMemberId_returnsLikedPostsInDescOrder() {
        Post post1 = persistPost("첫번째 글", memberA);
        Post post2 = persistPost("두번째 글", memberA);
        persistLike(post1, memberA); // pl.id 작음
        persistLike(post2, memberA); // pl.id 큼 → 먼저 나와야 함
        flushAndClear();

        List<Post> result = postLikeRepository.findLikedPostsByMemberId(memberA.getId());

        assertThat(result)
                .extracting(Post::getId)
                .containsExactly(post2.getId(), post1.getId());
    }

    @Test
    @DisplayName("좋아요 취소 제외: PostLike.deleted=true 인 좋아요는 결과에서 빠진다")
    void findLikedPostsByMemberId_excludesCanceledLike() {
        Post liked = persistPost("살아있는 좋아요 글", memberA);
        Post canceled = persistPost("취소된 좋아요 글", memberA);
        persistLike(liked, memberA);
        PostLike canceledLike = persistLike(canceled, memberA);
        canceledLike.delete();
        flushAndClear();

        List<Post> result = postLikeRepository.findLikedPostsByMemberId(memberA.getId());

        assertThat(result)
                .extracting(Post::getId)
                .containsExactly(liked.getId());
    }

    @Test
    @DisplayName("삭제된 글 제외: post.deleted=true 면 좋아요가 살아있어도 결과에서 빠진다")
    void findLikedPostsByMemberId_excludesDeletedPost() {
        Post alive = persistPost("살아있는 글", memberA);
        Post deletedPost = persistPost("삭제된 글", memberA);
        persistLike(alive, memberA);
        persistLike(deletedPost, memberA); // 좋아요는 살아있음
        deletedPost.delete();
        flushAndClear();

        List<Post> result = postLikeRepository.findLikedPostsByMemberId(memberA.getId());

        assertThat(result)
                .extracting(Post::getId)
                .containsExactly(alive.getId());
    }

    @Test
    @DisplayName("타인 좋아요 격리: 다른 회원의 좋아요는 섞이지 않는다")
    void findLikedPostsByMemberId_isolatesOtherMembersLikes() {
        Post postA = persistPost("A가 좋아요한 글", memberA);
        Post postB = persistPost("B가 좋아요한 글", memberB);
        persistLike(postA, memberA);
        persistLike(postB, memberB);
        flushAndClear();

        List<Post> result = postLikeRepository.findLikedPostsByMemberId(memberA.getId());

        assertThat(result)
                .extracting(Post::getId)
                .containsExactly(postA.getId());
    }

    @Test
    @DisplayName("작성자 로드: JOIN FETCH 로 영속성 컨텍스트 종료 후에도 작성자 접근 시 LazyInitException 이 없다")
    void findLikedPostsByMemberId_fetchesPostMember() {
        Post post = persistPost("작성자 페치 글", memberA);
        persistLike(post, memberA);
        flushAndClear();

        List<Post> result = postLikeRepository.findLikedPostsByMemberId(memberA.getId());

        assertThat(result).hasSize(1);
        // em.clear() 이후이므로 LAZY 라면 프록시 접근에서 예외가 나야 정상.
        // JOIN FETCH 로 미리 로딩되었기 때문에 예외 없이 값에 접근 가능해야 한다.
        assertThatCode(() -> {
            String email = result.get(0).getMember().getEmail();
            assertThat(email).isEqualTo("a@test.com");
        }).doesNotThrowAnyException();
    }

    private Post persistPost(String title, Member writer) {
        return persist(Post.builder()
                .member(writer)
                .place(place)
                .title(title)
                .content(title + " 내용")
                .build());
    }

    private PostLike persistLike(Post post, Member member) {
        return persist(PostLike.of(post, member));
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
