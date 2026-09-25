package com.labplatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Configuration applicative typée (préfixe "app" dans application.yml). */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Session session = new Session();
    private final Cors cors = new Cors();
    private final Security security = new Security();
    private final Hypervisor hypervisor = new Hypervisor();
    private final Vpn vpn = new Vpn();
    private final Boxes boxes = new Boxes();

    public Jwt getJwt() {
        return jwt;
    }

    public Session getSession() {
        return session;
    }

    public Cors getCors() {
        return cors;
    }

    public Security getSecurity() {
        return security;
    }

    public Vpn getVpn() {
        return vpn;
    }

    public Hypervisor getHypervisor() {
        return hypervisor;
    }

    public Boxes getBoxes() {
        return boxes;
    }

    public static class Jwt {
        private String secret;
        private Duration validity = Duration.ofHours(8);
        private String issuer = "lab-platform";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public Duration getValidity() {
            return validity;
        }

        public void setValidity(Duration validity) {
            this.validity = validity;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }
    }

    public static class Session {
        private String cookieName = "LAB_SESSION";
        private boolean cookieSecure = false;
        private String cookieSameSite = "Strict";

        public String getCookieName() {
            return cookieName;
        }

        public void setCookieName(String cookieName) {
            this.cookieName = cookieName;
        }

        public boolean isCookieSecure() {
            return cookieSecure;
        }

        public void setCookieSecure(boolean cookieSecure) {
            this.cookieSecure = cookieSecure;
        }

        public String getCookieSameSite() {
            return cookieSameSite;
        }

        public void setCookieSameSite(String cookieSameSite) {
            this.cookieSameSite = cookieSameSite;
        }
    }

    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Security {
        private boolean exposeResetTokenInResponse = false;
        private Duration resetTokenValidity = Duration.ofMinutes(15);

        public boolean isExposeResetTokenInResponse() {
            return exposeResetTokenInResponse;
        }

        public void setExposeResetTokenInResponse(boolean exposeResetTokenInResponse) {
            this.exposeResetTokenInResponse = exposeResetTokenInResponse;
        }

        public Duration getResetTokenValidity() {
            return resetTokenValidity;
        }

        public void setResetTokenValidity(Duration resetTokenValidity) {
            this.resetTokenValidity = resetTokenValidity;
        }
    }

    /** Catalogue de machines à compromettre. */
    public static class Boxes {
        /**
         * true uniquement pour les démonstrations : les flags tirés au
         * premier démarrage sont alors écrits dans les journaux, faute de
         * machines réelles où aller les chercher.
         */
        private boolean logSeededFlags = false;
        private int leaderboardSize = 20;

        public boolean isLogSeededFlags() {
            return logSeededFlags;
        }

        public void setLogSeededFlags(boolean logSeededFlags) {
            this.logSeededFlags = logSeededFlags;
        }

        public int getLeaderboardSize() {
            return leaderboardSize;
        }

        public void setLeaderboardSize(int leaderboardSize) {
            this.leaderboardSize = leaderboardSize;
        }
    }

    public static class Hypervisor {
        private String mode = "simulated";
        private String defaultUsername = "labuser";
        private String simulatedSubnetPrefix = "10.42";
        private Duration simulatedBootDelay = Duration.ofMillis(1500);
        private Duration simulatedShutdownDelay = Duration.ofMillis(600);
        private final Docker docker = new Docker();

        public Docker getDocker() {
            return docker;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getDefaultUsername() {
            return defaultUsername;
        }

        public void setDefaultUsername(String defaultUsername) {
            this.defaultUsername = defaultUsername;
        }

        public String getSimulatedSubnetPrefix() {
            return simulatedSubnetPrefix;
        }

        public void setSimulatedSubnetPrefix(String simulatedSubnetPrefix) {
            this.simulatedSubnetPrefix = simulatedSubnetPrefix;
        }

        public Duration getSimulatedBootDelay() {
            return simulatedBootDelay;
        }

        public void setSimulatedBootDelay(Duration simulatedBootDelay) {
            this.simulatedBootDelay = simulatedBootDelay;
        }

        public Duration getSimulatedShutdownDelay() {
            return simulatedShutdownDelay;
        }

        public void setSimulatedShutdownDelay(Duration simulatedShutdownDelay) {
            this.simulatedShutdownDelay = simulatedShutdownDelay;
        }
    }

    /** Machines Linux en conteneurs Docker (app.hypervisor.mode=docker). */
    public static class Docker {
        private String binary = "docker";
        private String image = "labplatform/lab-linux:latest";
        private String publicHost = "localhost";
        private String bindAddress = "127.0.0.1";
        private String network = "";
        private String memory = "512m";
        private String cpus = "1.0";
        private int pidsLimit = 256;
        private String containerPrefix = "labplatform-vm-";
        private Duration commandTimeout = Duration.ofSeconds(60);
        private Duration readyTimeout = Duration.ofSeconds(20);
        private Duration pollInterval = Duration.ofMillis(250);

        public String getBinary() {
            return binary;
        }

        public void setBinary(String binary) {
            this.binary = binary;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public String getPublicHost() {
            return publicHost;
        }

        public void setPublicHost(String publicHost) {
            this.publicHost = publicHost;
        }

        public String getBindAddress() {
            return bindAddress;
        }

        public void setBindAddress(String bindAddress) {
            this.bindAddress = bindAddress;
        }

        public String getNetwork() {
            return network;
        }

        public void setNetwork(String network) {
            this.network = network;
        }

        public String getMemory() {
            return memory;
        }

        public void setMemory(String memory) {
            this.memory = memory;
        }

        public String getCpus() {
            return cpus;
        }

        public void setCpus(String cpus) {
            this.cpus = cpus;
        }

        public int getPidsLimit() {
            return pidsLimit;
        }

        public void setPidsLimit(int pidsLimit) {
            this.pidsLimit = pidsLimit;
        }

        public String getContainerPrefix() {
            return containerPrefix;
        }

        public void setContainerPrefix(String containerPrefix) {
            this.containerPrefix = containerPrefix;
        }

        public Duration getCommandTimeout() {
            return commandTimeout;
        }

        public void setCommandTimeout(Duration commandTimeout) {
            this.commandTimeout = commandTimeout;
        }

        public Duration getReadyTimeout() {
            return readyTimeout;
        }

        public void setReadyTimeout(Duration readyTimeout) {
            this.readyTimeout = readyTimeout;
        }

        public Duration getPollInterval() {
            return pollInterval;
        }

        public void setPollInterval(Duration pollInterval) {
            this.pollInterval = pollInterval;
        }
    }

    /** Accès VPN des utilisateurs (profils .ovpn personnels). */
    public static class Vpn {
        private boolean enabled = false;
        private String udpHost = "";
        private int udpPort = 1194;
        private String tcpHost = "";
        private int tcpPort = 1194;
        private String labNetwork = "10.10.10.0/24";
        private String easyrsaBinary = "/usr/share/easy-rsa/easyrsa";
        private String directory = "/var/lib/labplatform/vpn";
        private String caName = "cyberMans-Lab-CA";
        private String serverName = "serveur";
        private Duration commandTimeout = Duration.ofSeconds(120);
        private Duration crlRefresh = Duration.ofDays(1);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUdpHost() {
            return udpHost;
        }

        public void setUdpHost(String udpHost) {
            this.udpHost = udpHost;
        }

        public int getUdpPort() {
            return udpPort;
        }

        public void setUdpPort(int udpPort) {
            this.udpPort = udpPort;
        }

        public String getTcpHost() {
            return tcpHost;
        }

        public void setTcpHost(String tcpHost) {
            this.tcpHost = tcpHost;
        }

        public int getTcpPort() {
            return tcpPort;
        }

        public void setTcpPort(int tcpPort) {
            this.tcpPort = tcpPort;
        }

        public String getLabNetwork() {
            return labNetwork;
        }

        public void setLabNetwork(String labNetwork) {
            this.labNetwork = labNetwork;
        }

        public String getEasyrsaBinary() {
            return easyrsaBinary;
        }

        public void setEasyrsaBinary(String easyrsaBinary) {
            this.easyrsaBinary = easyrsaBinary;
        }

        public String getDirectory() {
            return directory;
        }

        public void setDirectory(String directory) {
            this.directory = directory;
        }

        public String getCaName() {
            return caName;
        }

        public void setCaName(String caName) {
            this.caName = caName;
        }

        public String getServerName() {
            return serverName;
        }

        public void setServerName(String serverName) {
            this.serverName = serverName;
        }

        public Duration getCommandTimeout() {
            return commandTimeout;
        }

        public void setCommandTimeout(Duration commandTimeout) {
            this.commandTimeout = commandTimeout;
        }

        public Duration getCrlRefresh() {
            return crlRefresh;
        }

        public void setCrlRefresh(Duration crlRefresh) {
            this.crlRefresh = crlRefresh;
        }
    }
}
