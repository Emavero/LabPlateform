package com.labplatform.adapter.out.vpn;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * @param directory       dossier persistant du VPN ; la PKI est créée dans son sous-dossier « pki »
 * @param caCommonName    nom de l'autorité de certification
 * @param serverCommonName nom du certificat du serveur OpenVPN (à copier sur la passerelle)
 * @param crlRefresh      âge maximal de la liste de révocation avant régénération
 */
public record EasyRsaSettings(String easyrsaBinary, Path directory, String caCommonName, String serverCommonName,
                              Duration commandTimeout, Duration crlRefresh) {

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_.-]{0,63}");

    public EasyRsaSettings {
        Objects.requireNonNull(easyrsaBinary, "easyrsaBinary");
        Objects.requireNonNull(directory, "directory");
        Objects.requireNonNull(commandTimeout, "commandTimeout");
        Objects.requireNonNull(crlRefresh, "crlRefresh");
        if (caCommonName == null || !NAME.matcher(caCommonName).matches()
                || serverCommonName == null || !NAME.matcher(serverCommonName).matches()) {
            throw new IllegalArgumentException("Noms de l'AC ou du serveur VPN invalides");
        }
    }

    public Path pki() {
        return directory.resolve("pki");
    }

    public Path tlsCryptKey() {
        return directory.resolve("ta.key");
    }
}
