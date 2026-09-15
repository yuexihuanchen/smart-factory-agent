CREATE TABLE IF NOT EXISTS alarm (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    device_id BIGINT UNSIGNED NOT NULL,
    alarm_code VARCHAR(64) NOT NULL,
    alarm_type VARCHAR(64) DEFAULT NULL,
    alarm_level VARCHAR(20) NOT NULL,
    title VARCHAR(255) DEFAULT NULL,
    message TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    first_occurred_at DATETIME NOT NULL,
    last_occurred_at DATETIME NOT NULL,
    acknowledged_at DATETIME DEFAULT NULL,
    resolved_at DATETIME DEFAULT NULL,
    acknowledged_by BIGINT UNSIGNED DEFAULT NULL,
    occurrence_count INT UNSIGNED NOT NULL DEFAULT 1,
    open_key VARCHAR(160)
        GENERATED ALWAYS AS (
            CASE
                WHEN status IN ('ACTIVE', 'ACKNOWLEDGED')
                    THEN CONCAT(device_id, ':', alarm_code)
                ELSE NULL
            END
        ) STORED,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_alarm_open (open_key),
    KEY idx_alarm_device_id (device_id),
    KEY idx_alarm_device_status_last (
        device_id,
        status,
        last_occurred_at
    ),
    CONSTRAINT chk_alarm_level
        CHECK (alarm_level IN ('INFO', 'WARNING', 'CRITICAL')),
    CONSTRAINT chk_alarm_status
        CHECK (status IN ('ACTIVE', 'ACKNOWLEDGED', 'RESOLVED')),
    CONSTRAINT chk_alarm_occurrence_count
        CHECK (occurrence_count >= 1)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '设备告警';
