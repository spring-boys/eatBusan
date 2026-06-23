package com.ssafy.eatBusan.voteroom.dto;

// STOMP push 페이로드 — 참가자 입장으로 총원(분모)이 바뀜.
// 집계(version)와 무관한 별도 카운터라 version은 싣지 않는다. 클라이언트는 participantCount를 그대로 반영한다.
public record ParticipantsUpdatedMessage(String type, long participantCount) {

    public static ParticipantsUpdatedMessage of(long participantCount) {
        return new ParticipantsUpdatedMessage("PARTICIPANTS_UPDATED", participantCount);
    }
}
