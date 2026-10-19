package com.jobcraft.service;

import com.jobcraft.config.JobCraftProperties;
import com.jobcraft.model.User;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Stellt signierte JWTs (HS256) für angemeldete Benutzer aus. Subject ist die Benutzer-ID.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String ISSUER = "jobcraft";

    private final JwtEncoder jwtEncoder;
    private final JobCraftProperties properties;

    /**
     * Erzeugt ein Token für den Benutzer.
     *
     * @param user Benutzer
     * @return kompakt serialisiertes JWT
     */
    public String createToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(properties.jwt().expiration()))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
