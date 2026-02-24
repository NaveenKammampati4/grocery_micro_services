package com.grocery.api_gateway.filter;

import com.grocery.api_gateway.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
@Slf4j
@Component
@Order(-100) // Runs FIRST
public class JwtAuthenticationGatewayFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;

    public JwtAuthenticationGatewayFilter(JwtUtil jwtUtil, RedisTemplate<String, String> redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        if (isPublicPath(path)){
            return chain.filter(exchange);
        }

//       String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String token = extractToken(request);
        if (token==null || !token.startsWith("Bearer ")){
            return unauthorized(exchange);
        }

        try {

            if (!jwtUtil.validateToken(token) || isTokenBlocked(token)){
                return unauthorized(exchange);
            }
            String userId = jwtUtil.getUserIdFromToken(token);
//            Claims claims = jwtUtil.getClaims(token);
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("userId", userId)
                    .header("roles", String.join(",", jwtUtil.getRoles(token)))
                    .build();

//            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
//                    .header("userId", claims.getSubject())
//                    .header("role", claims.get("role", String.class))
//                    .build();

            log.info("User {} authenticated for path: {}", userId, path);
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception e) {
            log.warn("JWT validation failed for path {}: {}", path, e.getMessage());
            return unauthorized(exchange);
        }

    }

    private boolean isTokenBlocked(String token) {
        try {
            // Extract JTI (JWT ID) or use token hash for blacklist key
            String jti = jwtUtil.getJtiFromToken(token);
            if (jti==null){
                // Fallback: hash token (production safe)
                jti = DigestUtils.sha256Hex(token);

            }

            // Redis key: "blacklist:jti:{jti}" expires with token
            String redisKey = "blacklist:jti:" + jti;
            Boolean isBlocked = redisTemplate.hasKey(redisKey);

            return Boolean.TRUE.equals(isBlocked);
        } catch (Exception e) {
            log.warn("Redis blacklist check failed: {}", e.getMessage());
            return false;  // Fail open (don't block if Redis down)
        }
    }

    private String extractToken(ServerHttpRequest request) {
        List<String> authHeaders=request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (authHeaders!=null && !authHeaders.isEmpty()){
            String authHeader = authHeaders.get(0);
            if (authHeader.startsWith("Bearer ")){
                return authHeader.substring(7);
            }
        }
        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }


    private boolean isPublicPath(String path) {
        return path.startsWith("/api/auth/login") ||
                path.startsWith("/api/auth/register") ||
                path.startsWith("/api/public/");
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
