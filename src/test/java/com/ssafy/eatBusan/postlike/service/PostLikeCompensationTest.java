package com.ssafy.eatBusan.postlike.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.eatBusan.member.domain.Member;
import com.ssafy.eatBusan.member.repository.MemberRepository;
import com.ssafy.eatBusan.post.domain.Post;
import com.ssafy.eatBusan.post.repository.PostRepository;
import com.ssafy.eatBusan.postlike.repository.PostLikeRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class PostLikeCompensationTest {

    @Test
    void DB_동기화_실패_시_Redis_토글을_보상한다() {
        PostLikeCacheService cacheService = mock(PostLikeCacheService.class);
        PostLikeRepository postLikeRepository = mock(PostLikeRepository.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        PostRepository postRepository = mock(PostRepository.class);
        Post post = mock(Post.class);
        Member member = mock(Member.class);
        PostLikeService service = new PostLikeService(
            cacheService, postLikeRepository, memberRepository, postRepository);

        when(post.getId()).thenReturn(1L);
        when(member.getId()).thenReturn(2L);
        when(postRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(post));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(member));
        when(cacheService.toggle(1L, 2L)).thenReturn(new long[]{1L, 1L});
        when(postLikeRepository.findIncludingDeleted(1L, 2L)).thenReturn(Optional.empty());
        doThrow(new DataIntegrityViolationException("DB sync failed"))
            .when(postLikeRepository).flush();

        assertThatThrownBy(() -> service.like(1L, 2L))
            .isInstanceOf(DataIntegrityViolationException.class);

        verify(cacheService).compensate(1L, 2L, true);
    }
}
