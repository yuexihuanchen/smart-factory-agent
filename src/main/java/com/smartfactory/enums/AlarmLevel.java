package com.smartfactory.enums;

/**
 * 告警等级。
 */
public enum AlarmLevel {

    INFO,
    WARNING,
    CRITICAL;

    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        for (AlarmLevel level : values()) {
            if (level.name().equals(value)) {
                return true;
            }
        }

        return false;
    }
}
