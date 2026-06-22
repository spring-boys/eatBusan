package com.ssafy.eatBusan.voteroom.controller;

import com.ssafy.eatBusan.auth.resolver.LoginMember;
import com.ssafy.eatBusan.global.exception.EBException;
import com.ssafy.eatBusan.global.exception.ErrorCode;
import com.ssafy.eatBusan.member.dto.MemberDto;
import com.ssafy.eatBusan.voteroom.dto.JoinRequest;
import com.ssafy.eatBusan.voteroom.dto.VoteRequest;
import com.ssafy.eatBusan.voteroom.dto.VoteResponse;
import com.ssafy.eatBusan.voteroom.dto.VoteRoomCreateRequest;
import com.ssafy.eatBusan.voteroom.dto.VoteRoomCreateResponse;
import com.ssafy.eatBusan.voteroom.dto.VoteRoomDetailResponse;
import com.ssafy.eatBusan.voteroom.dto.VoteRoomResultResponse;
import com.ssafy.eatBusan.voteroom.service.VoteRoomService;
import com.ssafy.eatBusan.voteroom.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vote-rooms")
@RequiredArgsConstructor
public class VoteRoomController {

    private final VoteRoomService voteRoomService;
    private final VoteService voteService;

    // 방 생성: 호스트 위치 기반 후보 시드 + 초대 코드 발급 (호스트만 JOINED)
    @PostMapping
    public ResponseEntity<VoteRoomCreateResponse> create(
        @RequestBody VoteRoomCreateRequest request,
        @LoginMember MemberDto loginMember
    ) {
        VoteRoomCreateResponse response = voteRoomService.create(requireMemberId(loginMember), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 코드 입장: 초대 코드로 OPEN 방에 참가(JOINED) — 이미 참가자면 멱등. 상세 응답 반환.
    @PostMapping("/join")
    public ResponseEntity<VoteRoomDetailResponse> join(
        @RequestBody JoinRequest request,
        @LoginMember MemberDto loginMember
    ) {
        return ResponseEntity.ok(voteRoomService.join(request.code(), requireMemberId(loginMember)));
    }

    // 방 상세: 후보·참가자·내 표·상태 (참가자만)
    @GetMapping("/{publicId}")
    public ResponseEntity<VoteRoomDetailResponse> getDetail(
        @PathVariable String publicId,
        @LoginMember MemberDto loginMember
    ) {
        return ResponseEntity.ok(voteRoomService.getDetail(publicId, requireMemberId(loginMember)));
    }

    // 현재 집계 스냅샷: 폴링/재연결 직후 초기화용 (참가자만)
    @GetMapping("/{publicId}/result")
    public ResponseEntity<VoteRoomResultResponse> getResult(
        @PathVariable String publicId,
        @LoginMember MemberDto loginMember
    ) {
        return ResponseEntity.ok(voteRoomService.getResult(publicId, requireMemberId(loginMember)));
    }

    // 투표/표 변경: 순위 ballot 제출. 같은 ballot 재제출은 멱등이므로 200으로 일괄 처리
    @PostMapping("/{publicId}/votes")
    public ResponseEntity<VoteResponse> vote(
        @PathVariable String publicId,
        @RequestBody VoteRequest request,
        @LoginMember MemberDto loginMember
    ) {
        return ResponseEntity.ok(voteService.cast(publicId, requireMemberId(loginMember), request.candidateIds()));
    }

    // 마감: 호스트만, 멱등 — 이미 CLOSED면 기존 winner 그대로 200
    @PostMapping("/{publicId}/close")
    public ResponseEntity<VoteRoomResultResponse> close(
        @PathVariable String publicId,
        @LoginMember MemberDto loginMember
    ) {
        return ResponseEntity.ok(voteRoomService.close(publicId, requireMemberId(loginMember)));
    }

    // 투표는 로그인 회원만 가능: @LoginMember가 null(미인증)이면 401(TOKEN_NOT_FOUND)로 응답해
    // 프론트가 로그인 화면으로 보내도록 한다. (전역 resolver는 place 등 낙관적 인증을 위해 null 허용)
    private Long requireMemberId(MemberDto loginMember) {
        if (loginMember == null) {
            throw new EBException(ErrorCode.TOKEN_NOT_FOUND);
        }
        return loginMember.id();
    }
}
