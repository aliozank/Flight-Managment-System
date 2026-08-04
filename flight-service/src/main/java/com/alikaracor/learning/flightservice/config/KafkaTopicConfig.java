package com.alikaracor.learning.flightservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic flightEventTopic() {

        return TopicBuilder
                .name("flight.events")
                .partitions(3)
                .replicas(1)
                .build();

    }
}
