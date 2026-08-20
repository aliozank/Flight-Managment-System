package com.alikaracor.learning.flightservice.outbox;

import com.alikaracor.learning.flightservice.client.dto.AirportReferenceResponse;
import com.alikaracor.learning.flightservice.dto.FlightCreateRequest;
import com.alikaracor.learning.flightservice.dto.FlightResponse;
import com.alikaracor.learning.flightservice.model.OutboxEvent;
import com.alikaracor.learning.flightservice.model.OutboxStatus;
import com.alikaracor.learning.flightservice.repository.FlightRepository;
import com.alikaracor.learning.flightservice.repository.OutboxEventRepository;
import com.alikaracor.learning.flightservice.scheduler.OutboxPublisherJob;
import com.alikaracor.learning.flightservice.service.AircraftScheduleConflictValidationService;
import com.alikaracor.learning.flightservice.service.FlightReferenceValidationService;
import com.alikaracor.learning.flightservice.service.FlightService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
    "spring.cache.type=none",
    "spring.kafka.admin.auto-create=false",
    "spring.kafka.listener.auto-startup=false",
    "app.outbox.publisher-fixed-delay=1h",
    "ADMIN_USERNAME=admin",
    "ADMIN_EMAIL=admin@flight.com",
    "ADMIN_PASSWORD=adminpass"
})
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OutboxChaosIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    @Autowired
    private FlightService flightService;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxPublisherJob outboxPublisherJob;

    @MockitoBean
    private FlightReferenceValidationService flightReferenceValidationService;

    @MockitoBean
    private AircraftScheduleConflictValidationService aircraftScheduleConflictValidationService;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockitoBean
    private net.javacrumbs.shedlock.core.LockProvider lockProvider;

    @MockitoBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @MockitoBean
    private com.alikaracor.learning.flightservice.config.JwtConfig jwtConfig;

    @MockitoBean
    private org.springframework.security.oauth2.jwt.JwtEncoder jwtEncoder;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        lenient().when(lockProvider.lock(any())).thenReturn(java.util.Optional.of(mock(net.javacrumbs.shedlock.core.SimpleLock.class)));
        outboxEventRepository.deleteAll();
        flightRepository.deleteAll();

        doNothing().when(flightReferenceValidationService).validateCreateRequest(any());

        AirportReferenceResponse dummyAirport = new AirportReferenceResponse();
        dummyAirport.setAirportId(1L);
        dummyAirport.setAirportTimezone("UTC");
        lenient().when(flightReferenceValidationService.validateAirport(any())).thenReturn(dummyAirport);

        doNothing().when(aircraftScheduleConflictValidationService).validateAircraftScheduleForCreate(any(), any(), any());
    }

    @Test
    @DisplayName("KAOS TESTİ 1: Kafka çöktüğünde mesaj FAILED olmalı, Kafka iyileştiğinde sonraki job ile PUBLISHED olmalıdır")
    void simulateKafkaFailure_andRecovery() {
        // 1. Uçuş eklenir (Outbox'a PENDING kaydolur)
        FlightCreateRequest request = new FlightCreateRequest();
        request.setFlightNumber("TK8888");
        request.setAirlineId(1L);
        request.setAircraftId(2L);
        request.setAircraftTypeId(3L);
        request.setOriginAirportId(4L);
        request.setDestinationAirportId(5L);
        request.setFlightTypeId(6L);
        request.setFlightDate(LocalDate.now().plusDays(1));
        request.setScheduledDepartureTime(LocalTime.of(10, 0));
        request.setScheduledArrivalDate(LocalDate.now().plusDays(1));
        request.setScheduledArrivalTime(LocalTime.of(12, 0));

        // 🛑 KAOS ANI: Kafka çöktü! (TimeoutException)
        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new TimeoutException("KAOS: Kafka sunucusu kapalı / ulaşılamıyor!"));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(failedFuture);

        FlightResponse response = flightService.addFlight(request, 100L, "127.0.0.1");

        // 2. Outbox Job çalıştırılır (Kafka kapalıyken)
        outboxPublisherJob.publishOutboxEvents();

        // 🔍 KAOS DOĞRULAMASI: Mesaj kaybolmadı, FAILED oldu, attemptCount 1 ve lastError doldu!
        OutboxEvent failedEvent = outboxEventRepository.findAll().get(0);
        assertThat(failedEvent.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(failedEvent.getAttemptCount()).isEqualTo(1);
        assertThat(failedEvent.getLastError()).contains("KAOS: Kafka sunucusu kapalı");
        assertThat(failedEvent.getNextAttemptAt()).isNotNull();

        // 🟢 İYİLEŞME (RECOVERY) ANI: Kafka ayağa kalktı!
        CompletableFuture<SendResult<String, String>> successFuture = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(successFuture);

        // Deneme zamanını simülasyon için şimdiki zamana çekeriz
        failedEvent.setNextAttemptAt(Instant.now().minusSeconds(1));
        outboxEventRepository.saveAndFlush(failedEvent);

        // 3. Outbox Job tekrar çalıştırılır (Kafka iyileştiğinde)
        outboxPublisherJob.publishOutboxEvents();

        // 🔍 RECOVERY DOĞRULAMASI: Mesaj başarıyla PUBLISHED durumuna geçti!
        OutboxEvent recoveredEvent = outboxEventRepository.findById(failedEvent.getOutboxId()).orElseThrow();
        assertThat(recoveredEvent.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(recoveredEvent.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("KAOS TESTİ 2: Kafka 5 kez hatada kaldığında outbox kaydı dondurulmalı (FAILED & nextAttemptAt = null)")
    void simulateMaxRetriesReached_shouldFreezeEvent() {
        OutboxEvent event = new OutboxEvent();
        event.setOutboxId("chaos-outbox-1");
        event.setEventId("chaos-event-1");
        event.setAggregateType("FLIGHT");
        event.setAggregateId("99");
        event.setEventType("CREATED");
        event.setTopicName("flight.events");
        event.setPayload("{\"flightId\":99}");
        event.setStatus(OutboxStatus.FAILED);
        event.setAttemptCount(4); // Zaten 4 kez denenmiş
        event.setCreatedAt(Instant.now());
        event.setNextAttemptAt(Instant.now().minusSeconds(1)); // Deneme zamanı gelmiş

        outboxEventRepository.saveAndFlush(event);

        // Kafka hala çökmüş durumda
        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new TimeoutException("KAOS: Kafka hala çökmüş durumda!"));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(failedFuture);

        // Job çalıştırılır (5. deneme)
        outboxPublisherJob.publishOutboxEvents();

        // 🔍 DOĞRULAMA: 5. denemeden sonra durum FAILED, nextAttemptAt null olmalı!
        OutboxEvent frozenEvent = outboxEventRepository.findById("chaos-outbox-1").orElseThrow();
        assertThat(frozenEvent.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(frozenEvent.getAttemptCount()).isEqualTo(5);
        assertThat(frozenEvent.getNextAttemptAt()).isNull(); // Donduruldu!
    }

    @Test
    @DisplayName("İşlem sırasında çöken publisher'ın süresi dolmuş claim'i yeniden alınabilmelidir")
    void staleProcessingClaim_shouldBeRecovered() {
        OutboxEvent event = new OutboxEvent();
        event.setOutboxId("stale-outbox-1");
        event.setEventId("stale-event-1");
        event.setAggregateType("FLIGHT");
        event.setAggregateId("77");
        event.setEventType("UPDATED");
        event.setTopicName("flight.events");
        event.setPayload("{\"flightId\":77}");
        event.setStatus(OutboxStatus.PROCESSING);
        event.setAttemptCount(0);
        event.setCreatedAt(Instant.now().minusSeconds(60));
        event.setLockToken("crashed-publisher");
        event.setLockedUntil(Instant.now().minusSeconds(1));
        outboxEventRepository.saveAndFlush(event);

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        outboxPublisherJob.publishOutboxEvents();

        OutboxEvent recoveredEvent = outboxEventRepository.findById(event.getOutboxId()).orElseThrow();
        assertThat(recoveredEvent.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(recoveredEvent.getPublishedAt()).isNotNull();
        assertThat(recoveredEvent.getLockToken()).isNull();
        assertThat(recoveredEvent.getLockedUntil()).isNull();
        verify(kafkaTemplate, times(1)).send("flight.events", "77", "{\"flightId\":77}");
    }
}
