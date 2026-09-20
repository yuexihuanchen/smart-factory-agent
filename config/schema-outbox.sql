CREATE TABLE IF NOT EXISTS outbox_event (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    source VARCHAR(64)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL,
    event_id VARCHAR(128)
        CHARACTER SET ascii
        COLLATE ascii_bin
        NOT NULL,
    exchange VARCHAR(255) NOT NULL,
    routing_key VARCHAR(255) NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    published_at DATETIME(3) DEFAULT NULL,
    last_error TEXT DEFAULT NULL,
    lease_owner VARCHAR(64) DEFAULT NULL,
    lease_until DATETIME(3) DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_event_source_event (
        source,
        event_id
    ),
    KEY idx_outbox_event_status_created (
        status,
        created_at
    ),
    KEY idx_outbox_event_status_lease (
        status,
        lease_until
    ),
    CONSTRAINT chk_outbox_event_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'SENT'))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = 'RabbitMQ 可靠发布 Outbox';
