package com.goods.market.member.application;

import com.goods.market.member.application.dto.MemberSignupCommand;
import com.goods.market.member.application.dto.MemberUpdateCommand;
import com.goods.market.member.domain.Member;
import com.goods.market.member.domain.PhoneNumber;
import com.goods.market.member.infrastructure.Interest.InterestJpaRepository;
import com.goods.market.member.infrastructure.member.MemberJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberCommandServiceImplTest {

    @Mock
    private MemberJpaRepository memberJpaRepository;

    @Mock
    private InterestJpaRepository interestJpaRepository;

    @InjectMocks
    private MemberCommandServiceImpl memberCommandService;

    @Test
    @DisplayName("회원가입: 중복되지 않은 휴대폰 번호로 가입 시 성공하고 생성된 ID를 반환한다")
    void signupSuccess() {
        // given
        MemberSignupCommand command = new MemberSignupCommand("alice", "01012345678");
        when(memberJpaRepository.findByPhoneNumber(any(PhoneNumber.class))).thenReturn(Optional.empty());
        when(memberJpaRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        memberCommandService.signup(command);

        // then
        verify(memberJpaRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("회원가입: 이미 사용 중인 휴대폰 번호로 가입을 시도하면 예외가 발생한다")
    void signupFailsWhenPhoneExists() {
        // given
        PhoneNumber existingNumber = new PhoneNumber("01012345678");
        when(memberJpaRepository.findByPhoneNumber(existingNumber))
                .thenReturn(Optional.of(new Member("dup", existingNumber)));

        // when & then
        assertThatThrownBy(() -> memberCommandService.signup(new MemberSignupCommand("alice", "01012345678")))
                .isInstanceOf(RuntimeException.class);

        verify(memberJpaRepository, never()).save(any(Member.class));
    }

    @Test
    @DisplayName("정보 수정: 회원의 닉네임과 프로필 이미지를 변경하면 엔티티 상태가 갱신된다")
    void updateMemberInfoUpdatesNicknameAndProfileImage() {
        // given
        Member member = new Member("before", new PhoneNumber("01011112222"));
        when(memberJpaRepository.findById(member.getId())).thenReturn(Optional.of(member));

        // when
        memberCommandService.updateMemberInfo(member.getId(), new MemberUpdateCommand("after", "img.png"));

        // then
        assertThat(member.getNickname()).isEqualTo("after");
        assertThat(member.getProfileImageUrl()).isEqualTo("img.png");
    }

    @Test
    @DisplayName("관심 등록: 이미 관심 목록에 등록된 게시글이라면 추가 로직을 건너뛴다")
    void addInterestSkipsWhenAlreadyExists() {
        // given
        when(interestJpaRepository.existsByListingIdAndMemberId(10L, 1L)).thenReturn(true);

        // when
        memberCommandService.addInterest(1L, 10L);

        // then: 추가 조회를 하지 않으므로 memberRepository 호출이 없어야 함
        verify(memberJpaRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("관심 등록: 관심 목록에 없는 게시글은 새롭게 추가한다")
    void addInterestAddsWhenNotExists() {
        // given
        Member member = new Member("user", new PhoneNumber("01033334444"));
        when(interestJpaRepository.existsByListingIdAndMemberId(10L, member.getId())).thenReturn(false);
        when(memberJpaRepository.findById(member.getId())).thenReturn(Optional.of(member));

        // when
        memberCommandService.addInterest(member.getId(), 10L);

        // then
        assertThat(member.getInterests()).hasSize(1);
        assertThat(member.getInterests().get(0).getListingId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("회원 탈퇴: 존재하지 않는 회원 ID로 탈퇴를 시도하면 예외가 발생한다")
    void withdrawFailsWhenMemberNotFound() {
        // given
        when(memberJpaRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberCommandService.withdraw(1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("관심 해제: 게시글 ID와 회원 ID를 전달하여 관심 목록에서 삭제한다")
    void deleteInterestCallsRepository() {
        // given
        when(interestJpaRepository.deleteByListingIdAndMemberId(10L, 1L)).thenReturn(1);

        // when
        memberCommandService.deleteInterest(1L, 10L);

        // then: 정확한 인자값이 전달되었는지 캡처하여 확인
        ArgumentCaptor<Long> listingCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> memberCaptor = ArgumentCaptor.forClass(Long.class);

        verify(interestJpaRepository).deleteByListingIdAndMemberId(listingCaptor.capture(), memberCaptor.capture());
        assertThat(listingCaptor.getValue()).isEqualTo(10L);
        assertThat(memberCaptor.getValue()).isEqualTo(1L);
    }
}
