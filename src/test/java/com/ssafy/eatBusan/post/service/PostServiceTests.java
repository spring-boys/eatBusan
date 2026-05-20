package com.ssafy.eatBusan.post.service;

import com.ssafy.eatBusan.golbal.exception.EBException;
import com.ssafy.eatBusan.member.domain.Member;
import com.ssafy.eatBusan.member.repository.MemberRepository;
import com.ssafy.eatBusan.post.domain.Post;
import com.ssafy.eatBusan.post.dto.PostRequireDto;
import com.ssafy.eatBusan.post.dto.PostResponseDto;
import com.ssafy.eatBusan.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class PostServiceTests {

    private static final String MEMBER_EMAIL = "post-service@test.com";

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        memberRepository.save(Member.builder()
                .email(MEMBER_EMAIL)
                .pw("1234")
                .build());
    }

    @Test
    void writePost_게시글을_작성한다() {
        PostRequireDto req = createRequest("testTitle", "testContent");

        PostResponseDto response = postService.writePost(req);

        assertThat(response.postId()).isNotNull();
        assertThat(response.email()).isEqualTo(MEMBER_EMAIL);
        assertThat(response.title()).isEqualTo("testTitle");
        assertThat(response.content()).isEqualTo("testContent");
        assertThat(response.viewCount()).isZero();
        assertThat(response.likeCount()).isZero();
        assertThat(response.commentCount()).isZero();
    }

    @Test
    void getAllPost_삭제되지_않은_게시글만_조회한다() {
        PostResponseDto first = createPost("firstTitle", "firstContent");
        PostResponseDto second = createPost("secondTitle", "secondContent");
        postService.deletePost(first.postId());

        List<PostResponseDto> posts = postService.getAllPost();

        assertThat(posts)
                .extracting(PostResponseDto::postId)
                .containsExactly(second.postId());
    }

    @Test
    void getPost_상세조회시_조회수가_증가한다() {
        PostResponseDto created = createPost("testTitle", "testContent");

        PostResponseDto firstRead = postService.getPost(created.postId());
        PostResponseDto secondRead = postService.getPost(created.postId());

        assertThat(firstRead.viewCount()).isEqualTo(1);
        assertThat(secondRead.viewCount()).isEqualTo(2);
    }

    @Test
    void updatePost_제목과_내용을_수정한다() {
        PostResponseDto created = createPost("oldTitle", "oldContent");
        PostRequireDto updateReq = createRequest("newTitle", "newContent");

        PostResponseDto updated = postService.updatePost(updateReq, created.postId());

        assertThat(updated.postId()).isEqualTo(created.postId());
        assertThat(updated.title()).isEqualTo("newTitle");
        assertThat(updated.content()).isEqualTo("newContent");

        Post post = postRepository.findByIdAndDeletedFalse(created.postId()).orElseThrow();
        assertThat(post.getTitle()).isEqualTo("newTitle");
        assertThat(post.getContent()).isEqualTo("newContent");
    }

    @Test
    void deletePost_게시글을_soft_delete한다() {
        PostResponseDto created = createPost("testTitle", "testContent");

        postService.deletePost(created.postId());

        assertThat(postRepository.findByIdAndDeletedFalse(created.postId())).isEmpty();
        assertThat(postRepository.findById(created.postId()))
                .isPresent()
                .get()
                .extracting(Post::isDeleted)
                .isEqualTo(true);
        assertThatThrownBy(() -> postService.getPost(created.postId()))
                .isInstanceOf(EBException.class);
    }

    private PostResponseDto createPost(String title, String content) {
        return postService.writePost(createRequest(title, content));
    }

    private PostRequireDto createRequest(String title, String content) {
        return new PostRequireDto(null, MEMBER_EMAIL, title, content);
    }
}
