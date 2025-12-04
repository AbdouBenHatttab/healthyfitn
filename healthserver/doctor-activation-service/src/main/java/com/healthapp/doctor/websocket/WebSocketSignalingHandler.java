package com.healthapp.doctor.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket Signaling Handler avec Keycloak
 * ✅ Utilise JwtDecoder pour valider les tokens Keycloak
 * ✅ Extrait les informations utilisateur depuis le token JWT
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketSignalingHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtDecoder jwtDecoder; // ✅ Injecté par Spring (OAuth2 Resource Server)

    // Map: callId -> Map<userId, WebSocketSession>
    private final Map<String, ConcurrentHashMap<String, WebSocketSession>> callSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String callId = extractCallId(session);
        String userId = extractUserId(session);
        String token = extractToken(session);

        log.info("═══════════════════════════════════════════════════════════════");
        log.info("🔗 WEBSOCKET CONNECTION ATTEMPT (KEYCLOAK)");
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("   Session ID: {}", session.getId());
        log.info("   Call ID: {}", callId);
        log.info("   User ID: {}", userId);
        log.info("   Token present: {}", token != null && !token.isEmpty());
        log.info("   URI: {}", session.getUri());
        log.info("═══════════════════════════════════════════════════════════════");

        // ✅ Validate JWT token
        if (token == null || token.isEmpty()) {
            log.error("❌ No token provided");
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Authentication required"));
            return;
        }

        try {
            // ✅ Décoder et valider le token Keycloak
            Jwt jwt = jwtDecoder.decode(token);

            // Extraire l'email (preferred_username)
            String email = jwt.getClaimAsString("preferred_username");
            if (email == null) {
                email = jwt.getClaimAsString("email");
            }

            // Extraire les rôles
            List<String> roles = extractRoles(jwt);

            log.info("✅ Token validated for user: {} (Keycloak)", email);
            log.info("   Roles: {}", roles);

            // Store in session attributes
            session.getAttributes().put("email", email);
            session.getAttributes().put("roles", roles);
            session.getAttributes().put("authenticated", true);
            session.getAttributes().put("keycloakUserId", jwt.getSubject());

        } catch (Exception e) {
            log.error("❌ Token validation failed", e);
            log.error("   Error type: {}", e.getClass().getSimpleName());
            log.error("   Error message: {}", e.getMessage());
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Authentication failed"));
            return;
        }

        // Add session to map
        callSessions.computeIfAbsent(callId, k -> new ConcurrentHashMap<>())
                .put(userId, session);

        ConcurrentHashMap<String, WebSocketSession> sessions = callSessions.get(callId);
        int participantCount = sessions.size();

        log.info("📊 Active participants for call {}: {}", callId, participantCount);

        // Send CONNECTED confirmation
        try {
            Map<String, Object> confirmMsg = Map.of(
                    "type", "CONNECTED",
                    "callId", callId,
                    "userId", userId
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(confirmMsg)));
            log.info("✅ Sent CONNECTED confirmation to {}", userId);
        } catch (Exception e) {
            log.error("❌ Error sending confirmation", e);
        }

        // Notify others that user joined
        try {
            Map<String, Object> joinMsg = Map.of(
                    "type", "USER_JOINED",
                    "userId", userId,
                    "participantCount", participantCount
            );
            broadcastToOthers(callId, userId, joinMsg);
            log.info("📢 Broadcasted USER_JOINED to {} other participants", participantCount - 1);
        } catch (Exception e) {
            log.error("❌ Error broadcasting join", e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String callId = extractCallId(session);
        String senderId = extractUserId(session);

        String payload = message.getPayload();
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("📨 MESSAGE RECEIVED");
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("   From: {}", senderId);
        log.info("   Call ID: {}", callId);
        log.info("   Payload length: {} chars", payload.length());

        // Parse message
        Map<String, Object> data = objectMapper.readValue(payload, Map.class);
        String type = (String) data.get("type");
        log.info("   Message type: {}", type);
        log.info("═══════════════════════════════════════════════════════════════");

        // Add fromUserId to all messages
        data.put("fromUserId", senderId);
        String enrichedPayload = objectMapper.writeValueAsString(data);

        // Broadcast to other participants
        int forwardedCount = broadcastToOthers(callId, senderId, enrichedPayload);
        log.info("📤 Message forwarded to {} recipients", forwardedCount);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String callId = extractCallId(session);
        String userId = extractUserId(session);

        log.info("═══════════════════════════════════════════════════════════════");
        log.info("🔌 WEBSOCKET DISCONNECTED");
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("   Session ID: {}", session.getId());
        log.info("   Call ID: {}", callId);
        log.info("   User ID: {}", userId);
        log.info("   Close status: {} - {}", status.getCode(), status.getReason());
        log.info("═══════════════════════════════════════════════════════════════");

        // Remove session
        ConcurrentHashMap<String, WebSocketSession> sessions = callSessions.get(callId);
        if (sessions != null) {
            sessions.remove(userId);
            log.info("📊 Remaining sessions for call {}: {}", callId, sessions.size());

            // Notify others
            try {
                Map<String, String> endMsg = Map.of(
                        "type", "PEER_DISCONNECTED",
                        "userId", userId
                );
                broadcastToOthers(callId, userId, endMsg);
            } catch (Exception e) {
                log.error("Error notifying peer disconnect", e);
            }

            // Clean up if empty
            if (sessions.isEmpty()) {
                callSessions.remove(callId);
                log.info("🗑️ Removed empty call session: {}", callId);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String callId = extractCallId(session);
        String userId = extractUserId(session);

        log.error("═══════════════════════════════════════════════════════════════");
        log.error("❌ WEBSOCKET TRANSPORT ERROR");
        log.error("═══════════════════════════════════════════════════════════════");
        log.error("   Session ID: {}", session.getId());
        log.error("   Call ID: {}", callId);
        log.error("   User ID: {}", userId);
        log.error("   Error: {}", exception.getMessage());
        log.error("═══════════════════════════════════════════════════════════════");
        exception.printStackTrace();
    }

    /**
     * Extract callId from path using Spring's UriTemplate
     */
    private String extractCallId(WebSocketSession session) {
        try {
            URI uri = session.getUri();
            String path = uri.getPath();

            UriTemplate template = new UriTemplate("/ws/webrtc/{callId}");
            Map<String, String> variables = template.match(path);

            if (variables != null && variables.containsKey("callId")) {
                return variables.get("callId");
            }

            log.warn("⚠️ Could not extract callId from path: {}", path);
            return "unknown";

        } catch (Exception e) {
            log.error("❌ Error extracting callId", e);
            return "unknown";
        }
    }

    /**
     * Extract userId from query params
     */
    private String extractUserId(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            if (query == null || query.isEmpty()) {
                return "unknown";
            }

            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && pair[0].equals("userId")) {
                    return java.net.URLDecoder.decode(pair[1], "UTF-8");
                }
            }

            return "unknown";
        } catch (Exception e) {
            log.error("❌ Error extracting userId", e);
            return "unknown";
        }
    }

    /**
     * Extract JWT token from query params
     */
    private String extractToken(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            if (query == null || query.isEmpty()) {
                return null;
            }

            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && pair[0].equals("token")) {
                    return java.net.URLDecoder.decode(pair[1], "UTF-8");
                }
            }

            return null;
        } catch (Exception e) {
            log.error("❌ Error extracting token", e);
            return null;
        }
    }

    /**
     * Extract roles from Keycloak JWT token
     * Roles are stored in: realm_access.roles
     */
    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        try {
            // Extract realm_access claim
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");

            if (realmAccess != null && realmAccess.containsKey("roles")) {
                Object rolesObj = realmAccess.get("roles");

                if (rolesObj instanceof List) {
                    return (List<String>) rolesObj;
                }
            }

            log.warn("⚠️ No roles found in token");
            return new ArrayList<>();

        } catch (Exception e) {
            log.error("❌ Error extracting roles from token", e);
            return new ArrayList<>();
        }
    }

    /**
     * Broadcast message to all participants except sender
     */
    private int broadcastToOthers(String callId, String senderId, Object message) {
        ConcurrentHashMap<String, WebSocketSession> sessions = callSessions.get(callId);
        if (sessions == null) {
            return 0;
        }

        int forwardedCount = 0;
        String payload = (message instanceof String) ?
                (String) message :
                toJson(message);

        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            String userId = entry.getKey();
            WebSocketSession wsSession = entry.getValue();

            if (!userId.equals(senderId) && wsSession.isOpen()) {
                try {
                    wsSession.sendMessage(new TextMessage(payload));
                    forwardedCount++;
                } catch (IOException e) {
                    log.error("   ❌ Error forwarding to {}", userId, e);
                }
            }
        }

        return forwardedCount;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Error converting to JSON", e);
            return "{}";
        }
    }
}