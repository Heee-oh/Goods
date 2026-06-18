package com.goods.market.member.infrastructure.Interest;

import com.goods.market.member.domain.Interest;
import com.goods.market.member.domain.InterestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaInterestRepositoryAdapter implements InterestRepository {

    private final InterestJpaRepository interestJpaRepository;

    @Override
    public Interest save(Interest interest) {
        return interestJpaRepository.save(interest);
    }

    @Override
    public Optional<Interest> findById(Long id) {
        return interestJpaRepository.findById(id);
    }

    @Override
    public boolean existsByListingIdAndMemberId(Long listingId, Long memberId) {
        return interestJpaRepository.existsByListingIdAndMemberId(listingId, memberId);
    }

    @Override
    public int deleteByListingIdAndMemberId(Long listingId, Long memberId) {
        return interestJpaRepository.deleteByListingIdAndMemberId(listingId, memberId);
    }
}
