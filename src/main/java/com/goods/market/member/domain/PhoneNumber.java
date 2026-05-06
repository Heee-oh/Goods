package com.goods.market.member.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneNumber {
    private static final Pattern PATTERN = Pattern.compile("^\\d{10,11}$");

    @Column(name = "phone_number", length = 20, nullable = false, unique = true)
    private String value;


    public PhoneNumber(String value) {
        if (value == null || !PATTERN.matcher(value).matches() ) {
            throw new IllegalArgumentException("전화번호 형식이 맞지 않음");
        }

        this.value = value;
    }

}
