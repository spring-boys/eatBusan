package com.ssafy.eatBusan.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "member_withdrawal_cache_cleanup")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberWithdrawalCacheCleanupTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Lob
    @Column(nullable = false)
    private String likedPostIds;

    @Lob
    @Column(nullable = false)
    private String ownedPostIds;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private MemberWithdrawalCacheCleanupTask(
        Long memberId,
        List<Long> likedPostIds,
        List<Long> ownedPostIds
    ) {
        this.memberId = memberId;
        this.likedPostIds = serialize(likedPostIds);
        this.ownedPostIds = serialize(ownedPostIds);
    }

    public static MemberWithdrawalCacheCleanupTask create(
        Long memberId,
        List<Long> likedPostIds,
        List<Long> ownedPostIds
    ) {
        return new MemberWithdrawalCacheCleanupTask(memberId, likedPostIds, ownedPostIds);
    }

    public List<Long> getLikedPostIdList() {
        return deserialize(likedPostIds);
    }

    public List<Long> getOwnedPostIdList() {
        return deserialize(ownedPostIds);
    }

    private static String serialize(List<Long> ids) {
        return ids.stream()
            .distinct()
            .map(String::valueOf)
            .collect(Collectors.joining(","));
    }

    private static List<Long> deserialize(String ids) {
        if (ids.isBlank()) {
            return List.of();
        }
        return Arrays.stream(ids.split(","))
            .map(Long::valueOf)
            .toList();
    }
}
