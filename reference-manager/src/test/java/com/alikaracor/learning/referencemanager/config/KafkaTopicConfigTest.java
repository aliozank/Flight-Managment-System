package com.alikaracor.learning.referencemanager.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaTopicConfigTest {

    @Test
    void shouldCreateReferenceEventsTopicDefinition() {
        KafkaTopicConfig config = new KafkaTopicConfig();

        NewTopic topic = config.referenceEventsTopic();

        assertEquals("reference.events", topic.name());
        assertEquals(1, topic.numPartitions());
        assertEquals((short) 1, topic.replicationFactor());
    }
}
