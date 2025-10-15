package com.celcoin.disbursement.integration;

import com.celcoin.disbursement.model.dto.CreditParty;
import com.celcoin.disbursement.model.dto.DisbursementDto;
import com.celcoin.disbursement.model.dto.DisbursementRequest;
import com.celcoin.disbursement.model.dto.DisbursementStepRequest;
import com.celcoin.disbursement.model.dto.Schedule;
import com.celcoin.disbursement.model.utils.ScheduleType;
import com.celcoin.disbursement.model.utils.StepType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseIntegrationTest {


    static final MySQLContainer<?> mySQLContainer;
    static final KafkaContainer kafkaContainer;

    static {
        mySQLContainer = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.26"))
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpass");

        kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.0.1"));
        mySQLContainer.start();
        kafkaContainer.start();
    }

    @DynamicPropertySource
    static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        // MySQL
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    protected DisbursementRequest createDisbursementRequest(String clientCode, ScheduleType scheduleType, LocalDateTime date, StepType stepType) {
        CreditParty creditParty = CreditParty.builder().name("Test Receiver").taxId("11122233344").build();
        DisbursementStepRequest stepRequest = new DisbursementStepRequest(new BigDecimal("123.45"), creditParty, null);
        DisbursementDto disbursementDto = new DisbursementDto(stepType, stepRequest);
        Schedule schedule = new Schedule(scheduleType, date, null);
        return new DisbursementRequest(clientCode, schedule, List.of(disbursementDto));
    }
}
