package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.dto.VpnDtos.VpnAccessResponse;
import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.vpn.DownloadVpnProfileUseCase;
import com.labplatform.application.port.in.vpn.GetVpnAccessUseCase;
import com.labplatform.application.port.in.vpn.GetVpnRevocationListUseCase;
import com.labplatform.application.port.in.vpn.RegenerateVpnProfileUseCase;
import com.labplatform.application.port.in.vpn.VpnProfileFile;
import com.labplatform.domain.vpn.VpnProtocol;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/vpn")
public class VpnController {

    private static final MediaType OVPN = MediaType.parseMediaType("application/x-openvpn-profile");
    private static final MediaType PEM = MediaType.parseMediaType("application/x-pem-file");

    private final GetVpnAccessUseCase getAccess;
    private final DownloadVpnProfileUseCase downloadProfile;
    private final RegenerateVpnProfileUseCase regenerateProfile;
    private final GetVpnRevocationListUseCase revocationList;

    public VpnController(GetVpnAccessUseCase getAccess, DownloadVpnProfileUseCase downloadProfile,
                         RegenerateVpnProfileUseCase regenerateProfile, GetVpnRevocationListUseCase revocationList) {
        this.getAccess = getAccess;
        this.downloadProfile = downloadProfile;
        this.regenerateProfile = regenerateProfile;
        this.revocationList = revocationList;
    }

    @GetMapping
    public VpnAccessResponse access(@AuthenticationPrincipal AuthenticatedUser user) {
        return VpnAccessResponse.from(getAccess.getAccess(user.toActor()));
    }

    /** Lien de téléchargement direct : le navigateur enregistre le fichier .ovpn. */
    @GetMapping("/profile")
    public ResponseEntity<byte[]> profile(@AuthenticationPrincipal AuthenticatedUser user,
                                          @RequestParam(defaultValue = "udp") String protocol) {
        VpnProfileFile file = downloadProfile.download(user.toActor(), VpnProtocol.fromParameter(protocol));
        return ResponseEntity.ok()
                .contentType(OVPN)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName()).build().toString())
                // Le fichier contient une clé privée : aucun cache, ni navigateur ni proxy.
                .cacheControl(CacheControl.noStore())
                .body(file.content().getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/profile/regenerate")
    public VpnAccessResponse regenerate(@AuthenticationPrincipal AuthenticatedUser user) {
        return VpnAccessResponse.from(regenerateProfile.regenerate(user.toActor()));
    }

    /**
     * Liste de révocation, publique par nature (elle ne contient que des numéros de série) :
     * la passerelle OpenVPN la récupère régulièrement pour refuser les profils régénérés.
     */
    @GetMapping("/crl.pem")
    public ResponseEntity<String> crl() {
        return ResponseEntity.ok()
                .contentType(PEM)
                .cacheControl(CacheControl.noCache())
                .body(revocationList.revocationList());
    }
}
