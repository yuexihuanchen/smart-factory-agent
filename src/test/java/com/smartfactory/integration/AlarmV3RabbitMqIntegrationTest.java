package com.smartfactory.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysql.cj.jdbc.MysqlDataSource;
import com.rabbitmq.client.GetResponse;
import com.smartfactory.common.exception.BusinessException;
import com.smartfactory.common.exception.TransientAlarmException;
import com.smartfactory.config.RabbitMQConfig;
import com.smartfactory.config.RabbitMQMessageConfig;
import com.smartfactory.config.RabbitMQPublisherConfig;
import com.smartfactory.entity.Alarm;
import com.smartfactory.entity.OutboxEvent;
import com.smartfactory.enums.AlarmEventStatus;
import com.smartfactory.mapper.AlarmEventMapper;
import com.smartfactory.mapper.AlarmMapper;
import com.smartfactory.mapper.DeviceMapper;
import com.smartfactory.mapper.OutboxEventMapper;
import com.smartfactory.mapper.SysUserMapper;
import com.smartfactory.mq.DeviceAlarmEventConsumer;
import com.smartfactory.mq.DeviceAlarmCorrelationData;
import com.smartfactory.mq.DeviceAlarmEventMessage;
import com.smartfactory.mq.DeviceAlarmEventProducer;
import com.smartfactory.mq.OutboxPublisher;
import com.smartfactory.service.AlarmService;
import com.smartfactory.service.command.AlarmEventCommand;
import com.smartfactory.service.impl.AlarmServiceImpl;
import com.smartfactory.vo.AlarmProcessResponse;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariables;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListenerContainer;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.amqp.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@SpringJUnitConfig(AlarmV3RabbitMqIntegrationTest.TestConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@EnabledIfEnvironmentVariables({
        @EnabledIfEnvironmentVariable(
                named = "RUN_MYSQL_IT",
                matches = "true"
        ),
        @EnabledIfEnvironmentVariable(
                named = "RUN_RABBITMQ_IT",
                matches = "true"
        )
})
class AlarmV3RabbitMqIntegrationTest {

    private static final String TEST_DATABASE =
            "smart_factory_alarm_it_v3";

    private static final String TEST_VHOST =
            "smart_factory_alarm_it";

    private static final String MYSQL_USERNAME =
            System.getProperty("mysql.it.username", "root");

    private static final String MYSQL_PASSWORD =
            System.getProperty("mysql.it.password", "root");

    private static final String RABBITMQ_USERNAME =
            System.getProperty("rabbitmq.it.username", "admin");

    private static final String RABBITMQ_PASSWORD =
            System.getProperty("rabbitmq.it.password", "admin123");

    private static final String MYSQL_SERVER_URL =
            "jdbc:mysql://localhost:3306/"
                    + "?useUnicode=true"
                    + "&characterEncoding=utf8"
                    + "&serverTimezone=Asia/Shanghai"
                    + "&useSSL=false"
                    + "&allowPublicKeyRetrieval=true";

    private static final String MYSQL_TEST_URL =
            "jdbc:mysql://localhost:3306/" + TEST_DATABASE
                    + "?useUnicode=true"
                    + "&characterEncoding=utf8"
                    + "&serverTimezone=Asia/Shanghai"
                    + "&useSSL=false"
                    + "&allowPublicKeyRetrieval=true";

    private static final Long DEVICE_ID = 900001L;

    private static final String SOURCE = "it-gateway-A";

    private static final Duration WAIT_TIMEOUT =
            Duration.ofSeconds(15);

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().findAndRegisterModules();

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitListenerEndpointRegistry listenerRegistry;

    @Autowired
    private DeviceAlarmEventProducer producer;

    @Autowired
    @Qualifier("alarmService")
    private AlarmService alarmService;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private OutboxEventMapper outboxEventMapper;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private CountingAlarmService countingAlarmService;

    @Autowired
    private BlockingQueue<Throwable> listenerErrors;

    @Autowired
    private ConnectionFactory rabbitConnectionFactory;

    @BeforeEach
    void setUp() throws Exception {

        stopAlarmListener();
        resetDatabase();

        countingAlarmService.reset();
        listenerErrors.clear();

        rabbitAdmin.initialize();

        waitUntil(
                () -> managementClient().getQueue(
                        RabbitMQConfig.DEVICE_ALARM_QUEUE
                ) != null,
                WAIT_TIMEOUT,
                this::diagnostics
        );

        resetQueue();
        startAlarmListener();
    }

    @AfterEach
    void tearDown() throws Exception {

        stopAlarmListener();
        resetQueue();
        listenerErrors.clear();
    }

    @AfterAll
    void cleanUp() throws Exception {

        stopAlarmListener();

        if (rabbitConnectionFactory
                instanceof CachingConnectionFactory connectionFactory) {
            connectionFactory.destroy();
        }

        managementClient().deleteVhost();

        try (Connection connection =
                     DriverManager.getConnection(
                             MYSQL_SERVER_URL,
                             MYSQL_USERNAME,
                             MYSQL_PASSWORD
                     );
             Statement statement = connection.createStatement()) {

            statement.execute(
                    "DROP DATABASE IF EXISTS " + TEST_DATABASE
            );
        }
    }

    @Test
    void rabbitMqTopologyIsDeclaredInIsolatedVhost() {

        JsonNode exchange = managementClient().getExchange(
                RabbitMQConfig.DEVICE_EXCHANGE
        );
        JsonNode queue = managementClient().getQueue(
                RabbitMQConfig.DEVICE_ALARM_QUEUE
        );
        JsonNode dlx = managementClient().getExchange(
                RabbitMQConfig.DEVICE_ALARM_DLX
        );
        JsonNode dlq = managementClient().getQueue(
                RabbitMQConfig.DEVICE_ALARM_DLQ
        );
        JsonNode bindings = managementClient().getBindings(
                RabbitMQConfig.DEVICE_EXCHANGE,
                RabbitMQConfig.DEVICE_ALARM_QUEUE
        );
        JsonNode dlqBindings = managementClient().getBindings(
                RabbitMQConfig.DEVICE_ALARM_DLX,
                RabbitMQConfig.DEVICE_ALARM_DLQ
        );

        assertThat(exchange).isNotNull();
        assertThat(exchange.path("type").asText())
                .isEqualTo("direct");
        assertThat(exchange.path("durable").asBoolean())
                .isTrue();

        assertThat(queue).isNotNull();
        assertThat(queue.path("durable").asBoolean())
                .isTrue();
        assertThat(queue.path("arguments")
                .path("x-dead-letter-exchange").asText())
                .isEqualTo(RabbitMQConfig.DEVICE_ALARM_DLX);
        assertThat(queue.path("arguments")
                .path("x-dead-letter-routing-key").asText())
                .isEqualTo(RabbitMQConfig.DEVICE_ALARM_DLQ_ROUTING_KEY);

        assertThat(dlx).isNotNull();
        assertThat(dlx.path("type").asText())
                .isEqualTo("direct");
        assertThat(dlx.path("durable").asBoolean())
                .isTrue();

        assertThat(dlq).isNotNull();
        assertThat(dlq.path("durable").asBoolean())
                .isTrue();
        assertThat(dlq.path("consumers").asInt())
                .isZero();

        assertThat(bindings)
                .anySatisfy(binding -> assertThat(
                        binding.path("routing_key").asText()
                ).isEqualTo(RabbitMQConfig.DEVICE_ALARM_ROUTING_KEY));
        assertThat(dlqBindings)
                .anySatisfy(binding -> assertThat(
                        binding.path("routing_key").asText()
                ).isEqualTo(
                        RabbitMQConfig.DEVICE_ALARM_DLQ_ROUTING_KEY
                ));
    }

    @Test
    void producerRoutesMessageToAlarmQueue() throws Exception {

        stopAlarmListener();
        resetQueue();

        String eventId = "EVT-ROUTE-" + UUID.randomUUID();
        DeviceAlarmEventMessage message = message(
                eventId,
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );

        DeviceAlarmCorrelationData correlationData =
                producer.send(message);

        waitUntil(
                () -> confirmAcknowledged(correlationData),
                WAIT_TIMEOUT,
                this::diagnostics
        );

        waitUntil(
                () -> queueMessageCount(
                        RabbitMQConfig.DEVICE_ALARM_QUEUE
                ) == 1,
                WAIT_TIMEOUT,
                this::diagnostics
        );

        assertThat(queueMessageCount(
                RabbitMQConfig.DEVICE_ALARM_QUEUE
        )).isEqualTo(1);
        assertThat(correlationData.getReturned()).isNull();

        startAlarmListener();

        waitUntil(
                () -> eventCount(eventId) == 1
                        && queueMessageCount(
                                RabbitMQConfig.DEVICE_ALARM_QUEUE
                        ) == 0,
                WAIT_TIMEOUT,
                this::diagnostics
        );
    }

    @Test
    void unroutableMessageIsReturnedAndNotQueued()
            throws Exception {

        stopAlarmListener();
        resetQueue();

        waitUntil(
                () -> queueMessageCount(
                        RabbitMQConfig.DEVICE_ALARM_QUEUE
                ) == 0
                        && queueMessageCount(
                                RabbitMQConfig.DEVICE_ALARM_DLQ
                        ) == 0,
                WAIT_TIMEOUT,
                this::diagnostics
        );

        DeviceAlarmEventMessage message = message(
                "EVT-RETURN-" + UUID.randomUUID(),
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );

        DeviceAlarmCorrelationData correlationData = producer.send(
                RabbitMQConfig.DEVICE_EXCHANGE,
                "device.alarm.invalid",
                message
        );

        waitUntil(
                () -> correlationData.getFuture().isDone()
                        && correlationData.getReturned() != null,
                WAIT_TIMEOUT,
                this::diagnostics
        );

        ReturnedMessage returned = correlationData.getReturned();

        assertThat(correlationData.getFuture().get().ack())
                .isTrue();
        assertThat(returned.getExchange())
                .isEqualTo(RabbitMQConfig.DEVICE_EXCHANGE);
        assertThat(returned.getRoutingKey())
                .isEqualTo("device.alarm.invalid");
        Object sourceHeader = returned.getMessage()
                .getMessageProperties()
                .getHeader("alarm.source");
        Object eventIdHeader = returned.getMessage()
                .getMessageProperties()
                .getHeader("alarm.eventId");

        assertThat(sourceHeader)
                .isEqualTo(message.getSource());
        assertThat(eventIdHeader)
                .isEqualTo(message.getEventId());
        assertThat(queueMessageCount(
                RabbitMQConfig.DEVICE_ALARM_QUEUE
        )).isZero();
        assertThat(queueMessageCount(
                RabbitMQConfig.DEVICE_ALARM_DLQ
        )).isZero();
    }

    @Test
    void correlationDataIdentifiesOriginalAlarmEvent()
            throws Exception {

        DeviceAlarmEventMessage message = message(
                "EVT-V3-001",
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );
        message.setSource("gateway-A");

        DeviceAlarmCorrelationData correlationData =
                producer.send(message);

        waitUntil(
                () -> confirmAcknowledged(correlationData),
                WAIT_TIMEOUT,
                this::diagnostics
        );

        assertThat(correlationData.getSource())
                .isEqualTo("gateway-A");
        assertThat(correlationData.getEventId())
                .isEqualTo("EVT-V3-001");
        assertThat(correlationData.getExchange())
                .isEqualTo(RabbitMQConfig.DEVICE_EXCHANGE);
        assertThat(correlationData.getRoutingKey())
                .isEqualTo(RabbitMQConfig.DEVICE_ALARM_ROUTING_KEY);
        assertThat(correlationData.getReturned()).isNull();
    }

    @Test
    void outboxPublisherMarksSentAfterRealBrokerConfirm()
            throws Exception {

        stopAlarmListener();
        resetQueue();

        DeviceAlarmEventMessage message = message(
                "EVT-OUTBOX-ACK-" + UUID.randomUUID(),
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );
        OutboxEvent outbox = insertPendingOutbox(
                message,
                RabbitMQConfig.DEVICE_ALARM_ROUTING_KEY
        );

        List<OutboxPublisher.PublishResult> results =
                outboxPublisher.publishPending();

        assertThat(results)
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.outboxId())
                            .isEqualTo(outbox.getId());
                    assertThat(result.outcome())
                            .isEqualTo(
                                    OutboxPublisher.Outcome.SENT
                            );
                    assertThat(result.confirmed()).isTrue();
                    assertThat(result.returned()).isFalse();
                });

        waitUntil(
                () -> "SENT".equals(outboxStatus(outbox.getId())),
                WAIT_TIMEOUT,
                this::diagnostics
        );
        waitUntil(
                () -> queueMessageCount(
                        RabbitMQConfig.DEVICE_ALARM_QUEUE
                ) == 1,
                WAIT_TIMEOUT,
                this::diagnostics
        );

        assertThat(outboxPublishedAt(outbox.getId()))
                .isNotNull();
        assertThat(outboxLastError(outbox.getId())).isNull();
        assertThat(outboxRetryCount(outbox.getId())).isZero();
    }

    @Test
    void outboxPublisherKeepsReturnedMessageProcessing()
            throws Exception {

        stopAlarmListener();
        resetQueue();

        DeviceAlarmEventMessage message = message(
                "EVT-OUTBOX-RETURN-" + UUID.randomUUID(),
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );
        OutboxEvent outbox = insertPendingOutbox(
                message,
                "device.alarm.invalid"
        );

        List<OutboxPublisher.PublishResult> results =
                outboxPublisher.publishPending();

        assertThat(results)
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.outboxId())
                            .isEqualTo(outbox.getId());
                    assertThat(result.outcome())
                            .isEqualTo(
                                    OutboxPublisher.Outcome.RETURNED
                            );
                    assertThat(result.confirmed()).isTrue();
                    assertThat(result.returned()).isTrue();
                    assertThat(result.replyCode()).isEqualTo(312);
                    assertThat(result.replyText())
                            .isEqualTo("NO_ROUTE");
                });

        assertThat(outboxStatus(outbox.getId()))
                .isEqualTo("PROCESSING");
        assertThat(outboxLeaseOwner(outbox.getId()))
                .isNotBlank();
        assertThat(outboxRetryCount(outbox.getId())).isEqualTo(1);
        assertThat(outboxLastError(outbox.getId()))
                .contains("NO_ROUTE", "312");
        assertThat(queueMessageCount(
                RabbitMQConfig.DEVICE_ALARM_QUEUE
        )).isZero();
        assertThat(queueMessageCount(
                RabbitMQConfig.DEVICE_ALARM_DLQ
        )).isZero();
    }

    @Test
    void outboxPublisherKeepsProcessingWhenPublishThrows()
            throws Exception {

        RabbitTemplate brokenRabbitTemplate =
                mock(RabbitTemplate.class);

        doThrow(new RuntimeException("simulated publish failure"))
                .when(brokenRabbitTemplate)
                .convertAndSend(
                        anyString(),
                        anyString(),
                        any(DeviceAlarmEventMessage.class),
                        any(MessagePostProcessor.class),
                        any(CorrelationData.class)
                );

        OutboxPublisher brokenPublisher = new OutboxPublisher(
                outboxEventMapper,
                brokenRabbitTemplate,
                jsonMapper
        );

        DeviceAlarmEventMessage message = message(
                "EVT-OUTBOX-EXCEPTION-" + UUID.randomUUID(),
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );
        OutboxEvent outbox = insertPendingOutbox(
                message,
                RabbitMQConfig.DEVICE_ALARM_ROUTING_KEY
        );

        List<OutboxPublisher.PublishResult> results =
                brokenPublisher.publishPending();

        assertThat(results)
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.outboxId())
                            .isEqualTo(outbox.getId());
                    assertThat(result.outcome())
                            .isEqualTo(
                                    OutboxPublisher.Outcome
                                            .PUBLISH_FAILED
                            );
                });

        assertThat(outboxStatus(outbox.getId()))
                .isEqualTo("PROCESSING");
        assertThat(outboxLeaseOwner(outbox.getId()))
                .isNotBlank();
        assertThat(outboxRetryCount(outbox.getId())).isEqualTo(1);
        assertThat(outboxLastError(outbox.getId()))
                .contains("simulated publish failure");
        assertThat(outboxPublishedAt(outbox.getId())).isNull();
    }

    @Test
    void outboxPublisherUpdatesEachCorrelatedRecord()
            throws Exception {

        stopAlarmListener();
        resetQueue();

        OutboxEvent first = insertPendingOutbox(
                message(
                        "EVT-OUTBOX-MULTI-A-" + UUID.randomUUID(),
                        LocalDateTime.of(2026, 9, 16, 10, 0)
                ),
                RabbitMQConfig.DEVICE_ALARM_ROUTING_KEY
        );
        OutboxEvent second = insertPendingOutbox(
                message(
                        "EVT-OUTBOX-MULTI-B-" + UUID.randomUUID(),
                        LocalDateTime.of(2026, 9, 16, 10, 1)
                ),
                RabbitMQConfig.DEVICE_ALARM_ROUTING_KEY
        );

        List<OutboxPublisher.PublishResult> results =
                outboxPublisher.publishPending();

        assertThat(results)
                .hasSize(2)
                .allSatisfy(result -> assertThat(result.outcome())
                        .isEqualTo(OutboxPublisher.Outcome.SENT))
                .extracting(OutboxPublisher.PublishResult::outboxId)
                .containsExactlyInAnyOrder(
                        first.getId(),
                        second.getId()
                );

        assertThat(outboxStatus(first.getId()))
                .isEqualTo("SENT");
        assertThat(outboxStatus(second.getId()))
                .isEqualTo("SENT");
        assertThat(queueMessageCount(
                RabbitMQConfig.DEVICE_ALARM_QUEUE
        )).isEqualTo(2);
    }

    @Test
    void outboxPublisherReclaimsExpiredProcessing()
            throws Exception {

        stopAlarmListener();
        resetQueue();

        DeviceAlarmEventMessage message = message(
                "EVT-OUTBOX-RECLAIM-" + UUID.randomUUID(),
                LocalDateTime.of(2026, 9, 20, 10, 0)
        );
        OutboxEvent outbox = insertPendingOutbox(
                message,
                RabbitMQConfig.DEVICE_ALARM_ROUTING_KEY
        );

        LocalDateTime expiredNow =
                LocalDateTime.now().minusSeconds(60).withNano(0);

        assertThat(outboxEventMapper.claim(
                outbox.getId(),
                "crashed-owner",
                expiredNow.minusSeconds(1),
                expiredNow
        )).isEqualTo(1);
        assertThat(outboxLeaseOwner(outbox.getId()))
                .isEqualTo("crashed-owner");

        List<OutboxPublisher.PublishResult> results =
                outboxPublisher.publishPending();

        assertThat(results)
                .singleElement()
                .satisfies(result -> assertThat(result.outcome())
                        .isEqualTo(OutboxPublisher.Outcome.SENT));
        assertThat(outboxStatus(outbox.getId()))
                .isEqualTo("SENT");
        assertThat(outboxLeaseOwner(outbox.getId()))
                .isNotEqualTo("crashed-owner");
        waitUntil(
                () -> queueMessageCount(
                        RabbitMQConfig.DEVICE_ALARM_QUEUE
                ) == 1,
                WAIT_TIMEOUT,
                this::diagnostics
        );
    }

    @Test
    void expiredProcessingRecoveryKeepsConsumerIdempotency()
            throws Exception {

        startAlarmListener();

        DeviceAlarmEventMessage message = message(
                "EVT-OUTBOX-RECLAIM-E2E-" + UUID.randomUUID(),
                LocalDateTime.of(2026, 9, 20, 10, 0)
        );

        alarmService.processEvent(toCommand(message));

        OutboxEvent outbox =
                outboxEventMapper.findBySourceAndEventId(
                        message.getSource(),
                        message.getEventId()
                );

        LocalDateTime expiredNow =
                LocalDateTime.now().minusSeconds(60).withNano(0);

        assertThat(outboxEventMapper.claim(
                outbox.getId(),
                "crashed-owner",
                expiredNow.minusSeconds(1),
                expiredNow
        )).isEqualTo(1);

        List<OutboxPublisher.PublishResult> results =
                outboxPublisher.publishPending();

        assertThat(results)
                .singleElement()
                .satisfies(result -> assertThat(result.outcome())
                        .isEqualTo(OutboxPublisher.Outcome.SENT));

        waitUntil(
                () -> "SENT".equals(outboxStatus(outbox.getId()))
                        && countingAlarmService.callCount() == 1
                        && eventCount(message.getEventId()) == 1
                        && alarmCount() == 1
                        && alarmOccurrenceCount() == 1
                        && queueMessageCount(
                                RabbitMQConfig.DEVICE_ALARM_QUEUE
                        ) == 0,
                WAIT_TIMEOUT,
                this::diagnostics
        );
    }

    @Test
    void outboxPublisherCompletesAlarmChainWithoutDuplicateAggregation()
            throws Exception {

        startAlarmListener();

        DeviceAlarmEventMessage message = message(
                "EVT-OUTBOX-E2E-" + UUID.randomUUID(),
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );

        alarmService.processEvent(toCommand(message));

        OutboxEvent outbox =
                outboxEventMapper.findBySourceAndEventId(
                        message.getSource(),
                        message.getEventId()
                );

        assertThat(outbox).isNotNull();
        assertThat(outbox.getStatus()).isEqualTo("PENDING");

        List<OutboxPublisher.PublishResult> results =
                outboxPublisher.publishPending();

        assertThat(results)
                .singleElement()
                .satisfies(result -> assertThat(result.outcome())
                        .isEqualTo(OutboxPublisher.Outcome.SENT));

        waitUntil(
                () -> "SENT".equals(outboxStatus(outbox.getId()))
                        && countingAlarmService.callCount() == 1
                        && eventCount(message.getEventId()) == 1
                        && alarmCount() == 1
                        && alarmOccurrenceCount() == 1
                        && queueMessageCount(
                                RabbitMQConfig.DEVICE_ALARM_QUEUE
                        ) == 0,
                WAIT_TIMEOUT,
                this::diagnostics
        );
    }

    @Test
    void producerConsumerServiceAndMySqlCompleteEndToEnd()
            throws Exception {

        String eventId = "EVT-E2E-" + UUID.randomUUID();
        LocalDateTime occurredAt =
                LocalDateTime.of(2026, 9, 16, 10, 0);
        String payload = "{\"temperature\":95}";

        producer.send(message(
                eventId,
                occurredAt,
                payload
        ));

        waitUntil(
                () -> eventCount(eventId) == 1
                        && alarmCount() == 1,
                WAIT_TIMEOUT,
                this::diagnostics
        );

        Long eventAlarmId = jdbcTemplate.queryForObject(
                """
                SELECT alarm_id
                FROM alarm_event
                WHERE source = ? AND event_id = ?
                """,
                Long.class,
                SOURCE,
                eventId
        );
        Long alarmId = jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM alarm
                WHERE device_id = ? AND alarm_code = ?
                """,
                Long.class,
                DEVICE_ID,
                "TEMP_HIGH"
        );
        MapRow alarm = jdbcTemplate.queryForObject(
                """
                SELECT status, occurrence_count
                FROM alarm
                WHERE id = ?
                """,
                (resultSet, rowNumber) -> new MapRow(
                        resultSet.getString("status"),
                        resultSet.getInt("occurrence_count")
                ),
                alarmId
        );
        String storedPayload = jdbcTemplate.queryForObject(
                """
                SELECT payload
                FROM alarm_event
                WHERE source = ? AND event_id = ?
                """,
                String.class,
                SOURCE,
                eventId
        );
        Timestamp storedOccurredAt = jdbcTemplate.queryForObject(
                """
                SELECT occurred_at
                FROM alarm_event
                WHERE source = ? AND event_id = ?
                """,
                Timestamp.class,
                SOURCE,
                eventId
        );

        assertThat(eventAlarmId).isEqualTo(alarmId);
        assertThat(alarm.status()).isEqualTo("ACTIVE");
        assertThat(alarm.occurrenceCount()).isEqualTo(1);
        assertThat(OBJECT_MAPPER.readTree(storedPayload))
                .isEqualTo(OBJECT_MAPPER.readTree(payload));
        assertThat(storedOccurredAt)
                .isEqualTo(Timestamp.valueOf(occurredAt));
    }

    @Test
    void duplicateSourceAndEventIdIsConsumedButIdempotent()
            throws Exception {

        DeviceAlarmEventMessage message = message(
                "EVT-DUPLICATE-" + UUID.randomUUID(),
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );

        producer.send(message);
        producer.send(message);

        waitUntil(
                () -> countingAlarmService.callCount() == 2
                        && countingAlarmService.responseCount() == 2
                        && eventCount(message.getEventId()) == 1
                        && alarmCount() == 1
                        && alarmOccurrenceCount() == 1,
                WAIT_TIMEOUT,
                this::diagnostics
        );

        List<AlarmProcessResponse> responses =
                countingAlarmService.responses();

        assertThat(responses)
                .extracting(AlarmProcessResponse::getEventStatus)
                .containsExactly(
                        AlarmEventStatus.CREATED,
                        AlarmEventStatus.DUPLICATE
                );
        assertThat(queueMessageCount(
                RabbitMQConfig.DEVICE_ALARM_QUEUE
        )).isZero();
    }

    @Test
    void differentEventIdsAggregateIntoSameAlarm()
            throws Exception {

        DeviceAlarmEventMessage first = message(
                "EVT-A-" + UUID.randomUUID(),
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );
        DeviceAlarmEventMessage second = message(
                "EVT-B-" + UUID.randomUUID(),
                LocalDateTime.of(2026, 9, 16, 10, 1)
        );

        producer.send(first);
        producer.send(second);

        waitUntil(
                () -> eventCount(first.getEventId()) == 1
                        && eventCount(second.getEventId()) == 1
                        && alarmCount() == 1
                        && alarmOccurrenceCount() == 2,
                WAIT_TIMEOUT,
                this::diagnostics
        );

        assertThat(distinctAlarmIds(
                first.getEventId(),
                second.getEventId()
        )).isEqualTo(1L);
    }

    @Test
    void consumerExceptionIsNotSwallowedAndTransactionRollsBack()
            throws Exception {

        DeviceAlarmEventMessage message = message(
                "EVT-FAIL-" + UUID.randomUUID(),
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );
        message.setDeviceId(999999L);

        producer.send(message);

        waitUntil(
                () -> queueMessageCount(
                        RabbitMQConfig.DEVICE_ALARM_QUEUE
                ) == 0
                        && queueMessageCount(
                                RabbitMQConfig.DEVICE_ALARM_DLQ
                        ) == 1
                        && countingAlarmService.callCount() == 1
                        && !listenerErrors.isEmpty(),
                WAIT_TIMEOUT,
                this::diagnostics
        );

        Throwable error = listenerErrors.poll(15, TimeUnit.SECONDS);

        assertThat(error).isNotNull();

        Throwable rootCause = rootCause(error);
        assertThat(rootCause)
                .isInstanceOf(BusinessException.class);
        assertThat(((BusinessException) rootCause).getCode())
                .isEqualTo(40401);

        waitUntil(
                () -> eventCount(message.getEventId()) == 0
                        && count(
                                """
                                SELECT COUNT(*)
                                FROM alarm
                                WHERE device_id = ?
                                """,
                                999999L
                        ) == 0,
                WAIT_TIMEOUT,
                this::diagnostics
        );

        DeviceAlarmEventMessage dlqMessage = receiveDlqMessage();

        assertThat(dlqMessage.getSource())
                .isEqualTo(message.getSource());
        assertThat(dlqMessage.getEventId())
                .isEqualTo(message.getEventId());
        assertThat(dlqMessage.getDeviceId())
                .isEqualTo(message.getDeviceId());
        assertThat(dlqMessage.getAlarmCode())
                .isEqualTo(message.getAlarmCode());
        assertThat(dlqMessage.getOccurredAt())
                .isEqualTo(message.getOccurredAt());
        assertThat(dlqMessage.getPayload())
                .isEqualTo(message.getPayload());
    }

    @Test
    void newEventAfterResolvedCreatesDifferentAlarm() throws Exception {

        DeviceAlarmEventMessage first = message(
                "EVT-RESOLVED-A-" + UUID.randomUUID(),
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );

        producer.send(first);

        waitUntil(
                () -> eventCount(first.getEventId()) == 1
                        && alarmCount() == 1,
                WAIT_TIMEOUT,
                this::diagnostics
        );

        Long firstAlarmId = alarmId();

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

        assertThat(openKeyIsNull(firstAlarmId)).isTrue();

        DeviceAlarmEventMessage second = message(
                "EVT-RESOLVED-B-" + UUID.randomUUID(),
                LocalDateTime.of(2026, 9, 16, 10, 5)
        );

        producer.send(second);

        waitUntil(
                () -> eventCount(second.getEventId()) == 1
                        && alarmCount() == 2,
                WAIT_TIMEOUT,
                this::diagnostics
        );

        Long secondAlarmId = jdbcTemplate.queryForObject(
                """
                SELECT alarm_id
                FROM alarm_event
                WHERE source = ? AND event_id = ?
                """,
                Long.class,
                SOURCE,
                second.getEventId()
        );

        assertThat(secondAlarmId)
                .isNotEqualTo(firstAlarmId);
    }

    @Test
    void outOfOrderOccurredAtDoesNotMoveLastOccurredAtBackwards()
            throws Exception {

        DeviceAlarmEventMessage newer = message(
                "EVT-TIME-NEW-" + UUID.randomUUID(),
                LocalDateTime.of(2026, 9, 16, 12, 0)
        );
        DeviceAlarmEventMessage older = message(
                "EVT-TIME-OLD-" + UUID.randomUUID(),
                LocalDateTime.of(2026, 9, 16, 11, 0)
        );

        producer.send(newer);
        waitUntil(
                () -> eventCount(newer.getEventId()) == 1,
                WAIT_TIMEOUT,
                this::diagnostics
        );

        producer.send(older);
        waitUntil(
                () -> eventCount(older.getEventId()) == 1
                        && alarmOccurrenceCount() == 2,
                WAIT_TIMEOUT,
                this::diagnostics
        );

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
                .isEqualTo(Timestamp.valueOf(newer.getOccurredAt()));
    }

    @Test
    void transientFailureIsRetriedThenSucceeds() throws Exception {

        String eventId = "EVT-RETRY-" + UUID.randomUUID();

        countingAlarmService.failNext(1);

        producer.send(message(
                eventId,
                LocalDateTime.of(2026, 9, 16, 10, 0)
        ));

        waitUntil(
                () -> countingAlarmService.callCount() == 2
                        && countingAlarmService.responseCount() == 1
                        && eventCount(eventId) == 1
                        && alarmCount() == 1
                        && alarmOccurrenceCount() == 1
                        && queueMessageCount(
                                RabbitMQConfig.DEVICE_ALARM_QUEUE
                        ) == 0,
                WAIT_TIMEOUT,
                this::diagnostics
        );

        assertThat(countingAlarmService.transientFailureCount())
                .isEqualTo(1);
        assertThat(listenerErrors).isEmpty();
    }

    @Test
    void transientFailureExhaustsRetriesAndGoesToDlq()
            throws Exception {

        String eventId = "EVT-RETRY-DLQ-" + UUID.randomUUID();
        DeviceAlarmEventMessage message = message(
                eventId,
                LocalDateTime.of(2026, 9, 16, 10, 0)
        );

        countingAlarmService.failNext(3);

        producer.send(message);

        waitUntil(
                () -> countingAlarmService.callCount() == 3
                        && queueMessageCount(
                                RabbitMQConfig.DEVICE_ALARM_QUEUE
                        ) == 0
                        && queueMessageCount(
                                RabbitMQConfig.DEVICE_ALARM_DLQ
                        ) == 1
                        && !listenerErrors.isEmpty(),
                WAIT_TIMEOUT,
                this::diagnostics
        );

        Throwable error = listenerErrors.poll(1, TimeUnit.SECONDS);
        assertThat(error).isNotNull();
        assertThat(rootCause(error))
                .isInstanceOf(TransientAlarmException.class);

        DeviceAlarmEventMessage dlqMessage = receiveDlqMessage();
        assertThat(dlqMessage.getEventId()).isEqualTo(eventId);
        assertThat(eventCount(eventId)).isZero();
        assertThat(alarmCount()).isZero();
    }

    private void resetDatabase() throws Exception {

        try (Connection connection =
                     DriverManager.getConnection(
                             MYSQL_SERVER_URL,
                             MYSQL_USERNAME,
                             MYSQL_PASSWORD
                     );
             Statement statement = connection.createStatement()) {

            statement.execute(
                    "CREATE DATABASE IF NOT EXISTS "
                            + TEST_DATABASE
                            + " CHARACTER SET utf8mb4"
                            + " COLLATE utf8mb4_unicode_ci"
            );
        }

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
                "IT-V3-DEVICE-01",
                "Alarm V3 Integration Device",
                "PLC",
                "RUNNING"
        );
    }

    private void executeSqlFile(String path) throws Exception {

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(
                    connection,
                    new FileSystemResource(path)
            );
        }
    }

    private void resetQueue() {
        rabbitAdmin.purgeQueue(RabbitMQConfig.DEVICE_ALARM_QUEUE);
        rabbitAdmin.purgeQueue(RabbitMQConfig.DEVICE_ALARM_DLQ);
    }

    private DeviceAlarmEventMessage receiveDlqMessage()
            throws Exception {

        Message rawMessage = rabbitTemplate.execute(channel -> {
            GetResponse response = channel.basicGet(
                    RabbitMQConfig.DEVICE_ALARM_DLQ,
                    false
            );

            if (response == null) {
                return null;
            }

            Message message = new Message(
                    response.getBody(),
                    new MessageProperties()
            );

            channel.basicAck(
                    response.getEnvelope().getDeliveryTag(),
                    false
            );

            return message;
        });

        assertThat(rawMessage).isNotNull();

        DeviceAlarmEventMessage converted =
                OBJECT_MAPPER.readValue(
                        rawMessage.getBody(),
                        DeviceAlarmEventMessage.class
        );

        assertThat(converted)
                .isInstanceOf(DeviceAlarmEventMessage.class);

        return converted;
    }

    private void startAlarmListener() {

        MessageListenerContainer container = alarmListenerContainer();

        if (!container.isRunning()) {
            container.start();
        }
    }

    private void stopAlarmListener() {

        MessageListenerContainer container = alarmListenerContainer();

        if (container.isRunning()) {
            container.stop();
        }
    }

    private MessageListenerContainer alarmListenerContainer() {

        return listenerRegistry.getListenerContainers()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Alarm listener container not found"
                ));
    }

    private DeviceAlarmEventMessage message(
            String eventId,
            LocalDateTime occurredAt) {
        return message(
                eventId,
                occurredAt,
                "{\"temperature\":95}"
        );
    }

    private DeviceAlarmEventMessage message(
            String eventId,
            LocalDateTime occurredAt,
            String payload) {

        DeviceAlarmEventMessage message =
                new DeviceAlarmEventMessage();
        message.setSource(SOURCE);
        message.setEventId(eventId);
        message.setDeviceId(DEVICE_ID);
        message.setAlarmCode("TEMP_HIGH");
        message.setAlarmType("TEMPERATURE");
        message.setAlarmLevel("CRITICAL");
        message.setTitle("设备温度过高");
        message.setMessage("RabbitMQ integration test event");
        message.setOccurredAt(occurredAt);
        message.setPayload(payload);
        return message;
    }

    private long eventCount(String eventId) {

        return count(
                """
                SELECT COUNT(*)
                FROM alarm_event
                WHERE source = ? AND event_id = ?
                """,
                SOURCE,
                eventId
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

    private int alarmOccurrenceCount() {

        return jdbcTemplate.queryForObject(
                """
                SELECT occurrence_count
                FROM alarm
                WHERE device_id = ? AND alarm_code = ?
                """,
                Integer.class,
                DEVICE_ID,
                "TEMP_HIGH"
        );
    }

    private Long alarmId() {

        return jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM alarm
                WHERE device_id = ? AND alarm_code = ?
                """,
                Long.class,
                DEVICE_ID,
                "TEMP_HIGH"
        );
    }

    private long distinctAlarmIds(
            String firstEventId,
            String secondEventId) {

        return count(
                """
                SELECT COUNT(DISTINCT alarm_id)
                FROM alarm_event
                WHERE event_id IN (?, ?)
                """,
                firstEventId,
                secondEventId
        );
    }

    private boolean openKeyIsNull(Long alarmId) {

        Boolean result = jdbcTemplate.queryForObject(
                """
                SELECT open_key IS NULL
                FROM alarm
                WHERE id = ?
                """,
                Boolean.class,
                alarmId
        );

        return Boolean.TRUE.equals(result);
    }

    private boolean confirmAcknowledged(
            DeviceAlarmCorrelationData correlationData) {

        if (!correlationData.getFuture().isDone()) {
            return false;
        }

        try {
            return correlationData.getFuture().get().ack();
        } catch (Exception e) {
            return false;
        }
    }

    private long count(String sql, Object... arguments) {

        Long result = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                arguments
        );

        return result == null ? 0L : result;
    }

    private long queueMessageCount(String queueName) {

        Long count = rabbitTemplate.execute(channel ->
                (long) channel
                        .queueDeclarePassive(queueName)
                        .getMessageCount()
        );

        return count == null ? -1L : count;
    }

    private OutboxEvent insertPendingOutbox(
            DeviceAlarmEventMessage message,
            String routingKey) {

        OutboxEvent outbox = new OutboxEvent();

        outbox.setSource(message.getSource());
        outbox.setEventId(message.getEventId());
        outbox.setExchange(RabbitMQConfig.DEVICE_EXCHANGE);
        outbox.setRoutingKey(routingKey);
        outbox.setPayload(
                jsonMapper.writeValueAsString(message)
        );
        outbox.setStatus("PENDING");
        outbox.setRetryCount(0);

        assertThat(outboxEventMapper.insert(outbox))
                .isEqualTo(1);

        return outbox;
    }

    private AlarmEventCommand toCommand(
            DeviceAlarmEventMessage message) {

        AlarmEventCommand command = new AlarmEventCommand();

        command.setSource(message.getSource());
        command.setEventId(message.getEventId());
        command.setDeviceId(message.getDeviceId());
        command.setAlarmCode(message.getAlarmCode());
        command.setAlarmType(message.getAlarmType());
        command.setAlarmLevel(message.getAlarmLevel());
        command.setTitle(message.getTitle());
        command.setMessage(message.getMessage());
        command.setOccurredAt(message.getOccurredAt());
        command.setPayload(message.getPayload());

        return command;
    }

    private String outboxStatus(Long outboxId) {

        return value(
                """
                SELECT status
                FROM outbox_event
                WHERE id = ?
                """,
                String.class,
                outboxId
        );
    }

    private Integer outboxRetryCount(Long outboxId) {

        return value(
                """
                SELECT retry_count
                FROM outbox_event
                WHERE id = ?
                """,
                Integer.class,
                outboxId
        );
    }

    private Timestamp outboxPublishedAt(Long outboxId) {

        return value(
                """
                SELECT published_at
                FROM outbox_event
                WHERE id = ?
                """,
                Timestamp.class,
                outboxId
        );
    }

    private String outboxLastError(Long outboxId) {

        return value(
                """
                SELECT last_error
                FROM outbox_event
                WHERE id = ?
                """,
                String.class,
                outboxId
        );
    }

    private String outboxLeaseOwner(Long outboxId) {

        return value(
                """
                SELECT lease_owner
                FROM outbox_event
                WHERE id = ?
                """,
                String.class,
                outboxId
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

    private Throwable rootCause(Throwable throwable) {

        Throwable current = throwable;

        while (current.getCause() != null) {
            current = current.getCause();
        }

        return current;
    }

    private void waitUntil(
            BooleanSupplier condition,
            Duration timeout,
            Supplier<String> diagnostics) throws Exception {

        long deadline = System.nanoTime() + timeout.toNanos();
        Exception lastException = null;

        while (System.nanoTime() < deadline) {
            try {
                if (condition.getAsBoolean()) {
                    return;
                }
            } catch (Exception e) {
                lastException = e;
            }

            Thread.sleep(100);
        }

        String message = diagnostics.get();

        if (lastException != null) {
            throw new AssertionError(
                    message + ", lastError=" + lastException,
                    lastException
            );
        }

        throw new AssertionError(message);
    }

    private String diagnostics() {

        return "queue=" + managementClient()
                .getQueue(RabbitMQConfig.DEVICE_ALARM_QUEUE)
                + ", dlq=" + managementClient()
                .getQueue(RabbitMQConfig.DEVICE_ALARM_DLQ)
                + ", serviceCalls="
                + countingAlarmService.callCount()
                + ", transientFailures="
                + countingAlarmService.transientFailureCount()
                + ", lastListenerError="
                + listenerErrors.peek()
                + ", alarmEvents=" + count(
                "SELECT COUNT(*) FROM alarm_event"
        )
                + ", alarms=" + count(
                "SELECT COUNT(*) FROM alarm"
        );
    }

    private ManagementClient managementClient() {
        return ManagementClient.INSTANCE;
    }

    private record MapRow(
            String status,
            int occurrenceCount) {
    }

    private static final class CountingAlarmService
            implements AlarmService {

        private final AlarmService delegate;

        private final AtomicInteger calls = new AtomicInteger();

        private final AtomicInteger transientFailuresRemaining =
                new AtomicInteger();

        private final BlockingQueue<AlarmProcessResponse> responses =
                new LinkedBlockingQueue<>();

        private final BlockingQueue<TransientAlarmException>
                transientFailures = new LinkedBlockingQueue<>();

        private CountingAlarmService(AlarmService delegate) {
            this.delegate = delegate;
        }

        @Override
        public AlarmProcessResponse processEvent(
                AlarmEventCommand command) {

            calls.incrementAndGet();

            if (transientFailuresRemaining.getAndUpdate(
                    current -> current > 0 ? current - 1 : 0
            ) > 0) {
                TransientAlarmException failure =
                        new TransientAlarmException(
                                "Simulated transient AlarmService failure"
                        );
                transientFailures.add(failure);
                throw failure;
            }

            AlarmProcessResponse response =
                    delegate.processEvent(command);

            responses.add(response);
            return response;
        }

        @Override
        public com.smartfactory.common.response.PageResult<Alarm> findPage(
                com.smartfactory.dto.AlarmQueryRequest request) {
            return delegate.findPage(request);
        }

        @Override
        public Alarm findById(Long id) {
            return delegate.findById(id);
        }

        @Override
        public Alarm acknowledge(Long id, String username) {
            return delegate.acknowledge(id, username);
        }

        @Override
        public Alarm resolve(Long id) {
            return delegate.resolve(id);
        }

        private int callCount() {
            return calls.get();
        }

        private int responseCount() {
            return responses.size();
        }

        private int transientFailureCount() {
            return transientFailures.size();
        }

        private void failNext(int count) {
            transientFailuresRemaining.set(count);
        }

        private List<AlarmProcessResponse> responses() {

            List<AlarmProcessResponse> result =
                    new java.util.ArrayList<>();

            responses.drainTo(result);
            return result;
        }

        private void reset() {
            calls.set(0);
            transientFailuresRemaining.set(0);
            responses.clear();
            transientFailures.clear();
        }
    }

    private static final class ManagementClient {

        private static final ManagementClient INSTANCE =
                new ManagementClient();

        private static final ObjectMapper OBJECT_MAPPER =
                new ObjectMapper();

        private final HttpClient httpClient = HttpClient.newHttpClient();

        private final String baseUrl =
                System.getProperty(
                        "rabbitmq.it.management-url",
                        "http://localhost:15672/api"
                );

        private void ensureVhost() {

            deleteVhost();

            request(
                    "PUT",
                    "/vhosts/" + TEST_VHOST,
                    null,
                    201,
                    204
            );

            request(
                    "PUT",
                    "/permissions/"
                            + TEST_VHOST
                            + "/"
                            + RABBITMQ_USERNAME,
                    """
                    {
                      "configure": ".*",
                      "write": ".*",
                      "read": ".*"
                    }
                    """,
                    201,
                    204
            );
        }

        private void deleteVhost() {

            request(
                    "DELETE",
                    "/vhosts/" + TEST_VHOST,
                    null,
                    204,
                    404
            );
        }

        private JsonNode getExchange(String exchangeName)
        {

            return get("/exchanges/"
                    + TEST_VHOST
                    + "/"
                    + exchangeName);
        }

        private JsonNode getQueue(String queueName)
        {

            return get("/queues/"
                    + TEST_VHOST
                    + "/"
                    + queueName);
        }

        private JsonNode getBindings(
                String exchangeName,
                String queueName) {

            return get("/bindings/"
                    + TEST_VHOST
                    + "/e/"
                    + exchangeName
                    + "/q/"
                    + queueName);
        }

        private JsonNode get(String path) {

            try {
                HttpResponse<String> response = request(
                        "GET",
                        path,
                        null,
                        200
                );

                return OBJECT_MAPPER.readTree(response.body());
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Failed to read RabbitMQ management response: "
                                + path,
                        e
                );
            }
        }

        private HttpResponse<String> request(
                String method,
                String path,
                String body,
                int... allowedStatuses) {

            try {
                HttpRequest.Builder builder =
                        HttpRequest.newBuilder()
                                .uri(URI.create(baseUrl + path))
                                .header(
                                        "Authorization",
                                        basicAuthorization()
                                );

                if ("PUT".equals(method)) {
                    builder.header(
                            "Content-Type",
                            "application/json"
                    );
                    builder.PUT(HttpRequest.BodyPublishers.ofString(
                            body == null ? "" : body
                    ));
                } else if ("DELETE".equals(method)) {
                    builder.DELETE();
                } else {
                    builder.GET();
                }

                HttpResponse<String> response = httpClient.send(
                        builder.build(),
                        HttpResponse.BodyHandlers.ofString()
                );

                for (int allowedStatus : allowedStatuses) {
                    if (response.statusCode() == allowedStatus) {
                        return response;
                    }
                }

                throw new IllegalStateException(
                        "RabbitMQ management request failed: "
                                + method
                                + " "
                                + path
                                + ", status="
                                + response.statusCode()
                                + ", body="
                                + response.body()
                );
            } catch (Exception e) {
                if (e instanceof IllegalStateException) {
                    throw (IllegalStateException) e;
                }

                throw new IllegalStateException(
                        "RabbitMQ management request failed: "
                                + method
                                + " "
                                + path,
                        e
                );
            }
        }

        private String basicAuthorization() {

            String credentials =
                    RABBITMQ_USERNAME + ":" + RABBITMQ_PASSWORD;

            return "Basic " + Base64.getEncoder().encodeToString(
                    credentials.getBytes(StandardCharsets.UTF_8)
            );
        }
    }

    @Configuration
    @EnableRabbit
    @EnableTransactionManagement(proxyTargetClass = true)
    @MapperScan("com.smartfactory.mapper")
    @Import({
            RabbitMQConfig.class,
            RabbitMQMessageConfig.class,
            RabbitMQPublisherConfig.class
    })
    static class TestConfiguration {

        @Bean
        DataSource dataSource() {

            MysqlDataSource dataSource = new MysqlDataSource();
            dataSource.setUrl(MYSQL_TEST_URL);
            dataSource.setUser(MYSQL_USERNAME);
            dataSource.setPassword(MYSQL_PASSWORD);
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
        ConnectionFactory rabbitConnectionFactory()
                throws Exception {

            ManagementClient.INSTANCE.ensureVhost();

            CachingConnectionFactory connectionFactory =
                    new CachingConnectionFactory();
            connectionFactory.setHost(
                    System.getProperty(
                            "rabbitmq.it.host",
                            "localhost"
                    )
            );
            connectionFactory.setPort(Integer.parseInt(
                    System.getProperty(
                            "rabbitmq.it.port",
                            "5672"
                    )
            ));
            connectionFactory.setUsername(RABBITMQ_USERNAME);
            connectionFactory.setPassword(RABBITMQ_PASSWORD);
            connectionFactory.setVirtualHost(TEST_VHOST);
            connectionFactory.setPublisherConfirmType(
                    CachingConnectionFactory.ConfirmType.CORRELATED
            );
            connectionFactory.setPublisherReturns(true);
            return connectionFactory;
        }

        @Bean
        RabbitAdmin rabbitAdmin(
                ConnectionFactory rabbitConnectionFactory) {

            RabbitAdmin rabbitAdmin =
                    new RabbitAdmin(rabbitConnectionFactory);
            rabbitAdmin.setAutoStartup(false);
            return rabbitAdmin;
        }

        @Bean
        RabbitTemplate rabbitTemplate(
                ConnectionFactory rabbitConnectionFactory,
                JacksonJsonMessageConverter converter,
                RabbitTemplateCustomizer publisherCustomizer) {

            RabbitTemplate rabbitTemplate =
                    new RabbitTemplate(rabbitConnectionFactory);
            rabbitTemplate.setMessageConverter(converter);
            publisherCustomizer.customize(rabbitTemplate);
            return rabbitTemplate;
        }

        @Bean
        BlockingQueue<Throwable> listenerErrors() {
            return new LinkedBlockingQueue<>();
        }

        @Bean
        SimpleRabbitListenerContainerFactory
        alarmRabbitListenerContainerFactory(
                ConnectionFactory rabbitConnectionFactory,
                JacksonJsonMessageConverter converter,
                BlockingQueue<Throwable> listenerErrors) {

            SimpleRabbitListenerContainerFactory factory =
                    new SimpleRabbitListenerContainerFactory();
            factory.setConnectionFactory(rabbitConnectionFactory);
            factory.setMessageConverter(converter);
            factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
            factory.setAutoStartup(false);
            factory.setDefaultRequeueRejected(false);
            factory.setErrorHandler(listenerErrors::add);
            factory.setAdviceChain(
                    RetryInterceptorBuilder
                            .stateless()
                            .configureRetryPolicy(retryPolicy ->
                                    retryPolicy
                                            .maxRetries(2)
                                            .includes(
                                                    TransientAlarmException.class
                                            )
                                            .excludes(
                                                    BusinessException.class
                                            )
                            )
                            .backOffOptions(100, 1.0, 300)
                            .recoverer(
                                    new RejectAndDontRequeueRecoverer()
                            )
                            .build()
            );
            return factory;
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

        @Bean
        OutboxPublisher outboxPublisher(
                OutboxEventMapper outboxEventMapper,
                RabbitTemplate rabbitTemplate,
                JsonMapper jsonMapper) {

            return new OutboxPublisher(
                    outboxEventMapper,
                    rabbitTemplate,
                    jsonMapper
            );
        }

        @Bean
        CountingAlarmService countingAlarmService(
                @Qualifier("alarmService")
                AlarmService alarmService) {

            return new CountingAlarmService(alarmService);
        }

        @Bean
        DeviceAlarmEventProducer deviceAlarmEventProducer(
                RabbitTemplate rabbitTemplate) {

            return new DeviceAlarmEventProducer(rabbitTemplate);
        }

        @Bean
        DeviceAlarmEventConsumer deviceAlarmEventConsumer(
                @Qualifier("countingAlarmService")
                AlarmService alarmService) {

            return new DeviceAlarmEventConsumer(alarmService);
        }
    }
}
