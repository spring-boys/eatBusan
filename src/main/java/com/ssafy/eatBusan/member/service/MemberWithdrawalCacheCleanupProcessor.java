package com.ssafy.eatBusan.member.service;

import com.ssafy.eatBusan.member.domain.MemberWithdrawalCacheCleanupTask;
import com.ssafy.eatBusan.member.repository.MemberWithdrawalCacheCleanupTaskRepository;
import com.ssafy.eatBusan.postlike.service.PostLikeCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberWithdrawalCacheCleanupProcessor {

    private final MemberWithdrawalCacheCleanupTaskRepository taskRepository;
    private final PostLikeCacheService postLikeCacheService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long taskId) {
        MemberWithdrawalCacheCleanupTask task = taskRepository.findByIdForUpdate(taskId)
            .orElse(null);
        if (task == null) {
            return;
        }

        postLikeCacheService.invalidateAfterMemberWithdrawal(
            task.getLikedPostIdList(),
            task.getOwnedPostIdList()
        );
        taskRepository.delete(task);
    }
}
