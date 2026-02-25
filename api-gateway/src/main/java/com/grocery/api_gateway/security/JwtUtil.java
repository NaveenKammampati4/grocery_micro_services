package com.grocery.api_gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    private SecretKey secretKey;

    //    private SecretKey secretKey() {
//        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
//    }
    @PostConstruct
    public void init(){
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            throw new JwtException("Invalid JWT");
        }
    }

    public String getUserIdFromToken(String token) {
        return parseClaims(token).get("userId",String.class);
    }

    public String getJtiFromToken(String token) {
        return parseClaims(token).get("jti", String.class);
    }

    public List<String> getRoles(String token) {
        List<String> roles = parseClaims(token).get("roles", List.class);
        return roles != null ? roles : List.of();
    }

}
