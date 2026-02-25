package com.grocery.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;


// Adds unique ID to each request (Distributed tracing, Log tracking across services, Debugging production issues)
// All logs share same ID, Easy tracing
@Component
@Order(-2)
public class CorrelationIdFilter implements GlobalFilter {

    private static final String CORRELATION_ID = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String existingId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID);
        String correlationId = existingId != null ? existingId : UUID.randomUUID().toString();

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .header(CORRELATION_ID, correlationId)
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }
}
