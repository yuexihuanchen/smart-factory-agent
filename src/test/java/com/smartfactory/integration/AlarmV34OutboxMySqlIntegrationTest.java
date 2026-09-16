package com.smartfactory.integration;

import com.mysql.cj.jdbc.MysqlDataSource;
import com.smartfactory.common.exception.BusinessException;
import com.smartfactory.config.RabbitMQConfig;
import com.smartfactory.entity.OutboxEvent;
import com.smartfactory.enums.AlarmEventStatus;
import com.smartfactory.mapper.AlarmEventMapper;
import com.smartfactory.mapper.AlarmMapper;
import com.smartfactory.mapper.DeviceMapper;
import com.smartfactory.mapper.OutboxEventMapper;
import com.smartfactory.mapper.SysUserMapper;
import com.smartfactory.mq.DeviceAlarmEventMessage;
import com.smartfactory.service.AlarmService;
import com.smartfactory.service.command.AlarmEventCommand;
import com.smartfactory.service.impl.AlarmServiceImpl;
import com.smartfactory.vo.AlarmProcessResponse;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import tools.jackson.databind.json.JsonMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(
        AlarmV34OutboxMySqlIntegrationTest.TestConfiguration.class
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_IT", matches = "true")
class AlarmV34OutboxMySqlIntegrationTest {

    private static final String TEST_DATABASE =
            "smart_factory_alarm_it_v3";

    private static final String USERNAME =
            System.getProperty("mysql.it.username", "root");

    private static final String PASSWORD =
            System.getProperty("mysql.it.password", "root");

    private static final String SERVER_URL =
            "jdbc:mysql://localhost:3306/"
                    + "?useUnicode=true"
                    + "&characterEncoding=utf8"
                    + "&serverTimezone=Asia/Shanghai"
                    + "&useSSL=false"
                    + "&allowPublicKeyRetrieval=true";

    private static final String TEST_URL =
            "jdbc:mysql://localhost:3306/" + TEST_DATABASE
                    + "?useUnicode=true"
                    + "&characterEncoding=utf8"
                    + "&serverTimezone=Asia/Shanghai"
                    + "&useSSL=false"
                    + "&allowPublicKeyRetrieval=true";

    private static final Long DEVICE_ID = 900001L;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private OutboxEventMapper outboxEventMapper;

    @Autowired
    private JsonMapper jsonMapper;

    @BeforeEach
    void setUpDatabase() throws Exception {

        createTestDatabase();

        jdbcTemplate.execute("DROP TABLE IF EXISTS outbox_event");
        jdbcTemplate.execute("DROP TABLE IF EXISTS alarm_event");
        jdbcTemplate.execute("DROP TABLE IF EXISTS alarm");
        jdbcTemplate.execute("DROP TABLE IF EXISTS device");

        executeSqlFile("config/schema-alarm.sql");
        executeSqlFile("config/schema-alarm-event.sql");
        executeSqlFile("config/schema-outbox.sql");

        jdbcTemplate.update(
                """
                CREATE TABLE device (
                    id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
                    device_code VARCHAR(64) NOT NULL,
                    device_name VARCHAR(128) NOT NULL,
                    device_type VARCHAR(64) DEFAULT NULL,
                    location VARCHAR(255) DEFAULT NULL,
                    ip_address VARCHAR(64) DEFAULT NULL,
                    port INT DEFAULT NULL,
                    protocol VARCHAR(64) DEFAULT NULL,
                    status VARCHAR(32) DEFAULT NULL,
                    description TEXT,
                    created_at DATETIME DEFAULT NULL,
                    updated_at DATETIME DEFAULT NULL
                ) ENGINE = InnoDB
                  DEFAULT CHARSET = utf8mb4
                """
        );

        jdbcTemplate.update(
                """
                INSERT INTO device (
                    id,
                    device_code,
                    device_name,
                    device_type,
                    status
                )
                VALUES (?, ?, ?, ?, ?)
                """,
                DEVICE_ID,
                "IT-V34-DEVICE-01",
                "Alarm V3.4 Outbox Device",
                "PLC",
                "RUNNING"
        );
    }

    @AfterAll
    void dropTestDatabase() throws Exception {

        try (Connection connection =
                     DriverManager.getConnection(
                             SERVER_URL,
                             USERNAME,
                             PASSWORD
                     );
             Statement statement = connection.createStatement()) {

            statement.execute(
                    "DROP DATABASE IF EXISTS " + TEST_DATABASE
            );
        }
    }

    @Test
    void newEventCreatesPendingOutboxEventInSameTransaction() {

        AlarmEventCommand request = request(
                "gateway-A",
                "EVT-V34-NORMAL-001",
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );

        AlarmProcessResponse response =
                alarmService.processEvent(request);

        assertThat(response.getEventStatus())
                .isEqualTo(AlarmEventStatus.CREATED);
        assertThat(eventCount(request)).isEqualTo(1L);
        assertThat(alarmCount()).isEqualTo(1L);
        assertThat(outboxCount(request)).isEqualTo(1L);

        assertThat(value(
                """
                SELECT status
                FROM outbox_event
                WHERE source = ? AND event_id = ?
                """,
                String.class,
                request.getSource(),
                request.getEventId()
        )).isEqualTo("PENDING");

        assertThat(value(
                """
                SELECT retry_count
                FROM outbox_event
                WHERE source = ? AND event_id = ?
                """,
                Integer.class,
                request.getSource(),
                request.getEventId()
        )).isZero();

        assertThat(value(
                """
                SELECT published_at
                FROM outbox_event
                WHERE source = ? AND event_id = ?
                """,
                LocalDateTime.class,
                request.getSource(),
                request.getEventId()
        )).isNull();

        assertThat(value(
                """
                SELECT last_error
                FROM outbox_event
                WHERE source = ? AND event_id = ?
                """,
                String.class,
                request.getSource(),
                request.getEventId()
        )).isNull();

        assertThat(value(
                """
                SELECT exchange
                FROM outbox_event
                WHERE source = ? AND event_id = ?
                """,
                String.class,
                request.getSource(),
                request.getEventId()
        )).isEqualTo(RabbitMQConfig.DEVICE_EXCHANGE);

        assertThat(value(
                """
                SELECT routing_key
                FROM outbox_event
                WHERE source = ? AND event_id = ?
                """,
                String.class,
                request.getSource(),
                request.getEventId()
        )).isEqualTo(RabbitMQConfig.DEVICE_ALARM_ROUTING_KEY);
    }

    @Test
    void outboxInsertFailureRollsBackAlarmEventAndAlarm() {

        AlarmEventCommand request = request(
                "gateway-A",
                "EVT-V34-ROLLBACK-001",
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );

        OutboxEvent preset = outboxEvent(request);
        assertThat(outboxEventMapper.insert(preset)).isEqualTo(1);

        assertThatThrownBy(() -> alarmService.processEvent(request))
                .isInstanceOf(DataAccessException.class);

        assertThat(eventCount(request)).isZero();
        assertThat(alarmCount()).isZero();
        assertThat(value(
                """
                SELECT COUNT(*)
                FROM outbox_event
                """,
                Long.class
        )).isEqualTo(1L);
        assertThat(value(
                """
                SELECT id
                FROM outbox_event
                WHERE source = ? AND event_id = ?
                """,
                Long.class,
                request.getSource(),
                request.getEventId()
        )).isEqualTo(preset.getId());
    }

    @Test
    void alarmBusinessFailureDoesNotLeaveOutboxEvent() {

        AlarmEventCommand request = request(
                "gateway-A",
                "EVT-V34-BUSINESS-FAIL-001",
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );
        request.setDeviceId(999999L);

        assertThatThrownBy(() -> alarmService.processEvent(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40401);

        assertThat(eventCount(request)).isZero();
        assertThat(count(
                """
                SELECT COUNT(*)
                FROM alarm
                WHERE device_id = ?
                """,
                request.getDeviceId()
        )).isZero();
        assertThat(outboxCount(request)).isZero();
    }

    @Test
    void duplicateSourceAndEventIdCreatesOnlyOneOutboxEvent() {

        AlarmEventCommand request = request(
                "gateway-A",
                "EVT-V34-DUPLICATE-001",
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );

        AlarmProcessResponse first =
                alarmService.processEvent(request);
        AlarmProcessResponse second =
                alarmService.processEvent(request);

        assertThat(first.getEventStatus())
                .isEqualTo(AlarmEventStatus.CREATED);
        assertThat(second.getEventStatus())
                .isEqualTo(AlarmEventStatus.DUPLICATE);
        assertThat(eventCount(request)).isEqualTo(1L);
        assertThat(alarmCount()).isEqualTo(1L);
        assertThat(outboxCount(request)).isEqualTo(1L);
        assertThat(value(
                """
                SELECT occurrence_count
                FROM alarm
                WHERE device_id = ? AND alarm_code = ?
                """,
                Integer.class,
                DEVICE_ID,
                "TEMP_HIGH"
        )).isEqualTo(1);
    }

    @Test
    void differentEventIdsCreateTwoOutboxEventsForSameAlarm() {

        AlarmEventCommand first = request(
                "gateway-A",
                "EVT-V34-A",
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );
        AlarmEventCommand second = request(
                "gateway-A",
                "EVT-V34-B",
                LocalDateTime.of(2026, 9, 16, 10, 1)
        );

        alarmService.processEvent(first);
        alarmService.processEvent(second);

        assertThat(count(
                """
                SELECT COUNT(*)
                FROM alarm_event
                WHERE event_id IN (?, ?)
                """,
                first.getEventId(),
                second.getEventId()
        )).isEqualTo(2L);
        assertThat(alarmCount()).isEqualTo(1L);
        assertThat(count(
                """
                SELECT COUNT(*)
                FROM outbox_event
                WHERE event_id IN (?, ?)
                """,
                first.getEventId(),
                second.getEventId()
        )).isEqualTo(2L);
        assertThat(value(
                """
                SELECT occurrence_count
                FROM alarm
                WHERE device_id = ? AND alarm_code = ?
                """,
                Integer.class,
                DEVICE_ID,
                "TEMP_HIGH"
        )).isEqualTo(2);
        assertThat(jdbcTemplate.queryForList(
                """
                SELECT event_id
                FROM outbox_event
                WHERE event_id IN (?, ?)
                ORDER BY event_id
                """,
                String.class,
                first.getEventId(),
                second.getEventId()
        )).containsExactly(
                first.getEventId(),
                second.getEventId()
        );
    }

    @Test
    void outboxPayloadRoundTripsAsDeviceAlarmEventMessage() {

        AlarmEventCommand request = request(
                "gateway-A",
                "EVT-V34-PAYLOAD-001",
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );
        request.setPayload("{\"temperature\":95,\"unit\":\"C\"}");

        alarmService.processEvent(request);

        String payload = value(
                """
                SELECT payload
                FROM outbox_event
                WHERE source = ? AND event_id = ?
                """,
                String.class,
                request.getSource(),
                request.getEventId()
        );

        DeviceAlarmEventMessage restored =
                jsonMapper.readValue(
                        payload,
                        DeviceAlarmEventMessage.class
                );
        DeviceAlarmEventMessage expected = toMessage(request);

        assertThat(restored.getSource())
                .isEqualTo(expected.getSource());
        assertThat(restored.getEventId())
                .isEqualTo(expected.getEventId());
        assertThat(restored.getDeviceId())
                .isEqualTo(expected.getDeviceId());
        assertThat(restored.getAlarmCode())
                .isEqualTo(expected.getAlarmCode());
        assertThat(restored.getAlarmType())
                .isEqualTo(expected.getAlarmType());
        assertThat(restored.getAlarmLevel())
                .isEqualTo(expected.getAlarmLevel());
        assertThat(restored.getTitle())
                .isEqualTo(expected.getTitle());
        assertThat(restored.getMessage())
                .isEqualTo(expected.getMessage());
        assertThat(restored.getOccurredAt())
                .isEqualTo(expected.getOccurredAt());
        assertThat(restored.getPayload())
                .isEqualTo(expected.getPayload());
    }

    private OutboxEvent outboxEvent(AlarmEventCommand request) {

        OutboxEvent event = new OutboxEvent();
        event.setSource(request.getSource());
        event.setEventId(request.getEventId());
        event.setExchange(RabbitMQConfig.DEVICE_EXCHANGE);
        event.setRoutingKey(
                RabbitMQConfig.DEVICE_ALARM_ROUTING_KEY
        );
        event.setPayload(
                jsonMapper.writeValueAsString(toMessage(request))
        );
        event.setStatus("PENDING");
        event.setRetryCount(0);
        return event;
    }

    private DeviceAlarmEventMessage toMessage(
            AlarmEventCommand request) {

        DeviceAlarmEventMessage message =
                new DeviceAlarmEventMessage();

        message.setSource(request.getSource());
        message.setEventId(request.getEventId());
        message.setDeviceId(request.getDeviceId());
        message.setAlarmCode(request.getAlarmCode());
        message.setAlarmType(request.getAlarmType());
        message.setAlarmLevel(request.getAlarmLevel());
        message.setTitle(request.getTitle());
        message.setMessage(request.getMessage());
        message.setOccurredAt(request.getOccurredAt());
        message.setPayload(request.getPayload());

        return message;
    }

    private AlarmEventCommand request(
            String source,
            String eventId,
            LocalDateTime occurredAt) {

        AlarmEventCommand request = new AlarmEventCommand();
        request.setSource(source);
        request.setEventId(eventId);
        request.setDeviceId(DEVICE_ID);
        request.setAlarmCode("TEMP_HIGH");
        request.setAlarmType("TEMPERATURE");
        request.setAlarmLevel("CRITICAL");
        request.setTitle("设备温度过高");
        request.setMessage("Outbox integration test event");
        request.setOccurredAt(occurredAt);
        request.setPayload("{\"temperature\":95}");
        return request;
    }

    private long eventCount(AlarmEventCommand request) {

        return count(
                """
                SELECT COUNT(*)
                FROM alarm_event
                WHERE source = ? AND event_id = ?
                """,
                request.getSource(),
                request.getEventId()
        );
    }

    private long outboxCount(AlarmEventCommand request) {

        return count(
                """
                SELECT COUNT(*)
                FROM outbox_event
                WHERE source = ? AND event_id = ?
                """,
                request.getSource(),
                request.getEventId()
        );
    }

    private long alarmCount() {

        return count(
                """
                SELECT COUNT(*)
                FROM alarm
                WHERE device_id = ? AND alarm_code = ?
                """,
                DEVICE_ID,
                "TEMP_HIGH"
        );
    }

    private long count(String sql, Object... arguments) {

        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                arguments
        );
    }

    private <T> T value(
            String sql,
            Class<T> type,
            Object... arguments) {

        return jdbcTemplate.queryForObject(
                sql,
                type,
                arguments
        );
    }

    private void createTestDatabase() throws Exception {

        try (Connection connection =
                     DriverManager.getConnection(
                             SERVER_URL,
                             USERNAME,
                             PASSWORD
                     );
             Statement statement = connection.createStatement()) {

            statement.execute(
                    "CREATE DATABASE IF NOT EXISTS "
                            + TEST_DATABASE
                            + " CHARACTER SET utf8mb4"
                            + " COLLATE utf8mb4_unicode_ci"
            );
        }
    }

    private void executeSqlFile(String path) throws Exception {

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(
                    connection,
                    new FileSystemResource(path)
            );
        }
    }

    @Configuration
    @EnableTransactionManagement(proxyTargetClass = true)
    @MapperScan("com.smartfactory.mapper")
    static class TestConfiguration {

        @Bean
        DataSource dataSource() {

            MysqlDataSource dataSource = new MysqlDataSource();
            dataSource.setUrl(TEST_URL);
            dataSource.setUser(USERNAME);
            dataSource.setPassword(PASSWORD);
            return dataSource;
        }

        @Bean
        SqlSessionFactory sqlSessionFactory(
                DataSource dataSource) throws Exception {

            SqlSessionFactoryBean factory =
                    new SqlSessionFactoryBean();
            factory.setDataSource(dataSource);
            factory.setMapperLocations(
                    new PathMatchingResourcePatternResolver()
                            .getResources("classpath*:mapper/*.xml")
            );
            return factory.getObject();
        }

        @Bean
        PlatformTransactionManager transactionManager(
                DataSource dataSource) {

            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        JsonMapper jsonMapper() {
            return JsonMapper.builder().build();
        }

        @Bean
        AlarmService alarmService(
                AlarmMapper alarmMapper,
                AlarmEventMapper alarmEventMapper,
                OutboxEventMapper outboxEventMapper,
                DeviceMapper deviceMapper,
                SysUserMapper sysUserMapper,
                JsonMapper jsonMapper) {

            return new AlarmServiceImpl(
                    alarmMapper,
                    alarmEventMapper,
                    outboxEventMapper,
                    deviceMapper,
                    sysUserMapper,
                    jsonMapper
            );
        }
    }
}
