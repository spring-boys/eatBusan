package com.ssafy.eatBusan.member.service;

import com.ssafy.eatBusan.auth.domain.TokenType;
import com.ssafy.eatBusan.auth.dto.RefreshTokenResponseDto;
import com.ssafy.eatBusan.auth.service.RefreshTokenService;
import com.ssafy.eatBusan.auth.util.CookieUtil;
import com.ssafy.eatBusan.auth.util.JWTUtil;
import com.ssafy.eatBusan.global.exception.EBException;
import com.ssafy.eatBusan.global.exception.ErrorCode;
import com.ssafy.eatBusan.global.storage.s3.S3Service;
import com.ssafy.eatBusan.member.domain.Member;
import com.ssafy.eatBusan.member.domain.MemberWithdrawalCacheCleanupTask;
import com.ssafy.eatBusan.member.dto.LoginRequestDto;
import com.ssafy.eatBusan.member.dto.MemberDto;
import com.ssafy.eatBusan.member.dto.MemberInfoDto;
import com.ssafy.eatBusan.member.dto.MemberRequestDto;
import com.ssafy.eatBusan.member.dto.MemberResponseDto;
import com.ssafy.eatBusan.member.event.MemberWithdrawnEvent;
import com.ssafy.eatBusan.member.repository.MemberRepository;
import com.ssafy.eatBusan.member.repository.MemberWithdrawalCacheCleanupTaskRepository;
import com.ssafy.eatBusan.placelike.mapper.PlaceLikeMapper;
import com.ssafy.eatBusan.placelike.tempdomain.PlaceLike;
import com.ssafy.eatBusan.post.repository.PostRepository;
import com.ssafy.eatBusan.postcomment.mapper.PostCommentMapper;
import com.ssafy.eatBusan.postimage.mapper.PostImageMapper;
import com.ssafy.eatBusan.postimage.service.PostImageService;
import com.ssafy.eatBusan.postlike.repository.PostLikeRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

    private final JWTUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final RefreshTokenService refreshTokenService;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostCommentMapper postCommentMapper;
    private final PostImageMapper postImageMapper;
    private final PlaceLikeMapper placeLikeMapper;
    private final S3Service s3Service;
    private final ApplicationEventPublisher eventPublisher;
    private final MemberWithdrawalCacheCleanupTaskRepository cacheCleanupTaskRepository;

    @Transactional
    public MemberResponseDto join(MemberRequestDto memberRequestDto){
        memberRepository.findMemberByEmail(memberRequestDto.email())
                .ifPresent(m -> { throw new EBException(ErrorCode.MEMBER_DUPLICATE);});

        Member member = Member.builder()
                .email(memberRequestDto.email())
                .pw(passwordEncoder.encode(memberRequestDto.password()))
                .build();

        Member savedMember = memberRepository.save(member);
        return MemberResponseDto.from(savedMember);
    }

    @Transactional
    public void login(LoginRequestDto loginRequestDto, HttpServletResponse response){
        Member member = memberRepository.findMemberByEmail(loginRequestDto.email())
                .orElseThrow(() -> new EBException(ErrorCode.AUTH_INVALID_LOGIN));

        if(!passwordEncoder.matches(loginRequestDto.password(), member.getPw())){
            throw new EBException(ErrorCode.AUTH_INVALID_LOGIN);
        }
        saveAccessToken(member, response);
        saveRefreshToken(member, response);
    }

    @Transactional
    public void logout(MemberDto memberDto){
        refreshTokenService.deleteRefreshTokenByMemberId(memberDto.id());
    }

    @Transactional
    public void refreshToken(HttpServletRequest request, HttpServletResponse response){

        //refreshToken을 기반으로 id 조회
        String refreshToken = cookieUtil.getRefreshToken(request).orElseThrow(() -> new EBException(ErrorCode.RTOKEN_COOKIE_NOT_FOUND));

        //refreshToken 검증 및 id 추출
        if(!jwtUtil.validateToken(refreshToken, TokenType.REFRESH)) throw new EBException(ErrorCode.RTOKEN_INVALID);
        Long memberId = jwtUtil.getId(refreshToken, TokenType.REFRESH);

        //기존 refreshToken 삭제 및 새로운 토큰 발급
        RefreshTokenResponseDto curRefreshToken = refreshTokenService.findRefreshTokenByMemberId(memberId);
        if(!curRefreshToken.refreshToken().equals(refreshToken)) throw new EBException(ErrorCode.RTOKEN_MISMATCH);

        refreshTokenService.deleteRefreshTokenByMemberId(memberId);
        Member member = memberRepository.getReferenceById(memberId);
        saveAccessToken(member, response);
        saveRefreshToken(member, response);
    }

    private void saveAccessToken(Member member, HttpServletResponse response){
        String accessToken = jwtUtil.createToken(member, TokenType.ACCESS);
        //헤더에 accessToken을
        response.addHeader("Authorization", String.format("Bearer %s", accessToken));
    }

    private void saveRefreshToken(Member member, HttpServletResponse response){
        String refreshToken = jwtUtil.createToken(member, TokenType.REFRESH);
        //쿠키에 refreshToken을,,,
        cookieUtil.saveRefreshToken(refreshToken, response);
        refreshTokenService.saveRefreshToken(member, refreshToken);
    }

    public MemberInfoDto findMemberInfo(MemberDto memberDto) {
        Member member = memberRepository.findById(memberDto.id())
                .orElseThrow(() -> new EBException(ErrorCode.MEMBER_NOT_FOUND));
        return MemberInfoDto.from(member);
    }

    @Transactional
    public void deleteRelatedEntity(MemberDto memberDto){

        Long memberId = memberDto.id();

        // TODO: vote 관련 도메인 지우기

        // 탈퇴 도중 새로운 FK 연관 데이터가 추가되지 않도록 회원 행을 먼저 잠근다.
        memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new EBException(ErrorCode.MEMBER_NOT_FOUND));

        List<Long> postIds = postRepository.findPostsByMemberId(memberId);
        List<Long> likedPostIds = postLikeRepository.findPostIdsLikedByMemberId(memberId);
        List<Long> commentedPostIds = postCommentMapper.findActivePostIdsByMemberId(memberId);

        // postLike 지우기
        postLikeRepository.deleteByMemberId(memberId);
        // postComment 지우기
        postCommentMapper.deletePostCommentByMemberId(memberId);

        if(!postIds.isEmpty()){
            postLikeRepository.deleteByPostIds(postIds);
            postCommentMapper.deletePostCommentByPostIds(postIds);
            postImageMapper.deleteByPostIds(postIds);
        }

        // post 지우기
        postRepository.deleteByMemberId(memberId);

        Set<Long> deletedPostIds = new HashSet<>(postIds);
        List<Long> survivingCommentedPostIds = commentedPostIds.stream()
                .filter(postId -> !deletedPostIds.contains(postId))
                .distinct()
                .toList();
        if(!survivingCommentedPostIds.isEmpty()){
            postCommentMapper.recalculateCommentCountsByPostIds(survivingCommentedPostIds);
        }

        //placeLike 지우기
        placeLikeMapper.deletePlaceLikesByMemberId(memberId);

        // RefreshToken 지우기
        refreshTokenService.deleteRefreshTokenByMemberId(memberId);

        //member 지우기
        memberRepository.deleteByMemberId(memberId);

        if (!likedPostIds.isEmpty() || !postIds.isEmpty()) {
            MemberWithdrawalCacheCleanupTask cleanupTask = cacheCleanupTaskRepository.save(
                MemberWithdrawalCacheCleanupTask.create(memberId, likedPostIds, postIds)
            );
            eventPublisher.publishEvent(new MemberWithdrawnEvent(memberId, cleanupTask.getId()));
        }

        //TODO: s3에서 이미지 직접 지우기

    }

}
