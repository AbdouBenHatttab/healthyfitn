package com.healthapp.doctor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

/**
 * Générateur de credentials TURN temporaires pour Metered.ca
 *
 * Documentation: https://www.metered.ca/docs/turnserver-guides/expiring-turn-credentials/
 */
@Component
@Slf4j
public class MeteredTurnCredentialsGenerator {

    @Value("${webrtc.metered.domain:web_rtc.metered.live}")
    private String meteredDomain;

    @Value("${webrtc.metered.secret-key:ws65O0FE2yBYYD9AADUYtVzQxc3X9fOuK6GnbnRFmMXIP6Ty}")
    private String secretKey;

    @Value("${webrtc.metered.credential-ttl:86400}") // 24 heures par défaut
    private long credentialTtlSeconds;

    /**
     * Génère des credentials TURN temporaires
     *
     * @return TurnCredentials contenant username et password
     */
    public TurnCredentials generateCredentials() {
        try {
            // 1. Calculer le timestamp d'expiration (timestamp actuel + TTL)
            long expirationTimestamp = System.currentTimeMillis() / 1000 + credentialTtlSeconds;

            // 2. Créer un username aléatoire
            String randomUsername = UUID.randomUUID().toString().substring(0, 8);

            // 3. Format du username: timestamp:random
            String username = expirationTimestamp + ":" + randomUsername;

            // 4. Calculer le password avec HMAC-SHA1
            String password = calculateHmacSha1(username, secretKey);

            log.info("✅ Generated Metered TURN credentials (expires in {}h)", credentialTtlSeconds / 3600);

            return new TurnCredentials(username, password, expirationTimestamp);

        } catch (Exception e) {
            log.error("❌ Error generating TURN credentials: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate TURN credentials", e);
        }
    }

    /**
     * Calcule HMAC-SHA1 et retourne en Base64
     */
    private String calculateHmacSha1(String data, String key)
            throws NoSuchAlgorithmException, InvalidKeyException {

        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8),
                "HmacSHA1"
        );
        mac.init(secretKeySpec);

        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Génère le JSON complet des ICE servers
     */
    public String generateIceServersJson() {
        TurnCredentials creds = generateCredentials();

        return String.format("""
            [
              {
                "urls": "stun:%s:80"
              },
              {
                "urls": "turn:%s:80",
                "username": "%s",
                "credential": "%s"
              },
              {
                "urls": "turn:%s:80?transport=tcp",
                "username": "%s",
                "credential": "%s"
              },
              {
                "urls": "turn:%s:443",
                "username": "%s",
                "credential": "%s"
              },
              {
                "urls": "turn:%s:443?transport=tcp",
                "username": "%s",
                "credential": "%s"
              }
            ]
            """,
                meteredDomain,
                meteredDomain, creds.getUsername(), creds.getPassword(),
                meteredDomain, creds.getUsername(), creds.getPassword(),
                meteredDomain, creds.getUsername(), creds.getPassword(),
                meteredDomain, creds.getUsername(), creds.getPassword()
        );
    }

    /**
     * Classe interne pour stocker les credentials
     */
    public static class TurnCredentials {
        private final String username;
        private final String password;
        private final long expiresAt;

        public TurnCredentials(String username, String password, long expiresAt) {
            this.username = username;
            this.password = password;
            this.expiresAt = expiresAt;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public long getExpiresAt() {
            return expiresAt;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() / 1000 > expiresAt;
        }
    }
}