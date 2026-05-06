package com.goods.market.member.infrastructure.member;

import com.goods.market.member.domain.Member;
import com.goods.market.member.domain.PhoneNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@Transactional
public interface MemberJpaRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
    @Query("SELECT m FROM Member m WHERE m.phoneNumber = :phoneNumber AND m.status = 'ACTIVE'")
    Optional<Member> findByPhoneNumber(PhoneNumber phoneNumber);
}
