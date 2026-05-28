package com.goods.market.member.infrastructure.Interest;

import com.goods.market.member.application.dto.InterestDto;
import com.goods.market.member.domain.Interest;
import com.goods.market.member.domain.Member;
import com.goods.market.member.domain.PhoneNumber;
import com.goods.market.member.infrastructure.member.MemberJpaRepository;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@Transactional
class InterestRepositoryCustomImplTest {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private InterestJpaRepository interestRepository;

    @Test
    void findAllByMemberIdSupportsCursorPaging() {
        log.info("멤버 저장");
        Member member = memberJpaRepository.save(new Member("m1", new PhoneNumber("01020000001")));
        Member other = memberJpaRepository.save(new Member("m2", new PhoneNumber("01020000002")));

        log.info("관심목록 저장");
        member.addInterest(new Interest(10L));
        member.addInterest(new Interest(20L));
        member.addInterest(new Interest(30L));
        other.addInterest(new Interest(999L));

        memberJpaRepository.save(member);
        memberJpaRepository.save(other);

        //member의 최신 순 2개만 조회
        Slice<InterestDto> firstPage = interestRepository.findAllByMemberId(
                member.getId(),
                Long.MAX_VALUE,
                PageRequest.of(0, 2)
        );

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.getContent().stream().map(InterestDto::listingId))
                .doesNotContain(999L); // other꺼 들어갔는지 체크


        // 마지막 꺼낸 페이지 행 관심목록 페이지 listing id = 20L 체크
        Long cursor = firstPage.getContent().getLast().id();
        InterestDto interestResponse = firstPage.getContent().getLast();
        Assertions.assertThat(interestResponse.listingId()).isEqualTo(20L);


        // 커서를 통해 페이지 정상작동 확인
        Slice<InterestDto> secondPage = interestRepository.findAllByMemberId(
                member.getId(),
                cursor,
                PageRequest.of(0, 2)
        );

        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(secondPage.getContent().stream().map(InterestDto::id)) // 조회한 페이지 내 id값들이 항상 cursor보다 작은지
                .allMatch(id -> id < cursor);

        // 2페이지에는 남은 30L만 있어야 함
        assertThat(secondPage.getContent())
                .extracting(InterestDto::listingId)
                .containsExactly(10L); // 순서와 내용이 정확히 일치하는지 확인

    }
}
