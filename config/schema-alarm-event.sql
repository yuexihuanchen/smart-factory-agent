CREATE TABLE IF NOT EXISTS alarm_event (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    source VARCHAR(64)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL,
    event_id VARCHAR(128)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL,
    device_id BIGINT UNSIGNED NOT NULL,
    alarm_code VARCHAR(64) NOT NULL,
    occurred_at DATETIME(3) NOT NULL,
    alarm_id BIGINT UNSIGNED DEFAULT NULL,
    payload JSON NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_alarm_event_source_event (
        source,
        event_id
    ),
    KEY idx_alarm_event_alarm_id (alarm_id),
    KEY idx_alarm_event_device_time (
        device_id,
        occurred_at
    )
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '设备告警原始事件';
