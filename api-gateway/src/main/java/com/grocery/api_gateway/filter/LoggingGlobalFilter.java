package com.grocery.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

// Logs response time of each request (Performance monitoring, Detect slow services)
@Component
@Slf4j
public class LoggingGlobalFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();
        return chain.filter(exchange).then(
                Mono.fromRunnable(()->{
                    long time = System.currentTimeMillis() - start;
                    log.info("Request: {} took {} ms",
                            exchange.getRequest().getURI(),
                            time);
                })
        );
    }

//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        ServerHttpRequest request = exchange.getRequest();
//        log.info("Request: {} {} {} User: {}",
//                request.getMethod(),
//                request.getURI(),
//                request.getHeaders().getFirst("X-User-Id"),
//                request.getRemoteAddress());
//
//        return chain.filter(exchange)
//                .doOnSuccess(aVoid -> logResponse(exchange.getResponse()))
//                .doOnError(ex -> log.error("Request failed: {}", ex.getMessage()));
//    }
}
