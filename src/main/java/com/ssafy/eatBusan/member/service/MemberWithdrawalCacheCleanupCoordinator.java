package com.ssafy.eatBusan.member.service;

import com.ssafy.eatBusan.member.event.MemberWithdrawnEvent;
import com.ssafy.eatBusan.member.repository.MemberWithdrawalCacheCleanupTaskRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemberWithdrawalCacheCleanupCoordinator {

    private static final int RETRY_BATCH_SIZE = 100;

    private final MemberWithdrawalCacheCleanupTaskRepository taskRepository;
    private final MemberWithdrawalCacheCleanupProcessor processor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void processAfterCommit(MemberWithdrawnEvent event) {
        processSafely(event.cacheCleanupTaskId(), event.memberId());
    }

    @Scheduled(
        fixedDelayString = "${member.withdrawal.cache-cleanup.retry-delay-ms:5000}",
        initialDelayString = "${member.withdrawal.cache-cleanup.initial-delay-ms:5000}"
    )
    public void retryPendingTasks() {
        List<Long> taskIds = taskRepository.findPendingTaskIds(
            PageRequest.of(0, RETRY_BATCH_SIZE)
        );
        taskIds.forEach(taskId -> processSafely(taskId, null));
    }

    private void processSafely(Long taskId, Long memberId) {
        try {
            processor.process(taskId);
        } catch (RuntimeException e) {
            log.warn(
                "Member withdrawal cache cleanup deferred. taskId={} memberId={}",
                taskId,
                memberId,
                e
            );
        }
    }
}
