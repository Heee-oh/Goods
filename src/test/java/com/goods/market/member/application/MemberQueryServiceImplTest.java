package com.goods.market.member.application;

import com.goods.market.member.application.dto.InterestResponse;
import com.goods.market.member.application.dto.MemberRegionResponse;
import com.goods.market.member.application.dto.MemberResponse;
import com.goods.market.member.infrastructure.Interest.InterestJpaRepository;
import com.goods.market.member.infrastructure.member.MemberJpaRepository;
import com.goods.market.member.infrastructure.memberRegion.MemberRegionJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberQueryServiceImplTest {

    @Mock
    private MemberJpaRepository memberRepository;

    @Mock
    private MemberRegionJpaRepository memberRegionRepository;

    @Mock
    private InterestJpaRepository interestRepository;

    @InjectMocks
    private MemberQueryServiceImpl memberQueryService;
    @Test
    @DisplayName("내 정보 조회 시 레포지토리에서 데이터를 가져와 정상적으로 반환한다")
    void getMeDelegatesToRepository() {
        // given
        Long memberId = 1L;
        MemberResponse expected = new MemberResponse("nick", "img.png", 365);
        when(memberRepository.findMember(memberId)).thenReturn(expected);

        // when
        MemberResponse actual = memberQueryService.getMe(memberId);

        // then
        assertThat(actual).isEqualTo(expected);
        verify(memberRepository).findMember(memberId);
    }

    @Test
    @DisplayName("사용자의 지역 목록 조회 시 레포지토리를 호출하여 결과를 반환한다")
    void getMyRegionsDelegatesToRepository() {
        // given
        Long memberId = 1L;
        List<MemberRegionResponse> expected = List.of(
                new MemberRegionResponse(1L, 11000, Instant.now(), true, "동네이름", null, null)
        );
        when(memberRegionRepository.findAllByMember(memberId)).thenReturn(expected);

        // when
        List<MemberRegionResponse> actual = memberQueryService.getMyRegions(memberId);

        // then
        assertThat(actual).hasSize(1);
        assertThat(actual).isEqualTo(expected);
        verify(memberRegionRepository).findAllByMember(memberId);
    }

    @Test
    @DisplayName("관심 목록 조회 시 커서 ID와 페이지 크기가 레포지토리에 올바르게 전달된다")
    void getMyInterestsPassesCursorAndPageSize() {
        // given
        Long memberId = 1L;
        Long lastInterestId = 50L;
        int size = 7;
        Slice<InterestResponse> expected = new SliceImpl<>(List.of(new InterestResponse(10L, 99L)));

        when(interestRepository.findAllByMemberId(eq(memberId), eq(lastInterestId), any(Pageable.class)))
                .thenReturn(expected);

        // when
        Slice<InterestResponse> actual = memberQueryService.getMyInterests(memberId, lastInterestId, size);

        // then
        assertThat(actual).isEqualTo(expected);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(interestRepository).findAllByMemberId(eq(memberId), eq(lastInterestId), pageableCaptor.capture());

        // No-Offset 페이징의 핵심인 PageSize가 잘 전달되었는지 확인
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(size);
    }
}
