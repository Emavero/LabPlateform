package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.vpn.LabVpnProfileSummary;
import com.labplatform.application.port.in.vpn.ManageLabVpnProfileUseCase;
import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.vpn.LabVpnProfile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Dépôt du profil VPN que les apprenants téléchargeront.
 * <p>
 * Sous /api/admin/** : la chaîne de sécurité y exige déjà le rôle
 * d'administrateur, et le cas d'usage le revérifie de son côté.
 */
@RestController
@RequestMapping("/api/admin/vpn/profile")
public class AdminVpnController {

    private final ManageLabVpnProfileUseCase profiles;

    public AdminVpnController(ManageLabVpnProfileUseCase profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    public LabVpnProfileResponse current(@AuthenticationPrincipal AuthenticatedUser user) {
        return profiles.current(user.toActor()).map(LabVpnProfileResponse::from)
                .orElseGet(LabVpnProfileResponse::none);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LabVpnProfileResponse upload(@AuthenticationPrincipal AuthenticatedUser user,
                                        @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidInputException("Le fichier est vide");
        }
        // La taille est vérifiée avant la lecture : inutile de charger dix
        // mégaoctets en mémoire pour conclure que ce n'est pas un .ovpn.
        if (file.getSize() > LabVpnProfile.MAX_SIZE_BYTES) {
            throw new InvalidInputException("Le profil dépasse " + (LabVpnProfile.MAX_SIZE_BYTES / 1024) + " Ko");
        }
        return LabVpnProfileResponse.from(
                profiles.upload(user.toActor(), file.getOriginalFilename(), textOf(file)));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal AuthenticatedUser user) {
        profiles.remove(user.toActor());
    }

    /**
     * Un profil est un fichier texte. Un contenu qui ne se décode pas en UTF-8
     * est écarté ici, avec un message qui dit ce qui a été déposé plutôt qu'une
     * erreur d'encodage.
     */
    private static String textOf(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (MalformedInputException e) {
            throw new InvalidInputException("Le fichier n'est pas un texte : ce n'est pas un profil OpenVPN");
        } catch (IOException e) {
            throw new InvalidInputException("Le fichier n'a pas pu être lu");
        }
    }

    /**
     * Ce que l'administration voit. Jamais le contenu : il porte une clé
     * privée, et l'écran n'en a pas besoin pour faire son travail.
     *
     * @param routesLabNetwork faux quand le profil ne route pas le réseau des
     *                         machines ; l'écran en avertit, sans refuser
     */
    public record LabVpnProfileResponse(boolean present, String fileName, int sizeBytes, Instant uploadedAt,
                                        boolean routesLabNetwork) {

        static LabVpnProfileResponse from(LabVpnProfileSummary summary) {
            return new LabVpnProfileResponse(true, summary.fileName(), summary.sizeBytes(), summary.uploadedAt(),
                    summary.routesLabNetwork());
        }

        static LabVpnProfileResponse none() {
            return new LabVpnProfileResponse(false, null, 0, null, false);
        }
    }
}
