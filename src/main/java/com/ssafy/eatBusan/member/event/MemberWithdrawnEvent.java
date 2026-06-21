package com.ssafy.eatBusan.member.event;

public record MemberWithdrawnEvent(
    Long memberId,
    Long cacheCleanupTaskId
) {}
