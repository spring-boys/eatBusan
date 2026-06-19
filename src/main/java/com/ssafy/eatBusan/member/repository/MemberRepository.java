package com.ssafy.eatBusan.member.repository;

import com.ssafy.eatBusan.member.domain.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findMemberByEmail(String email);

    @Modifying
    @Query("delete from Member m where m.id =:memeberId")
    void deleteByMemberId(Long memberId);

}
