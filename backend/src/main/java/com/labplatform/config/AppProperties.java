package com.labplatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.math.BigDecimal;
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
    private final Media media = new Media();
    private final Billing billing = new Billing();
    private final Mail mail = new Mail();
    private final Gcp gcp = new Gcp();
    private final Machine machine = new Machine();

    /**
     * Adresse publique de l'application, telle que la voit l'utilisateur.
     * <p>
     * Elle sert à fabriquer les liens envoyés par courriel : l'adresse interne
     * du conteneur ne serait cliquable pour personne. Derrière ngrok ou un nom
     * de domaine, c'est cette valeur qu'il faut changer.
     */
    private String publicUrl = "http://localhost:3000";

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

    public Gcp getGcp() {
        return gcp;
    }

    public Machine getMachine() {
        return machine;
    }

    public Boxes getBoxes() {
        return boxes;
    }

    public Media getMedia() {
        return media;
    }

    public Billing getBilling() {
        return billing;
    }

    public Mail getMail() {
        return mail;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
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
        /**
         * Comptes administrateurs, par adresse e-mail. Ils sont promus au
         * démarrage s'ils existent déjà, et à l'inscription sinon : c'est la
         * seule façon d'obtenir le rôle, qu'aucune route n'accorde.
         */
        private List<String> adminEmails = new ArrayList<>();

        public List<String> getAdminEmails() {
            return adminEmails;
        }

        public void setAdminEmails(List<String> adminEmails) {
            this.adminEmails = adminEmails;
        }

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

    /**
     * Abonnement Pro et encaissement.
     * <p>
     * {@code mode} vaut {@code simulated} par défaut : l'abonnement se déroule
     * de bout en bout sans compte marchand, ce qui convient à une
     * démonstration ou à un lab interne, mais accorde l'abonnement à qui
     * clique. Une installation qui facture réellement passe en {@code live} et
     * renseigne les clés du ou des prestataires.
     * <p>
     * Le tarif de base est exprimé dans {@code currency}, avec le montant tel
     * qu'il s'écrit (« 5000 » pour 5 000 F CFA, « 8.00 » pour huit euros).
     * {@code card} le remplace pour la carte, dont les réseaux n'acceptent pas
     * le franc CFA.
     */
    /**
     * Envoi des courriels transactionnels.
     * <p>
     * Éteint par défaut : une plateforme d'essai n'a pas de serveur SMTP, et le
     * lien de réinitialisation s'affiche alors à l'écran
     * ({@code app.security.expose-reset-token-in-response}). Les deux ne
     * doivent pas rester vrais ensemble une fois la plateforme ouverte.
     */
    public static class Mail {

        private boolean enabled = false;
        /** Expéditeur affiché. Certains serveurs exigent qu'il corresponde au compte SMTP. */
        private String from = "cyberMans <no-reply@cybermans.local>";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }
    }

    public static class Billing {
        private boolean enabled = true;
        private String mode = "simulated";
        private String currency = "XOF";
        private BigDecimal monthly = new BigDecimal("5000");
        private BigDecimal yearly = new BigDecimal("50000");
        private List<String> methods = new ArrayList<>(List.of("CARD", "WAVE"));
        /** Page de retour après paiement, côté frontend. */
        private String successUrl = "http://localhost:3000/abonnement/retour";
        private String cancelUrl = "http://localhost:3000/abonnement";
        private final Price card = new Price();
        private final Stripe stripe = new Stripe();
        private final Wave wave = new Wave();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public boolean isLive() {
            return "live".equalsIgnoreCase(mode);
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public BigDecimal getMonthly() {
            return monthly;
        }

        public void setMonthly(BigDecimal monthly) {
            this.monthly = monthly;
        }

        public BigDecimal getYearly() {
            return yearly;
        }

        public void setYearly(BigDecimal yearly) {
            this.yearly = yearly;
        }

        public List<String> getMethods() {
            return methods;
        }

        public void setMethods(List<String> methods) {
            this.methods = methods;
        }

        public String getSuccessUrl() {
            return successUrl;
        }

        public void setSuccessUrl(String successUrl) {
            this.successUrl = successUrl;
        }

        public String getCancelUrl() {
            return cancelUrl;
        }

        public void setCancelUrl(String cancelUrl) {
            this.cancelUrl = cancelUrl;
        }

        public Price getCard() {
            return card;
        }

        public Stripe getStripe() {
            return stripe;
        }

        public Wave getWave() {
            return wave;
        }

        /** Tarif propre à un moyen de paiement. Devise vide : pas de dérogation. */
        public static class Price {
            private String currency = "";
            private BigDecimal monthly;
            private BigDecimal yearly;

            public boolean isDefined() {
                return !currency.isBlank() && monthly != null && yearly != null;
            }

            public String getCurrency() {
                return currency;
            }

            public void setCurrency(String currency) {
                this.currency = currency;
            }

            public BigDecimal getMonthly() {
                return monthly;
            }

            public void setMonthly(BigDecimal monthly) {
                this.monthly = monthly;
            }

            public BigDecimal getYearly() {
                return yearly;
            }

            public void setYearly(BigDecimal yearly) {
                this.yearly = yearly;
            }
        }

        public static class Stripe {
            private String baseUrl = "";
            private String secretKey = "";
            private String webhookSecret = "";

            public String getBaseUrl() {
                return baseUrl;
            }

            public void setBaseUrl(String baseUrl) {
                this.baseUrl = baseUrl;
            }

            public String getSecretKey() {
                return secretKey;
            }

            public void setSecretKey(String secretKey) {
                this.secretKey = secretKey;
            }

            public String getWebhookSecret() {
                return webhookSecret;
            }

            public void setWebhookSecret(String webhookSecret) {
                this.webhookSecret = webhookSecret;
            }
        }

        public static class Wave {
            private String baseUrl = "";
            private String apiKey = "";
            private String webhookSecret = "";

            public String getBaseUrl() {
                return baseUrl;
            }

            public void setBaseUrl(String baseUrl) {
                this.baseUrl = baseUrl;
            }

            public String getApiKey() {
                return apiKey;
            }

            public void setApiKey(String apiKey) {
                this.apiKey = apiKey;
            }

            public String getWebhookSecret() {
                return webhookSecret;
            }

            public void setWebhookSecret(String webhookSecret) {
                this.webhookSecret = webhookSecret;
            }
        }
    }

    /** Fichiers téléversés (vidéos de cours). */
    public static class Media {
        /** Dossier persistant : à sauvegarder, comme le volume du VPN. */
        private String directory = "/var/lib/labplatform/media";
        private DataSize maxFileSize = DataSize.ofMegabytes(256);

        public String getDirectory() {
            return directory;
        }

        public void setDirectory(String directory) {
            this.directory = directory;
        }

        public DataSize getMaxFileSize() {
            return maxFileSize;
        }

        public void setMaxFileSize(DataSize maxFileSize) {
            this.maxFileSize = maxFileSize;
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
        /**
         * Durée de vie d'une cible lancée à la demande. Passée l'échéance,
         * elle est éteinte : une machine oubliée ne monopolise pas
         * l'infrastructure.
         */
        private Duration instanceLifetime = Duration.ofHours(2);

        public boolean isLogSeededFlags() {
            return logSeededFlags;
        }

        public void setLogSeededFlags(boolean logSeededFlags) {
            this.logSeededFlags = logSeededFlags;
        }

        public int getLeaderboardSize() {
            return leaderboardSize;
        }

        public Duration getInstanceLifetime() {
            return instanceLifetime;
        }

        public void setInstanceLifetime(Duration instanceLifetime) {
            this.instanceLifetime = instanceLifetime;
        }

        public void setLeaderboardSize(int leaderboardSize) {
            this.leaderboardSize = leaderboardSize;
        }
    }

    /**
     * Projet Compute Engine. Partagé par la cible unique (app.machine) et par
     * le mode d'hyperviseur « gcp » : les deux parlent au même projet, dans la
     * même zone, avec les mêmes identifiants.
     * <p>
     * Les identifiants eux-mêmes ne figurent pas ici : ils viennent des
     * identifiants par défaut de l'application (GOOGLE_APPLICATION_CREDENTIALS
     * ou identité attachée à la machine), et ne transitent jamais par le client.
     */
    public static class Gcp {
        private String projectId = "";
        private String zone = "europe-west1-b";
        private String instanceName = "";
        /** Gabarit du nom d'instance d'une cible du catalogue ; {slug} est remplacé. */
        private String targetName = "{slug}";
        private Duration operationTimeout = Duration.ofSeconds(60);

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        public String getZone() {
            return zone;
        }

        public void setZone(String zone) {
            this.zone = zone;
        }

        public String getInstanceName() {
            return instanceName;
        }

        public void setInstanceName(String instanceName) {
            this.instanceName = instanceName;
        }

        public String getTargetName() {
            return targetName;
        }

        public void setTargetName(String targetName) {
            this.targetName = targetName;
        }

        public Duration getOperationTimeout() {
            return operationTimeout;
        }

        public void setOperationTimeout(Duration operationTimeout) {
            this.operationTimeout = operationTimeout;
        }
    }

    /** Cible partagée pilotée par /api/machine : « simulated » ou « gcp ». */
    public static class Machine {
        private String provider = "simulated";
        /** Adresse interne annoncée par la cible simulée. */
        private String simulatedAddress = "10.10.10.10";

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getSimulatedAddress() {
            return simulatedAddress;
        }

        public void setSimulatedAddress(String simulatedAddress) {
            this.simulatedAddress = simulatedAddress;
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
        /** Image d'une cible du catalogue ; {slug} est remplacé par celui de la machine. */
        private String boxImagePattern = "labplatform/box-{slug}:latest";
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

        public String getBoxImagePattern() {
            return boxImagePattern;
        }

        public void setBoxImagePattern(String boxImagePattern) {
            this.boxImagePattern = boxImagePattern;
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
        /**
         * « generated » : la plateforme émet un certificat par apprenant.
         * « uploaded » : l'administrateur dépose un profil, partagé par tous.
         */
        private String source = "generated";
        private String udpHost = "";
        private int udpPort = 1194;
        private String tcpHost = "";
        private int tcpPort = 1194;
        private String labNetwork = "10.10.10.0/24";

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
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
