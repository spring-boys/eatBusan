package com.ssafy.eatBusan.postcomment.service;

import com.ssafy.eatBusan.global.exception.EBException;
import com.ssafy.eatBusan.global.exception.ErrorCode;
import com.ssafy.eatBusan.member.dto.MemberDto;
import com.ssafy.eatBusan.member.repository.MemberRepository;
import com.ssafy.eatBusan.post.domain.Post;
import com.ssafy.eatBusan.post.repository.PostRepository;
import com.ssafy.eatBusan.postcomment.dto.MyCommentDto;
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
        getPost(postId);
        validateContent(content);

        postCommentMapper.saveComment(memberId, postId, content);
        postCommentMapper.increaseCommentCount(postId);
    }

    @Transactional
    public void delete(Long postId, Long commentId, Long memberId) {
        int deleted = postCommentMapper.deleteComment(postId, memberId, commentId);
        if (deleted == 0) {
            throw new EBException(ErrorCode.COMMENT_NOT_FOUND);
        }

        postCommentMapper.decreaseCommentCount(postId);
    }

    @Transactional
    public void update(Long postId, Long commentId, String content, Long memberId) {
        getPost(postId);
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

    // TODO: JWT로 이미 검증된 멤버 재검증 메서드 삭제 여부 결정할 것
    @SuppressWarnings("unused")
    private void validateMember(Long memberId) {
        if (memberRepository.findById(memberId).isEmpty()) {
            throw new EBException(ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    public List<MyCommentDto> findMyComments(MemberDto memberDto) {
        return postCommentMapper.findByMemberId(memberDto.id());
    }
}
