//package com.grocery.api_gateway.config;
//
//import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory;
//import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
//import org.springframework.cloud.gateway.route.RouteLocator;
//import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class GatewayConfig {
//
//    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, RedisRateLimiter redisRateLimiter){
//        return builder.routes()
//                // Auth Service
//                .route("auth-service", r -> r.path("/api/auth/**")
//                        .filters(f -> f
//                                .filter(new RequestRateLimiterGatewayFilterFactory().apply(
//                                        RequestRateLimiterGatewayFilterFactory.Config
//                                                .from
//                                ))
//                        )
//                )
//    }
//}
