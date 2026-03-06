package com.grocery.auth_service.service;

import com.grocery.auth_service.util.CookieService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CookieServiceTest {

    private static final String ACCESS_TOKEN_COOKIE="accessToken";
    private static final String REFRESH_TOKEN_COOKIE="refreshToken";

    private CookieService cookieService;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp(){
        cookieService=new CookieService();
        response=new MockHttpServletResponse();
    }

    @Test
    @DisplayName("should add access token cookie with correct attributes")
    void shouldSetAccessTokenCookie(){
        cookieService.setAccessTokenCookie(response, ACCESS_TOKEN_COOKIE);
        String header = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(header);
        assertTrue(header.contains("accessToken="+ACCESS_TOKEN_COOKIE));
        assertTrue(header.contains("HttpOnly"));
        assertTrue(header.contains("Secure"));
        assertTrue(header.contains("SameSite=Strict"));
        assertTrue(header.contains("Path=/"));
    }

    @Test
    @DisplayName("should add refresh token cookie with correct attributes")
    void shouldSetRefreshTokenCookie(){
        cookieService.setRefreshTokenCookie(response,REFRESH_TOKEN_COOKIE);
        String header = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(header);
        assertTrue(header.contains("refreshToken="+REFRESH_TOKEN_COOKIE));
        assertTrue(header.contains("HttpOnly"));
        assertTrue(header.contains("Secure"));
        assertTrue(header.contains("SameSite=Strict"));
    }

    @Test
    @DisplayName("should return cookie value when cookie exists in request")
    void shouldReturnCookieValueWhenCookieExists(){
        MockHttpServletRequest request=new MockHttpServletRequest();
        request.setCookies(new Cookie("accessToken","abc123"));
        String value = cookieService.getCookieValue(request, "accessToken");
        assertEquals("abc123",value);
    }

    @Test
    @DisplayName("should return null when cookie does not exist")
    void shouldReturnNullWhenCookieDoesNotExist(){
        MockHttpServletRequest request=new MockHttpServletRequest();
        request.setCookies(new Cookie("otherCookie","value"));
        String value = cookieService.getCookieValue(request, "accessToken");
        assertNull(value);
    }

    @Test
    @DisplayName("should return null when request has no cookies")
    void shouldReturnNullWhenRequestHasNoCookies(){
        MockHttpServletRequest request=new MockHttpServletRequest();
        String value = cookieService.getCookieValue(request, "accessToken");
        assertNull(value);
    }

    @Test
    @DisplayName("should clear access token cookie")
    void shouldClearAccessTokenCookie(){
        cookieService.clearAccessTokenCookie(response);
        String header = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(header);
        assertTrue(header.contains("HttpOnly"));
        assertTrue(header.contains("Secure"));
        assertTrue(header.contains("SameSite=Strict"));
        assertTrue(header.contains("Path=/"));
    }

    @Test
    @DisplayName("should clear refresh token cookie")
    void shouldClearRefreshTokenCookie(){
       cookieService.clearRefreshTokenCookie(response);
        String header = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(header);
        assertTrue(header.contains("HttpOnly"));
        assertTrue(header.contains("Secure"));
        assertTrue(header.contains("SameSite=Strict"));
        assertTrue(header.contains("Path=/"));
    }
}
