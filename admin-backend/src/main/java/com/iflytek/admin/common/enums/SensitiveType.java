package com.iflytek.admin.common.enums;

import java.util.function.Function;

public enum SensitiveType {
    PHONE(val -> {
        if (val == null || val.length() < 7) return val;
        return val.substring(0, 3) + "****" + val.substring(val.length() - 4);
    }),
    EMAIL(val -> {
        if (val == null || !val.contains("@")) return val;
        int atIdx = val.indexOf('@');
        if (atIdx < 1) return val;
        return val.charAt(0) + "***" + val.substring(atIdx);
    }),
    ID_CARD(val -> {
        if (val == null || val.length() < 8) return val;
        return val.substring(0, 3) + "*".repeat(val.length() - 7) + val.substring(val.length() - 4);
    }),
    BANK_CARD(val -> {
        if (val == null || val.length() < 4) return val;
        return "*".repeat(val.length() - 4) + val.substring(val.length() - 4);
    }),
    ADDRESS(val -> {
        if (val == null || val.length() <= 6) return val;
        return val.substring(0, 6) + "***";
    });

    private final Function<String, String> maskFunction;

    SensitiveType(Function<String, String> maskFunction) {
        this.maskFunction = maskFunction;
    }

    public String mask(String value) {
        return maskFunction.apply(value);
    }
}
