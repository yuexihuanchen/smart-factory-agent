package com.smartfactory.integration;

import com.smartfactory.common.exception.BusinessException;
import com.smartfactory.entity.Alarm;
import com.smartfactory.enums.AlarmEventStatus;
import com.smartfactory.mapper.AlarmEventMapper;
import com.smartfactory.mapper.AlarmMapper;
import com.smartfactory.mapper.DeviceMapper;
import com.smartfactory.mapper.OutboxEventMapper;
import com.smartfactory.mapper.SysUserMapper;
import com.smartfactory.service.AlarmService;
import com.smartfactory.service.command.AlarmEventCommand;
import com.smartfactory.service.impl.AlarmServiceImpl;
import com.smartfactory.vo.AlarmProcessResponse;
import com.mysql.cj.jdbc.MysqlDataSource;
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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(AlarmV2MySqlIntegrationTest.TestConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_IT", matches = "true")
class AlarmV2MySqlIntegrationTest {

    private static final String TEST_DATABASE =
            "smart_factory_alarm_it";

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
                "IT-DEVICE-01",
                "Integration Test Device",
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
    void sameEventConcurrentlyCreatesOneEventAndOneAlarmOccurrence()
            throws Exception {

        AlarmEventCommand first = request(
                "gateway-A",
                "EVT-SAME-001",
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );
        AlarmEventCommand second = request(
                "gateway-A",
                "EVT-SAME-001",
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );

        List<AlarmProcessResponse> responses =
                processConcurrently(first, second);

        assertThat(responses)
                .extracting(AlarmProcessResponse::getEventStatus)
                .containsExactlyInAnyOrder(
                        AlarmEventStatus.CREATED,
                        AlarmEventStatus.DUPLICATE
                );

        assertThat(count(
                """
                SELECT COUNT(*)
                FROM alarm_event
                WHERE source = ? AND event_id = ?
                """,
                "gateway-A",
                "EVT-SAME-001"
        )).isEqualTo(1L);

        assertThat(count(
                """
                SELECT COUNT(*)
                FROM alarm
                WHERE device_id = ? AND alarm_code = ?
                """,
                DEVICE_ID,
                "TEMP_HIGH"
        )).isEqualTo(1L);

        assertThat(value(
                """
                SELECT occurrence_count
                FROM alarm
                WHERE device_id = ? AND alarm_code = ?
                """,
                DEVICE_ID,
                "TEMP_HIGH"
        )).isEqualTo(1);
    }

    @Test
    void differentEventsConcurrentlyAggregateIntoSameAlarm()
            throws Exception {

        AlarmEventCommand first = request(
                "gateway-A",
                "EVT-DIFF-001",
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );
        AlarmEventCommand second = request(
                "gateway-A",
                "EVT-DIFF-002",
                LocalDateTime.of(2026, 9, 16, 10, 1)
        );

        List<AlarmProcessResponse> responses =
                processConcurrently(first, second);

        assertThat(responses)
                .extracting(AlarmProcessResponse::getEventStatus)
                .containsOnly(AlarmEventStatus.CREATED);

        assertThat(count(
                """
                SELECT COUNT(*)
                FROM alarm_event
                WHERE event_id IN (?, ?)
                """,
                "EVT-DIFF-001",
                "EVT-DIFF-002"
        )).isEqualTo(2L);

        assertThat(count(
                """
                SELECT COUNT(*)
                FROM alarm
                WHERE device_id = ? AND alarm_code = ?
                """,
                DEVICE_ID,
                "TEMP_HIGH"
        )).isEqualTo(1L);

        assertThat(value(
                """
                SELECT occurrence_count
                FROM alarm
                WHERE device_id = ? AND alarm_code = ?
                """,
                DEVICE_ID,
                "TEMP_HIGH"
        )).isEqualTo(2);

        assertThat(count(
                """
                SELECT COUNT(DISTINCT alarm_id)
                FROM alarm_event
                WHERE event_id IN (?, ?)
                """,
                "EVT-DIFF-001",
                "EVT-DIFF-002"
        )).isEqualTo(1L);
    }

    @Test
    void eventInsertRollsBackWhenAlarmBusinessFails() {

        AlarmEventCommand request = request(
                "gateway-A",
                "EVT-ROLLBACK-001",
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );
        request.setDeviceId(999999L);

        assertThatThrownBy(() -> alarmService.processEvent(request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(40401);

        assertThat(count(
                """
                SELECT COUNT(*)
                FROM alarm_event
                WHERE source = ? AND event_id = ?
                """,
                "gateway-A",
                "EVT-ROLLBACK-001"
        )).isZero();

        assertThat(count(
                """
                SELECT COUNT(*)
                FROM alarm
                WHERE device_id = ? AND alarm_code = ?
                """,
                999999L,
                "TEMP_HIGH"
        )).isZero();
    }

    @Test
    void newEventAfterResolvedCreatesNewAlarm() {

        AlarmProcessResponse first = alarmService.processEvent(request(
                "gateway-A",
                "EVT-RESOLVED-001",
                LocalDateTime.of(2026, 9, 16, 10, 0)
        ));

        Long firstAlarmId = first.getAlarm().getId();

        jdbcTemplate.update(
                """
                UPDATE alarm
                SET status = 'RESOLVED',
                    resolved_at = CURRENT_TIMESTAMP(3),
                    updated_at = CURRENT_TIMESTAMP(3)
                WHERE id = ?
                """,
                firstAlarmId
        );

        AlarmProcessResponse second = alarmService.processEvent(request(
                "gateway-A",
                "EVT-RESOLVED-002",
                LocalDateTime.of(2026, 9, 16, 10, 5)
        ));

        assertThat(second.getAlarm().getId())
                .isNotEqualTo(firstAlarmId);
        assertThat(second.getAlarm().getStatus()).isEqualTo("ACTIVE");

        assertThat(count(
                """
                SELECT COUNT(*)
                FROM alarm
                WHERE device_id = ? AND alarm_code = ?
                """,
                DEVICE_ID,
                "TEMP_HIGH"
        )).isEqualTo(2L);

        assertThat(count(
                """
                SELECT COUNT(DISTINCT alarm_id)
                FROM alarm_event
                WHERE event_id IN (?, ?)
                """,
                "EVT-RESOLVED-001",
                "EVT-RESOLVED-002"
        )).isEqualTo(2L);
    }

    @Test
    void outOfOrderOccurredAtDoesNotMoveLastOccurredAtBackwards() {

        alarmService.processEvent(request(
                "gateway-A",
                "EVT-TIME-NEW",
                LocalDateTime.of(2026, 9, 16, 12, 0)
        ));

        alarmService.processEvent(request(
                "gateway-A",
                "EVT-TIME-OLD",
                LocalDateTime.of(2026, 9, 16, 11, 0)
        ));

        Timestamp lastOccurredAt = jdbcTemplate.queryForObject(
                """
                SELECT last_occurred_at
                FROM alarm
                WHERE device_id = ? AND alarm_code = ?
                """,
                Timestamp.class,
                DEVICE_ID,
                "TEMP_HIGH"
        );

        assertThat(lastOccurredAt)
                .isEqualTo(Timestamp.valueOf(
                        LocalDateTime.of(2026, 9, 16, 12, 0)
                ));
    }

    private List<AlarmProcessResponse> processConcurrently(
            AlarmEventCommand... requests) throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(
                requests.length
        );
        CountDownLatch start = new CountDownLatch(1);
        List<Future<AlarmProcessResponse>> futures = new ArrayList<>();

        try {
            for (AlarmEventCommand request : requests) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return alarmService.processEvent(request);
                }));
            }

            start.countDown();

            List<AlarmProcessResponse> responses = new ArrayList<>();

            for (Future<AlarmProcessResponse> future : futures) {
                responses.add(future.get(15, TimeUnit.SECONDS));
            }

            return responses;
        } finally {
            executor.shutdownNow();
        }
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
        request.setMessage("Integration test event");
        request.setOccurredAt(occurredAt);
        return request;
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

    private Long count(String sql, Object... arguments) {

        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                arguments
        );
    }

    private Integer value(String sql, Object... arguments) {

        return jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                arguments
        );
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

        @Bean
        JsonMapper jsonMapper() {
            return JsonMapper.builder().build();
        }
    }
}
