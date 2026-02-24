package com.grocery.auth_service.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class CookieService {

    private static final String ACCESS_TOKEN_COOKIE="accessToken";
    private static final String REFRESH_TOKEN_COOKIE="refreshToken";
    private static final int ACCESS_TOKEN_MAX_AGE=15 * 60;
    private static final int REFRESH_TOKEN_MAX_AGE= 7 * 24 * 60 * 60;
    private static final boolean SECURE = true;

    public void setAccessTokenCookie(HttpServletResponse response,String token){
        addCookie(response, ACCESS_TOKEN_COOKIE, token, ACCESS_TOKEN_MAX_AGE);
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String token){
        addCookie(response, REFRESH_TOKEN_COOKIE, token, REFRESH_TOKEN_MAX_AGE);
    }

    public void clearAccessTokenCookie(HttpServletResponse response){
        deleteCookie(response, ACCESS_TOKEN_COOKIE);
    }

    public void clearRefreshTokenCookie(HttpServletResponse response){
        deleteCookie(response, REFRESH_TOKEN_COOKIE);
    }

    public String getCookieValue(HttpServletRequest request, String cookieName){
        if (request.getCookies()==null){
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }


    private void addCookie(HttpServletResponse response, String accessTokenCookie, String token, int accessTokenMaxAge) {
        ResponseCookie cookie=ResponseCookie.from(accessTokenCookie, token)
                .httpOnly(true)
                .secure(SECURE)
                .path("/")
                .maxAge(accessTokenMaxAge)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void deleteCookie(HttpServletResponse response, String refreshTokenCookie) {
        ResponseCookie cookie=ResponseCookie.from(refreshTokenCookie,"")
                .httpOnly(true)
                .secure(SECURE)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

}
