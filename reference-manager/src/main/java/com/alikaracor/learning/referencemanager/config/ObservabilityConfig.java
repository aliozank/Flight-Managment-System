package com.alikaracor.learning.referencemanager.config;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationProperties;
import org.springframework.boot.micrometer.tracing.autoconfigure.TracingProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationConvention;
import org.springframework.web.filter.ServerHttpObservationFilter;

import java.util.List;

@Configuration
public class ObservabilityConfig {

    @Bean
    public FilterRegistrationBean<ServerHttpObservationFilter> actuatorAwareObservationFilter(
            ObservationRegistry observationRegistry,
            ObjectProvider<ServerRequestObservationConvention> customConvention,
            ObservationProperties observationProperties
    ) {
        String observationName = observationProperties.getHttp().getServer().getRequests().getName();
        ServerRequestObservationConvention convention = customConvention.getIfAvailable(
                () -> new DefaultServerRequestObservationConvention(observationName)
        );

        ServerHttpObservationFilter filter = new ServerHttpObservationFilter(observationRegistry, convention) {
            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                return isActuatorPath(request.getRequestURI());
            }
        };

        FilterRegistrationBean<ServerHttpObservationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC);
        return registration;
    }

    @Bean
    public Sampler actuatorFilteringSampler(TracingProperties tracingProperties) {
        Sampler delegate = Sampler.parentBased(
                Sampler.traceIdRatioBased(tracingProperties.getSampling().getProbability())
        );

        return new Sampler() {
            @Override
            public SamplingResult shouldSample(
                    Context parentContext,
                    String traceId,
                    String name,
                    SpanKind spanKind,
                    Attributes attributes,
                    List<LinkData> parentLinks
            ) {
                boolean rootSpan = !Span.fromContext(parentContext).getSpanContext().isValid();
                boolean rootInfrastructureSpan = rootSpan
                        && (spanKind == SpanKind.CLIENT || spanKind == SpanKind.INTERNAL);

                if (rootInfrastructureSpan) {
                    return SamplingResult.drop();
                }

                return delegate.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
            }

            @Override
            public String getDescription() {
                return "ActuatorFilteringSampler{" + delegate.getDescription() + "}";
            }
        };
    }

    boolean isActuatorPath(String requestPath) {
        return "/actuator".equals(requestPath) ||
                (requestPath != null && requestPath.startsWith("/actuator/"));
    }
}
