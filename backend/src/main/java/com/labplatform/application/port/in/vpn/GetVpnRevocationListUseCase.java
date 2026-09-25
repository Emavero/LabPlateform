package com.labplatform.application.port.in.vpn;

public interface GetVpnRevocationListUseCase {

    /** Liste de révocation (PEM), récupérée périodiquement par la passerelle OpenVPN. */
    String revocationList();
}
