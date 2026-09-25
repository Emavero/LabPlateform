package com.labplatform.adapter.in.web.security;

import com.labplatform.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Pose, lit et efface le cookie de session.
 * HttpOnly : inaccessible au JavaScript (protège le jeton contre le XSS).
 * SameSite=Strict : jamais envoyé par un site tiers (protège contre le CSRF).
 * Secure : activé dès que l'application est servie en HTTPS.
 */
@Component
public class SessionCookieManager {

    private final AppProperties.Session settings;
    private final Duration maxAge;

    public SessionCookieManager(AppProperties properties) {
        this.settings = properties.getSession();
        this.maxAge = properties.getJwt().getValidity();
    }

    public String issueHeaderValue(String token) {
        return build(token, maxAge).toString();
    }

    public String clearHeaderValue() {
        return build("", Duration.ZERO).toString();
    }

    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> settings.getCookieName().equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }

    public static String headerName() {
        return HttpHeaders.SET_COOKIE;
    }

    private ResponseCookie build(String value, Duration age) {
        return ResponseCookie.from(settings.getCookieName(), value)
                .httpOnly(true)
                .secure(settings.isCookieSecure())
                .sameSite(settings.getCookieSameSite())
                .path("/api")
                .maxAge(age)
                .build();
    }
}
