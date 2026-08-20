package com.alikaracor.learning.flightservice.config;

import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaProducerSerializerTest {

    @Test
    void configuredValueSerializerShouldSendStoredJsonWithoutDoubleSerialization() throws Exception {
        Properties properties = new Properties();
        try (InputStream input = getClass().getResourceAsStream("/application.properties")) {
            assertThat(input).isNotNull();
            properties.load(input);
        }

        String serializerClassName = properties.getProperty("spring.kafka.producer.value-serializer");
        Object configuredSerializer = Class.forName(serializerClassName).getDeclaredConstructor().newInstance();

        assertThat(configuredSerializer).isInstanceOf(StringSerializer.class);

        String payload = "{\"flightId\":42}";
        byte[] serialized = ((StringSerializer) configuredSerializer).serialize("flight.events", payload);

        assertThat(new String(serialized, StandardCharsets.UTF_8)).isEqualTo(payload);
    }
}
