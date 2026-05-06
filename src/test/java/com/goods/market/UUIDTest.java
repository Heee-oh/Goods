package com.goods.market;


import com.fasterxml.uuid.Generators;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class UUIDTest {

    @Test
    void uuid() {
        UUID uuid = UUID.fromString("73f231c5-7a44-47c5-93d8-0b23e2afc601");

    }

    @Test
    void tag() {

        UUID uuid = Generators.timeBasedEpochRandomGenerator().generate();  // 또는 Generators.timeBasedEpochRandomGenerator().generate()
        long lsb = uuid.getLeastSignificantBits(); // UUID의 하위 64비트 사용
        long positive = lsb < 0 ? -lsb : lsb;      // 음수 방지
        String base36 = Long.toString(positive, 36).toUpperCase(); // Base36 변환
        String rs = base36.replace(' ', '0').substring(0, 7);//

        System.out.println(rs);

    }
}
