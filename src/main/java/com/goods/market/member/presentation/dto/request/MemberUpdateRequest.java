package com.goods.market.member.presentation.dto.request;

import com.goods.market.member.application.dto.MemberUpdateCommand;

public record MemberUpdateRequest(
        String nickname,
        String profileImage
) {
    public MemberUpdateCommand toCommand() {
        return new MemberUpdateCommand(nickname, profileImage);
    }
}
