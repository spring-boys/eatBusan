package com.ssafy.eatBusan.postcomment.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.ssafy.eatBusan.member.domain.Member;
import com.ssafy.eatBusan.member.repository.MemberRepository;
import com.ssafy.eatBusan.place.Repository.PlaceRepository;
import com.ssafy.eatBusan.place.domain.Place;
import com.ssafy.eatBusan.post.domain.Post;
import com.ssafy.eatBusan.post.repository.PostRepository;
import com.ssafy.eatBusan.postcomment.dto.MyCommentDto;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * PostCommentMapper.findByMemberId("내가 작성한 댓글 목록") 통합 테스트.
 *
 * <p>JPA가 ddl 로 테이블을 만들고 MyBatis 가 같은 테이블을 조회하는 혼용 구조라
 * 매퍼만 슬라이스(@MybatisTest)하면 테이블이 없어 깨진다. 그래서 기존 PostServiceTests 와
 * 동일하게 @SpringBootTest(H2) + @Transactional(롤백)으로 검증한다.
 *
 * <p>이 한 클래스가 직접 잡는 회귀: SELECT 컬럼 순서↔record 생성자 순서, 컬럼 목록 쉼표,
 * p.deleted 필터(삭제 글 제외), 작성자 격리.
 */
@SpringBootTest
@Transactional
class PostCommentMapperTest {

    @Autowired
    private PostCommentMapper postCommentMapper;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PlaceRepository placeRepository;
    @Autowired
    private PostRepository postRepository;

    private Member me;
    private Place place;

    @BeforeEach
    void setUp() {
        me = memberRepository.save(Member.builder().email("me@test.com").pw("1234").build());
        place = placeRepository.save(Place.builder()
                .code("place-code").name("테스트 가게").address("부산")
                .phone("051-0000-0000").url("https://place.example.com")
                .x(129.0).y(35.0).build());
    }

    private Post savePost(String title) {
        return postRepository.save(Post.builder()
                .member(me).place(place).title(title).content("내용").build());
    }

    @Test
    @DisplayName("내가 작성한 댓글을 postTitle 포함해 최신순(id DESC)으로 반환한다")
    void findByMemberId_returnsMyCommentsWithPostTitle() {
        Post p1 = savePost("첫글");
        Post p2 = savePost("둘째글");
        postCommentMapper.saveComment(me.getId(), p1.getId(), "p1댓글");
        postCommentMapper.saveComment(me.getId(), p2.getId(), "p2댓글");

        List<MyCommentDto> result = postCommentMapper.findByMemberId(me.getId());

        assertThat(result).hasSize(2);
        // 컬럼 순서 회귀 방지: content 가 content 자리에, postId 가 postId 자리에 와야 한다.
        assertThat(result.get(0).postId()).isEqualTo(p2.getId());
        assertThat(result.get(0).postTitle()).isEqualTo("둘째글");
        assertThat(result.get(0).content()).isEqualTo("p2댓글");
        // 최신순(id DESC)
        assertThat(result.get(1).postTitle()).isEqualTo("첫글");
        assertThat(result.get(1).content()).isEqualTo("p1댓글");
    }

    @Test
    @DisplayName("삭제된 글의 댓글은 제외한다 (p.deleted = false)")
    void findByMemberId_excludesDeletedPostComments() {
        Post alive = savePost("살아있는글");
        Post deleted = savePost("삭제된글");
        postCommentMapper.saveComment(me.getId(), alive.getId(), "유지댓글");
        postCommentMapper.saveComment(me.getId(), deleted.getId(), "제외댓글");
        deleted.delete();
        postRepository.flush();

        List<MyCommentDto> result = postCommentMapper.findByMemberId(me.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).postTitle()).isEqualTo("살아있는글");
        assertThat(result.get(0).content()).isEqualTo("유지댓글");
    }

    @Test
    @DisplayName("다른 회원의 댓글은 섞이지 않는다")
    void findByMemberId_isolatesOtherMembers() {
        Member other = memberRepository.save(Member.builder().email("other@test.com").pw("1234").build());
        Post p = savePost("공용글");
        postCommentMapper.saveComment(me.getId(), p.getId(), "내댓글");
        postCommentMapper.saveComment(other.getId(), p.getId(), "남의댓글");

        List<MyCommentDto> result = postCommentMapper.findByMemberId(me.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).content()).isEqualTo("내댓글");
    }
}
