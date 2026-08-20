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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
class OutboxIntegrationTest {

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
    @DisplayName("Senaryo 1: Uçuş oluşturulduğunda outbox kaydı PENDING oluşmalı, job çalışınca PUBLISHED olmalıdır")
    void createFlight_shouldCreateOutboxEventAndPublishSuccessfully() {
        FlightCreateRequest request = new FlightCreateRequest();
        request.setFlightNumber("TK1923");
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

        CompletableFuture<SendResult<String, String>> kafkaFuture = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(kafkaFuture);

        FlightResponse response = flightService.addFlight(request, 100L, "127.0.0.1");

        assertThat(flightRepository.count()).isEqualTo(1);
        List<OutboxEvent> pendingEvents = outboxEventRepository.findAll();
        assertThat(pendingEvents).hasSize(1);

        OutboxEvent pendingEvent = pendingEvents.get(0);
        assertThat(pendingEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(pendingEvent.getAggregateId()).isEqualTo(response.getFlightId().toString());
        assertThat(pendingEvent.getTopicName()).isEqualTo("flight.events");

        jdbcTemplate.execute("DELETE FROM shedlock");
        outboxPublisherJob.publishOutboxEvents();

        OutboxEvent publishedEvent = outboxEventRepository.findById(pendingEvent.getOutboxId()).orElseThrow();
        assertThat(publishedEvent.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(publishedEvent.getPublishedAt()).isNotNull();
        verify(kafkaTemplate, times(1)).send(eq("flight.events"), eq(response.getFlightId().toString()), anyString());
    }

    @Test
    @DisplayName("Senaryo 2: İşlem sırasında hata oluşursa hem uçuş hem de outbox kaydı ROLLBACK olmalıdır")
    void createFlight_shouldRollbackFlightAndOutbox_whenExceptionOccurs() {
        doThrow(new IllegalArgumentException("Havalimanı doğrulama hatası"))
                .when(flightReferenceValidationService).validateCreateRequest(any());

        FlightCreateRequest request = new FlightCreateRequest();
        request.setFlightNumber("TK9999");
        request.setAirlineId(1L);
        request.setFlightDate(LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> flightService.addFlight(request, 100L, "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(flightRepository.count()).isEqualTo(0);
        assertThat(outboxEventRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Aynı outbox kaydı eş zamanlı olarak yalnızca bir publisher tarafından claim edilebilmelidir")
    void claimEvent_shouldGrantOwnershipOnlyOnce() {
        OutboxEvent event = new OutboxEvent();
        event.setOutboxId("claim-outbox-1");
        event.setEventId("claim-event-1");
        event.setAggregateType("FLIGHT");
        event.setAggregateId("42");
        event.setEventType("CREATED");
        event.setTopicName("flight.events");
        event.setPayload("{\"flightId\":42}");
        event.setStatus(OutboxStatus.PENDING);
        event.setCreatedAt(Instant.now());
        outboxEventRepository.saveAndFlush(event);

        Instant now = Instant.now();
        int firstClaim = outboxEventRepository.claimEvent(
                event.getOutboxId(),
                "publisher-1",
                now.plusSeconds(30),
                now,
                OutboxStatus.PENDING,
                OutboxStatus.FAILED,
                OutboxStatus.PROCESSING
        );
        int secondClaim = outboxEventRepository.claimEvent(
                event.getOutboxId(),
                "publisher-2",
                now.plusSeconds(30),
                now,
                OutboxStatus.PENDING,
                OutboxStatus.FAILED,
                OutboxStatus.PROCESSING
        );

        assertThat(firstClaim).isEqualTo(1);
        assertThat(secondClaim).isZero();
        assertThat(outboxEventRepository.markPublished(
                event.getOutboxId(),
                "publisher-2",
                Instant.now(),
                OutboxStatus.PROCESSING,
                OutboxStatus.PUBLISHED
        )).isZero();
        assertThat(outboxEventRepository.markPublished(
                event.getOutboxId(),
                "publisher-1",
                Instant.now(),
                OutboxStatus.PROCESSING,
                OutboxStatus.PUBLISHED
        )).isEqualTo(1);

        OutboxEvent publishedEvent = outboxEventRepository.findById(event.getOutboxId()).orElseThrow();
        assertThat(publishedEvent.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(publishedEvent.getLockToken()).isNull();
        assertThat(publishedEvent.getLockedUntil()).isNull();
    }
}
