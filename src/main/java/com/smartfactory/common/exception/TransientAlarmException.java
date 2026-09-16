package com.smartfactory.common.exception;

/**
 * 表示告警事件处理时遇到的临时故障，可以安全重试。
 */
public class TransientAlarmException extends RuntimeException {

    public TransientAlarmException(String message) {
        super(message);
    }

    public TransientAlarmException(
            String message,
            Throwable cause) {
        super(message, cause);
    }
}
