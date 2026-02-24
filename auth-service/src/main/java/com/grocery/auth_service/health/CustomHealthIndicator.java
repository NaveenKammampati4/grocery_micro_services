package com.grocery.auth_service.health;

import com.grocery.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomHealthIndicator implements HealthIndicator {

    private final UserRepository userRepository;

    @Override
    public Health health() {
        try {
            long activeUsers = userRepository.countActiveUsers();
            if (activeUsers>0){
                return Health.up()
                        .withDetail("activeUsers", activeUsers)
                        .withDetail("dbConnection","OK")
                        .build();
            }
            return Health.down().withDetail("activeUsers", 0).build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
