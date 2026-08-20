package com.labplatform.auth;

import com.labplatform.common.BusinessException;
import com.labplatform.security.JwtService;
import com.labplatform.user.Role;
import com.labplatform.user.User;
import com.labplatform.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long RESET_TOKEN_VALIDITY_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;

    // En dev, aucun serveur d'e-mail n'est branché : on renvoie le token
    // directement dans la réponse API pour permettre de tester le flux.
    // En production, mettre à false et brancher un vrai envoi d'e-mail.
    @Value("${app.security.expose-reset-token-in-response:true}")
    private boolean exposeResetTokenInResponse;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessException("Le mot de passe et sa confirmation ne correspondent pas", HttpStatus.BAD_REQUEST);
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Un compte existe déjà avec cette adresse e-mail", HttpStatus.CONFLICT);
        }

        User user = new User(request.email(), passwordEncoder.encode(request.password()), Role.USER);
        User saved = userRepository.save(user);

        // Publication de l'événement : le module lab (ou tout autre futur module)
        // décide lui-même de ce qu'il fait à l'inscription, sans coupler AuthService.
        eventPublisher.publishEvent(new UserRegisteredEvent(saved.getId(), saved.getEmail()));

        String token = jwtService.generateToken(saved);
        return new AuthResponse(token, saved.getEmail(), saved.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable", HttpStatus.UNAUTHORIZED));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getRole().name());
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String genericMessage = "Si un compte existe avec cette adresse, un lien de réinitialisation a été généré.";

        var userOpt = userRepository.findByEmail(request.email());
        if (userOpt.isEmpty()) {
            // Ne pas révéler si l'e-mail existe ou non : réponse identique dans les deux cas.
            return new ForgotPasswordResponse(genericMessage, null);
        }

        User user = userOpt.get();
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiresAt(Instant.now().plusSeconds(RESET_TOKEN_VALIDITY_MINUTES * 60));
        userRepository.save(user);

        return new ForgotPasswordResponse(genericMessage, exposeResetTokenInResponse ? token : null);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException("Le mot de passe et sa confirmation ne correspondent pas", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findByResetToken(request.token())
                .orElseThrow(() -> new BusinessException("Lien de réinitialisation invalide", HttpStatus.BAD_REQUEST));

        if (user.getResetTokenExpiresAt() == null || user.getResetTokenExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Le lien de réinitialisation a expiré", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        userRepository.save(user);
    }
}
