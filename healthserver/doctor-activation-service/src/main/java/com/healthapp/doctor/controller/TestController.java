package com.healthapp.doctor.controller;

import com.healthapp.doctor.service.MeteredTurnCredentialsGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final MeteredTurnCredentialsGenerator generator;

    @GetMapping("/turn-credentials")
    public Map<String, Object> testTurnCredentials() {
        var creds = generator.generateCredentials();

        return Map.of(
                "username", creds.getUsername(),
                "password", creds.getPassword(),
                "expiresAt", creds.getExpiresAt(),
                "iceServersJson", generator.generateIceServersJson()
        );
    }
}
