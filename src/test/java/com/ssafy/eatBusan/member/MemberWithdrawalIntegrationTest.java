package com.ssafy.eatBusan.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ssafy.eatBusan.auth.domain.RefreshToken;
import com.ssafy.eatBusan.auth.domain.TokenType;
import com.ssafy.eatBusan.auth.repository.RefreshTokenRepository;
import com.ssafy.eatBusan.auth.util.JWTUtil;
import com.ssafy.eatBusan.member.domain.Member;
import com.ssafy.eatBusan.member.repository.MemberRepository;
import com.ssafy.eatBusan.member.repository.MemberWithdrawalCacheCleanupTaskRepository;
import com.ssafy.eatBusan.place.Repository.PlaceRepository;
import com.ssafy.eatBusan.place.domain.Place;
import com.ssafy.eatBusan.placelike.dto.PlaceLikeRequestDto;
import com.ssafy.eatBusan.placelike.mapper.PlaceLikeMapper;
import com.ssafy.eatBusan.post.domain.Post;
import com.ssafy.eatBusan.post.repository.PostRepository;
import com.ssafy.eatBusan.postcomment.dto.PostCommentDto;
import com.ssafy.eatBusan.postcomment.mapper.PostCommentMapper;
import com.ssafy.eatBusan.postcomment.service.PostCommentService;
import com.ssafy.eatBusan.postimage.mapper.PostImageMapper;
import com.ssafy.eatBusan.postlike.domain.PostLike;
import com.ssafy.eatBusan.postlike.repository.PostLikeRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
    "member.withdrawal.cache-cleanup.initial-delay-ms=3600000",
    "member.withdrawal.cache-cleanup.retry-delay-ms=3600000"
})
@AutoConfigureMockMvc
class MemberWithdrawalIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JWTUtil jwtUtil;
    @Autowired MemberRepository memberRepository;
    @Autowired PlaceRepository placeRepository;
    @Autowired PostRepository postRepository;
    @Autowired PostLikeRepository postLikeRepository;
    @Autowired PostCommentService postCommentService;
    @Autowired PostCommentMapper postCommentMapper;
    @Autowired PostImageMapper postImageMapper;
    @Autowired PlaceLikeMapper placeLikeMapper;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired MemberWithdrawalCacheCleanupTaskRepository cacheCleanupTaskRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired DefaultRedisScript<Long> postLikeInvalidateScript;

    @MockitoBean StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUpRedis() {
        when(redisTemplate.execute(
            eq(postLikeInvalidateScript),
            any(),
            any(Object[].class)
        )).thenReturn(1L);
    }

    @Test
    void 회원_탈퇴를_위한_통합_테스트() throws Exception {
        Member withdrawing = saveMember("withdraw-integration@example.com");
        Member survivor = saveMember("survivor-integration@example.com");
        Member otherMember = saveMember("other-integration@example.com");
        Place place = savePlace();

        Post ownedPost = savePost(withdrawing, place, "owned-post");
        Post deletedOwnedPost = savePost(withdrawing, place, "deleted-owned-post");
        Post survivingPost = savePost(survivor, place, "surviving-post");
        Post softLikePost = savePost(survivor, place, "soft-like-post");

        postLikeRepository.saveAndFlush(PostLike.of(survivingPost, withdrawing));
        postLikeRepository.saveAndFlush(PostLike.of(ownedPost, survivor));
        postLikeRepository.saveAndFlush(PostLike.of(deletedOwnedPost, otherMember));
        PostLike softDeletedLike = PostLike.of(softLikePost, withdrawing);
        softDeletedLike.delete();
        postLikeRepository.saveAndFlush(softDeletedLike);

        postCommentService.save(
            survivingPost.getId(), "active withdrawing comment", withdrawing.getId());
        postCommentService.save(
            survivingPost.getId(), "remaining comment", otherMember.getId());
        postCommentService.save(
            survivingPost.getId(), "soft-deleted withdrawing comment", withdrawing.getId());
        List<PostCommentDto> comments = postCommentMapper.findByPostId(
            survivingPost.getId(), null, 10);
        postCommentService.delete(
            survivingPost.getId(), comments.getFirst().id(), withdrawing.getId());
        postCommentService.save(
            ownedPost.getId(), "comment on owned post", survivor.getId());
        postCommentService.save(
            deletedOwnedPost.getId(), "comment on deleted owned post", otherMember.getId());

        postImageMapper.saveImage(ownedPost.getId(), "image-url-1", "image-key-1", 0);
        postImageMapper.saveImage(
            deletedOwnedPost.getId(), "image-url-2", "image-key-2", 0);
        deletedOwnedPost.delete();
        postRepository.saveAndFlush(deletedOwnedPost);
        placeLikeMapper.insertPlaceLike(
            new PlaceLikeRequestDto(withdrawing.getId(), place.getId()));
        refreshTokenRepository.saveAndFlush(
            new RefreshToken(withdrawing, "refresh-token"));

        assertThat(postRepository.findById(survivingPost.getId())
            .orElseThrow().getCommentCount()).isEqualTo(2);
        assertThat(postLikeRepository.countByPostIdAndDeletedFalse(survivingPost.getId()))
            .isOne();

        String accessToken = "Bearer " + jwtUtil.createToken(withdrawing, TokenType.ACCESS);
        MvcResult result = mockMvc.perform(delete("/api/members")
                .header("Authorization", accessToken))
            .andExpect(status().isNoContent())
            .andReturn();

        assertThat(result.getResponse().getHeader("Set-Cookie"))
            .contains("EBRefreshToken=")
            .contains("Path=/")
            .contains("Max-Age=0")
            .contains("HttpOnly");

        assertCount("member", "id", withdrawing.getId(), 0);
        assertCount("post", "member_id", withdrawing.getId(), 0);
        assertCount("post_like", "member_id", withdrawing.getId(), 0);
        assertCount("post_like", "post_id", ownedPost.getId(), 0);
        assertCount("post_like", "post_id", deletedOwnedPost.getId(), 0);
        assertCount("post_comment", "member_id", withdrawing.getId(), 0);
        assertCount("post_comment", "post_id", ownedPost.getId(), 0);
        assertCount("post_comment", "post_id", deletedOwnedPost.getId(), 0);
        assertCount("post_image", "post_id", ownedPost.getId(), 0);
        assertCount("post_image", "post_id", deletedOwnedPost.getId(), 0);
        assertCount("place_like", "member_id", withdrawing.getId(), 0);
        assertCount("refresh_token", "member_id", withdrawing.getId(), 0);

        assertThat(memberRepository.existsById(survivor.getId())).isTrue();
        assertThat(memberRepository.existsById(otherMember.getId())).isTrue();
        assertThat(placeRepository.existsById(place.getId())).isTrue();
        assertThat(postRepository.existsById(survivingPost.getId())).isTrue();
        assertThat(postRepository.existsById(softLikePost.getId())).isTrue();
        assertThat(postRepository.findById(survivingPost.getId())
            .orElseThrow().getCommentCount()).isOne();
        assertThat(postLikeRepository.countByPostIdAndDeletedFalse(survivingPost.getId()))
            .isZero();
        assertThat(postCommentMapper.findByPostId(survivingPost.getId(), null, 10))
            .hasSize(1);

        assertThat(cacheCleanupTaskRepository.count()).isZero();
        verifyCacheInvalidated(survivingPost.getId());
        verifyCacheInvalidated(ownedPost.getId());
        verifyCacheInvalidated(deletedOwnedPost.getId());
    }

    private Member saveMember(String email) {
        return memberRepository.saveAndFlush(Member.builder()
            .email(email)
            .pw("encoded-password")
            .build());
    }

    private Place savePlace() {
        return placeRepository.saveAndFlush(Place.builder()
            .code("withdraw-integration-place")
            .name("place")
            .address("address")
            .areaCode("area")
            .phone("phone")
            .url("url")
            .x(1)
            .y(1)
            .build());
    }

    private Post savePost(Member member, Place place, String title) {
        return postRepository.saveAndFlush(Post.builder()
            .member(member)
            .place(place)
            .title(title)
            .content("content")
            .build());
    }

    private void assertCount(String table, String column, Long id, int expected) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?",
            Integer.class,
            id
        );
        assertThat(count).isEqualTo(expected);
    }

    private void verifyCacheInvalidated(Long postId) {
        verify(redisTemplate).execute(
            eq(postLikeInvalidateScript),
            eq(List.of(
                "post:likes:" + postId,
                "post:likes:" + postId + ":init",
                "post:likes:" + postId + ":lock"
            )),
            any(Object[].class)
        );
    }
}
