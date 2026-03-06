package com.grocery.auth_service.health;


import com.grocery.auth_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class CustomHealthIndicatorTest {

    private UserRepository userRepository;
    private CustomHealthIndicator healthIndicator;

    @BeforeEach
    void setUp(){
        userRepository= Mockito.mock(UserRepository.class);
        healthIndicator=new CustomHealthIndicator(userRepository);
    }

    @Test
    @DisplayName("should return up when active users exist")
    void shouldReturnUpWhenActiveUsersExist(){
        when(userRepository.countActiveUsers()).thenReturn(5L);
        Health health = healthIndicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(5L,health.getDetails().get("activeUsers"));
        assertEquals("OK",health.getDetails().get("dbConnection"));
    }

    @Test
    @DisplayName("should return down when no active users")
    void shouldReturnDownWhenNoActiveUsers(){
        when(userRepository.countActiveUsers()).thenReturn(0L);
        Health health = healthIndicator.health();
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals(0, health.getDetails().get("activeUsers"));
    }

    @Test
    @DisplayName("should return down when repository throws exception")
    void shouldReturnDownWhenExceptionOccurs(){
        when(userRepository.countActiveUsers()).thenThrow(new RuntimeException("Database error"));
        Health health = healthIndicator.health();
        assertEquals(Status.DOWN,health.getStatus());
    }
}
