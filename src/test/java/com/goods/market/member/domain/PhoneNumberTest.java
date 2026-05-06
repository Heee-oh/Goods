package com.goods.market.member.domain;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class PhoneNumberTest {

    @Test
    void phoneNumberTest() {

        Assertions.assertThatThrownBy(() -> new PhoneNumber(null))
                .isInstanceOf(IllegalArgumentException.class);

        Assertions.assertThatThrownBy(() -> new PhoneNumber("dd"))
                .isInstanceOf(IllegalArgumentException.class);

        Assertions.assertThatThrownBy(() -> new PhoneNumber("123456789101112"))
                .as("11자 초과 전화번호")
                .isInstanceOf(IllegalArgumentException.class);



    }

}