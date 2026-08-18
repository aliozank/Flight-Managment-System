package com.alikaracor.learning.referencemanager.publisher;

import com.alikaracor.learning.referencemanager.event.ReferenceEvent;
import com.alikaracor.learning.referencemanager.event.ReferenceEventType;
import com.alikaracor.learning.referencemanager.event.ReferenceResourceType;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@SpringJUnitConfig(ReferenceEventKafkaIntegrationTest.TestConfig.class)
@EmbeddedKafka(partitions = 1, topics = "reference.events")
class ReferenceEventKafkaIntegrationTest {

    @Configuration
    static class TestConfig {
    }

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    void shouldPublishAndConsumeReferenceEventAsJson() {
        Map<String, Object> producerProperties = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        DefaultKafkaProducerFactory<String, ReferenceEvent> producerFactory =
                new DefaultKafkaProducerFactory<>(
                        producerProperties,
                        new StringSerializer(),
                        new JacksonJsonSerializer<>()
                );
        KafkaTemplate<String, ReferenceEvent> kafkaTemplate = new KafkaTemplate<>(producerFactory);

        Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps(
                embeddedKafkaBroker,
                "reference-event-integration-test",
                true
        );
        JacksonJsonDeserializer<ReferenceEvent> jsonDeserializer =
                new JacksonJsonDeserializer<>(ReferenceEvent.class, false);
        DefaultKafkaConsumerFactory<String, ReferenceEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(
                        consumerProperties,
                        new StringDeserializer(),
                        jsonDeserializer
                );

        try (
                Consumer<String, ReferenceEvent> consumer = consumerFactory.createConsumer()
        ) {
            embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, "reference.events");

            ReferenceEvent expected = new ReferenceEvent();
            expected.setEventId(UUID.randomUUID());
            expected.setEventType(ReferenceEventType.UPDATED);
            expected.setResourceType(ReferenceResourceType.AIRPORT);
            expected.setResourceId(42L);
            expected.setOccurredAt(Instant.now());

            com.alikaracor.learning.referencemanager.service.OutboxService outboxService = org.mockito.Mockito.mock(com.alikaracor.learning.referencemanager.service.OutboxService.class);
            org.mockito.Mockito.doAnswer(invocation -> {
                kafkaTemplate.send("reference.events", expected);
                return null;
            }).when(outboxService).saveReferenceEvent(any());

            ReferenceEventPublisher publisher = new ReferenceEventPublisher(outboxService);
            publisher.publish(expected);
            kafkaTemplate.flush();

            ConsumerRecord<String, ReferenceEvent> record = KafkaTestUtils.getSingleRecord(
                    consumer,
                    "reference.events",
                    Duration.ofSeconds(10)
            );
            ReferenceEvent actual = record.value();

            assertEquals(expected.getEventId(), actual.getEventId());
            assertEquals(expected.getEventType(), actual.getEventType());
            assertEquals(expected.getResourceType(), actual.getResourceType());
            assertEquals(expected.getResourceId(), actual.getResourceId());
            assertEquals(expected.getOccurredAt(), actual.getOccurredAt());
        } finally {
            producerFactory.destroy();
        }
    }
}
