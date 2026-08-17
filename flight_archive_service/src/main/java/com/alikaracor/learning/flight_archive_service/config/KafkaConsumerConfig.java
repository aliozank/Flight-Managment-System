package com.alikaracor.learning.flight_archive_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public NewTopic flightEventsDeadLetterTopic(@Value("${app.kafka.flight-events-dlt-topic:flight.events.DLT}") String deadLetterTopic) {

        return TopicBuilder.name(deadLetterTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> kafkaOperations, @Value("${app.kafka.flight-events-dlt-topic:flight.events.DLT}") String deadLetterTopic) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaOperations, (record, exception) -> new TopicPartition(deadLetterTopic, record.partition()));

        recoverer.setFailIfSendResultIsError(true);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));

        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);

        return errorHandler;

    }
}
