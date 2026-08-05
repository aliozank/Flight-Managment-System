package com.alikaracor.learning.flightservice.publisher;

import com.alikaracor.learning.flightservice.dto.FlightResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightWebSocketPublisherTest {

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @InjectMocks
    private FlightWebSocketPublisher flightWebSocketPublisher;

    private FlightResponse sampleResponse;

    @BeforeEach
    void setUp() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.initSynchronization();
        }

        sampleResponse = new FlightResponse();
        sampleResponse.setFlightId(10L);
        sampleResponse.setFlightNumber("TK1234");
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("publish - Transaction commit edilmeden önce SimpMessagingTemplate.convertAndSend çağrılmamalıdır")
    void publish_shouldNotSendMessage_beforeTransactionCommit() {
        flightWebSocketPublisher.publish(sampleResponse);

        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);
        verifyNoInteractions(simpMessagingTemplate);
    }

    @Test
    @DisplayName("publish - afterCommit çalıştığında mesaj /topic/flights adresine doğru FlightResponse ile gönderilmelidir")
    void publish_shouldSendMessageToTopic_afterTransactionCommit() {
        flightWebSocketPublisher.publish(sampleResponse);

        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);

        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }

        verify(simpMessagingTemplate).convertAndSend("/topic/flights", sampleResponse);
    }
}
