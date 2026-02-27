package com.grocery.auth_service.security;

import com.grocery.auth_service.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtils {

    private static final Logger logger= LoggerFactory.getLogger(JwtUtils.class);

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs}")
    private int jwtExpirationMs;

    @Value("${app.jwtRefreshExpirationMs}")
    private int jwtRefreshExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init(){
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        logger.info("JWT Key initialized successfully");
    }

    public String generateJwtToken(Authentication authentication){
        UserDetailsImpl userPrincipal= (UserDetailsImpl) authentication.getPrincipal();
        return Jwts.builder()
                .subject(String.valueOf(userPrincipal.getId()))
                .claim("roles",userPrincipal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Long userId){
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime()+jwtRefreshExpirationMs))
                .signWith(key)
                .compact();
    }

    public String getEmailFromRefreshToken(String token){
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateJwtToken(String authToken){
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(authToken);
            return true;
        } catch (SignatureException | MalformedJwtException e) {
            logger.error("Invalid JWT signature/malformed: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token expired: {}", e.getMessage());
        } catch (UnsupportedJwtException | IllegalArgumentException e) {
            logger.error("JWT token unsupported/illegal: {}", e.getMessage());
        }
        return false;
    }

    public String generateJwtTokenFromUser(User user){
        UserDetails userDetails = new UserDetailsImpl(user);
        return generateJwtToken(new UsernamePasswordAuthenticationToken(
                userDetails,null, userDetails.getAuthorities()
        ));
    }

    public String generateTokenFromEmail(String email){
        return Jwts.builder()
                .subject(email)
                .claim("roles", List.of("ROLE_USER"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key)
                .compact();
    }

//    public String generateJwtTokenFromUser(User user){
//
//        return Jwts.builder()
//                .subject(String.valueOf(user.getId()))
//                .claim("email", user.getEmail())
//                .claim("role", user.getRole().getAuthority())
//                .claim("enabled", user.isEnabled())
//                .issuedAt(new Date())
//                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
//                .signWith(key)
//                .compact();
//    }

}
