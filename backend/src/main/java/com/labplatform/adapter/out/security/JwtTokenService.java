package com.labplatform.adapter.out.security;

import com.labplatform.adapter.in.web.security.AccessTokenVerifier;
import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.auth.UserSummary;
import com.labplatform.application.port.out.AccessTokenIssuerPort;
import com.labplatform.config.AppProperties;
import com.labplatform.domain.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Émission et vérification des JWT de session (signature HMAC-SHA).
 * Le jeton ne quitte le serveur que dans un cookie HttpOnly : le code
 * JavaScript du navigateur ne peut jamais le lire.
 */
@Component
public class JwtTokenService implements AccessTokenIssuerPort, AccessTokenVerifier {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey signingKey;
    private final long validitySeconds;
    private final String issuer;
    private final Clock clock;

    public JwtTokenService(AppProperties properties, Clock clock) {
        byte[] secret = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.jwt.secret doit contenir au moins " + MIN_SECRET_BYTES
                    + " octets (variable d'environnement APP_JWT_SECRET)");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret);
        this.validitySeconds = properties.getJwt().getValidity().toSeconds();
        this.issuer = properties.getJwt().getIssuer();
        this.clock = clock;
    }

    @Override
    public String issue(UserSummary user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(user.id()))
                .claim(CLAIM_EMAIL, user.email())
                .claim(CLAIM_ROLE, user.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(validitySeconds)))
                .signWith(signingKey)
                .compact();
    }

    @Override
    public Optional<AuthenticatedUser> verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new AuthenticatedUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get(CLAIM_EMAIL, String.class),
                    Role.valueOf(claims.get(CLAIM_ROLE, String.class))));
        } catch (JwtException | IllegalArgumentException | NullPointerException invalid) {
            return Optional.empty();
        }
    }

    public long validitySeconds() {
        return validitySeconds;
    }
}
