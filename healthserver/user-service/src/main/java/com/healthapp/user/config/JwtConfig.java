package com.healthapp.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtConfig {
    private String secret = "21f4e176cd2f4b991bd27fd94a7acfa923a032015252f7f725cee7761503b6120d0f92dcda38390c619190e921833477ea8f32100e9d59bcd398073b1552c15e";
    private long accessTokenExpiration = 900000;
    private String issuer = "health-app";
}
