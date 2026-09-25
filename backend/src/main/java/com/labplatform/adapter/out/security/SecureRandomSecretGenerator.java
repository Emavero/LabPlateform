package com.labplatform.adapter.out.security;

import com.labplatform.application.port.out.SecretGeneratorPort;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SecureRandomSecretGenerator implements SecretGeneratorPort {

    private final SecureRandom random = new SecureRandom();

    @Override
    public String urlSafeToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
