package com.grocery.api_gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class UserKeyResolver implements KeyResolver {

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        return Mono.justOrEmpty(extractUserId(exchange))
                .switchIfEmpty(Mono.just("anonymous"));
    }

    private String extractUserId(ServerWebExchange exchange) {
        return exchange.getRequest().getHeaders().getFirst("userId");
    }
}
