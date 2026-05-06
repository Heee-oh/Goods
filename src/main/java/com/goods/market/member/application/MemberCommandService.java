package com.goods.market.member.application;

import com.goods.market.member.application.dto.MemberSignupCommand;
import com.goods.market.member.application.dto.MemberUpdateCommand;

public interface MemberCommandService {
    Long signup(MemberSignupCommand command);
    void updateNickname(Long memberId, String nickname);
    void updateMemberInfo(Long memberId, MemberUpdateCommand command);
    void updateProfileImage(Long memberId, String profileImage);
    void addInterest(Long memberId, Long listingId);
    void deleteInterest(Long memberId, Long listingId);
    void withdraw(Long memberId);
}
