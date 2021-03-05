package com.chuangdun.flutter.newland.utils;

import java.math.BigDecimal;

public class BigDecimalUtils {

    public static BigDecimal scale2RoundHalfUp(BigDecimal value) {
        return value.setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}
