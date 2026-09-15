package com.smartfactory.enums;

/**
 * 告警生命周期状态。
 */
public enum AlarmStatus {

    ACTIVE,
    ACKNOWLEDGED,
    RESOLVED;

    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        for (AlarmStatus status : values()) {
            if (status.name().equals(value)) {
                return true;
            }
        }

        return false;
    }
}
