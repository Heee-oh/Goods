package com.goods.market.member.domain.exception;

public class MemberNotFoundException extends MemberDomainException{


    public MemberNotFoundException(String message) {
        super(message);
    }

    public MemberNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public MemberNotFoundException() {
        super("Member Not Found");
    }
}
