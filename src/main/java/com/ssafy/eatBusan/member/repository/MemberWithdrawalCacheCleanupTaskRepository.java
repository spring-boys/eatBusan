package com.ssafy.eatBusan.member.repository;

import com.ssafy.eatBusan.member.domain.MemberWithdrawalCacheCleanupTask;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberWithdrawalCacheCleanupTaskRepository
    extends JpaRepository<MemberWithdrawalCacheCleanupTask, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select task from MemberWithdrawalCacheCleanupTask task where task.id = :taskId")
    Optional<MemberWithdrawalCacheCleanupTask> findByIdForUpdate(@Param("taskId") Long taskId);

    @Query("select task.id from MemberWithdrawalCacheCleanupTask task order by task.id")
    List<Long> findPendingTaskIds(Pageable pageable);
}
