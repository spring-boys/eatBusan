package com.ssafy.eatBusan.postcomment.service;

import com.ssafy.eatBusan.global.exception.EBException;
import com.ssafy.eatBusan.global.exception.ErrorCode;
import com.ssafy.eatBusan.member.repository.MemberRepository;
import com.ssafy.eatBusan.post.domain.Post;
import com.ssafy.eatBusan.post.repository.PostRepository;
import com.ssafy.eatBusan.postcomment.dto.PostCommentDto;
import com.ssafy.eatBusan.postcomment.dto.PostCommentPageResponse;
import com.ssafy.eatBusan.postcomment.mapper.PostCommentMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostCommentService {

    private final PostCommentMapper postCommentMapper;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public void save(Long postId, String content, Long memberId) {
        Post post = getPost(postId);
        validateMember(memberId);
        validateContent(content);

        postCommentMapper.saveComment(memberId, postId, content);
        post.increaseCommentCount();
    }

    @Transactional
    public void delete(Long postId, Long commentId, Long memberId) {
        Post post = getPost(postId);
        validateMember(memberId);

        int deleted = postCommentMapper.deleteComment(postId, memberId, commentId);
        if (deleted == 0) {
            throw new EBException(ErrorCode.COMMENT_NOT_FOUND);
        }

        post.decreaseCommentCount();
    }

    @Transactional
    public void update(Long postId, Long commentId, String content, Long memberId) {
        getPost(postId);
        validateMember(memberId);
        validateContent(content);

        int updated = postCommentMapper.updateComment(postId, memberId, commentId, content);
        if (updated == 0) {
            throw new EBException(ErrorCode.COMMENT_NOT_FOUND);
        }
    }

    public PostCommentPageResponse findByPostId(Long postId, Long cursor, int size) {
        validatePageSize(size);
        getPost(postId);

        List<PostCommentDto> rows = postCommentMapper.findByPostId(postId, cursor, size + 1);
        boolean hasNext = rows.size() > size;
        if (hasNext) {
            rows = rows.subList(0, size);
        }
        Long nextCursor = hasNext ? rows.get(size - 1).id() : null;
        return PostCommentPageResponse.of(rows, nextCursor, hasNext);
    }

    private static void validatePageSize(int size) {
        if (size <= 0) {
            throw new EBException(ErrorCode.INVALID_PAGE_SIZE);
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new EBException(ErrorCode.COMMENT_CONTENT_EMPTY);
        }
    }

    private Post getPost(Long postId) {
        return postRepository.findByIdAndDeletedFalse(postId)
            .orElseThrow(() -> new EBException(ErrorCode.POST_NOT_FOUND));
    }

    private void validateMember(Long memberId) {
        if (memberRepository.findById(memberId).isEmpty()) {
            throw new EBException(ErrorCode.MEMBER_NOT_FOUND);
        }
    }
}
