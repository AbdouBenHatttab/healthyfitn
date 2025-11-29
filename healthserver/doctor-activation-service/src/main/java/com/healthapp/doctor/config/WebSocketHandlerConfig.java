package com.healthapp.doctor.config;

import com.healthapp.doctor.websocket.WebSocketSignalingHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Slf4j
public class WebSocketHandlerConfig implements WebSocketConfigurer {

    private final WebSocketSignalingHandler signalingHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("═══════════════════════════════════════════");
        log.info("🔌 REGISTERING WEBSOCKET HANDLERS");
        log.info("═══════════════════════════════════════════");
        
        // ✅ Register WebSocket endpoint - NO AUTH INTERCEPTOR
        // Authentication happens inside the handler via query param token
        registry.addHandler(signalingHandler, "/ws/webrtc/{callId}")
                .setAllowedOrigins("*")
                .setAllowedOriginPatterns("*");
        
        log.info("✅ WebSocket registered at: /ws/webrtc/{callId}");
        log.info("✅ Authentication: Via query parameter token");
        log.info("═══════════════════════════════════════════");
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(256 * 1024); // 256KB
        container.setMaxBinaryMessageBufferSize(256 * 1024);
        container.setMaxSessionIdleTimeout(600000L); // 10 minutes
        container.setAsyncSendTimeout(120000L); // 2 minutes
        
        log.info("✅ WebSocket container configured");
        log.info("   Max message size: 256KB");
        log.info("   Session timeout: 10 minutes");
        
        return container;
    }
}