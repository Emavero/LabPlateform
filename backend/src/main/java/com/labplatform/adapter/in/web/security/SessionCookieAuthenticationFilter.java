package com.labplatform.adapter.in.web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lit le cookie de session, vérifie le jeton et renseigne le SecurityContext.
 * Aucun accès base de données par requête : l'identité et le rôle sont
 * portés par le jeton signé.
 * <p>
 * Volontairement non annoté @Component : il n'est enregistré que dans la
 * chaîne Spring Security (voir SecurityConfig), pas une seconde fois comme
 * filtre servlet global.
 */
public class SessionCookieAuthenticationFilter extends OncePerRequestFilter {

    private final SessionCookieManager cookies;
    private final AccessTokenVerifier verifier;

    public SessionCookieAuthenticationFilter(SessionCookieManager cookies, AccessTokenVerifier verifier) {
        this.cookies = cookies;
        this.verifier = verifier;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            cookies.read(request)
                    .flatMap(verifier::verify)
                    .ifPresent(user -> {
                        var authentication = new UsernamePasswordAuthenticationToken(
                                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }
        chain.doFilter(request, response);
    }
}
