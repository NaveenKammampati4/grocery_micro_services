package com.grocery.auth_service.client;

import com.grocery.auth_service.dto.request.InitProfileRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "USER-SERVICE")
public interface UserServiceClient {

    @PostMapping("/api/users/{userId}/init-profile")
    ResponseEntity<Void> initProfile(@PathVariable Long userId, @RequestBody InitProfileRequest request);
}
