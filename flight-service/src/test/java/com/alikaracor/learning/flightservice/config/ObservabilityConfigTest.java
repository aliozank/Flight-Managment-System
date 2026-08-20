package com.alikaracor.learning.flightservice.config;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.micrometer.tracing.autoconfigure.TracingProperties;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilityConfigTest {

    private static final String TRACE_ID = "0123456789abcdef0123456789abcdef";

    private Sampler sampler;

    @BeforeEach
    void setUp() {
        TracingProperties tracingProperties = new TracingProperties();
        tracingProperties.getSampling().setProbability(1.0f);
        sampler = new ObservabilityConfig().actuatorFilteringSampler(tracingProperties);
    }

    @Test
    void shouldRecognizeActuatorPaths() {
        ObservabilityConfig config = new ObservabilityConfig();

        assertThat(config.isActuatorPath("/actuator/health")).isTrue();
        assertThat(config.isActuatorPath("/actuator/prometheus")).isTrue();
        assertThat(config.isActuatorPath("/api/flights")).isFalse();
    }

    @Test
    void shouldKeepBusinessServerTraces() {
        assertThat(sample("/api/flights", SpanKind.SERVER)).isEqualTo(SamplingDecision.RECORD_AND_SAMPLE);
    }

    @Test
    void shouldDropOrphanClientSpans() {
        Attributes attributes = Attributes.builder().put("db.system", "redis").build();

        SamplingDecision decision = sampler.shouldSample(
                Context.root(), TRACE_ID, "info", SpanKind.CLIENT, attributes, List.of()
        ).getDecision();

        assertThat(decision).isEqualTo(SamplingDecision.DROP);
    }

    private SamplingDecision sample(String path, SpanKind spanKind) {
        Attributes attributes = Attributes.builder().put("http.url", path == null ? "" : path).build();

        return sampler.shouldSample(Context.root(), TRACE_ID, "http request", spanKind, attributes, List.of())
                .getDecision();
    }
}
