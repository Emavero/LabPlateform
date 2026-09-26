package com.labplatform.config;

import com.labplatform.application.port.in.scoring.GetPlayerProgressUseCase;
import com.labplatform.application.port.out.BoxRatingRepositoryPort;
import com.labplatform.application.port.out.CourseRepositoryPort;
import com.labplatform.application.port.out.SectionCompletionRepositoryPort;
import com.labplatform.application.port.out.AccessTokenIssuerPort;
import com.labplatform.application.port.out.BoxRepositoryPort;
import com.labplatform.application.port.out.OwnRepositoryPort;
import com.labplatform.application.port.out.DomainEventPublisherPort;
import com.labplatform.application.port.out.HypervisorPort;
import com.labplatform.application.port.out.PasswordHasherPort;
import com.labplatform.application.port.out.PasswordResetNotifierPort;
import com.labplatform.application.port.out.SecretGeneratorPort;
import com.labplatform.application.port.out.TransactionPort;
import com.labplatform.application.port.out.UserRepositoryPort;
import com.labplatform.application.port.out.VirtualMachineRepositoryPort;
import com.labplatform.application.service.AcademyService;
import com.labplatform.application.service.AccountService;
import com.labplatform.application.service.BoxService;
import com.labplatform.application.service.ProfileService;
import com.labplatform.application.service.ScoreboardService;
import com.labplatform.application.service.AuthenticationService;
import com.labplatform.application.service.LabService;
import com.labplatform.application.service.PasswordResetService;
import com.labplatform.application.service.VpnService;
import com.labplatform.application.service.VpnSettings;
import com.labplatform.adapter.out.process.ProcessCommandRunner;
import com.labplatform.adapter.out.vpn.EasyRsaCertificateAuthority;
import com.labplatform.adapter.out.vpn.EasyRsaSettings;
import com.labplatform.application.port.out.VpnCertificateAuthorityPort;
import com.labplatform.application.port.out.VpnProfileRepositoryPort;
import com.labplatform.domain.vpn.VpnEndpoint;
import com.labplatform.domain.vpn.VpnProtocol;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * Racine de composition : c'est le seul endroit où les cas d'usage
 * (framework-agnostiques) rencontrent leurs adaptateurs concrets.
 * Les services applicatifs ne portent aucune annotation Spring.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public AuthenticationService authenticationService(UserRepositoryPort users, PasswordHasherPort passwordHasher,
                                                       AccessTokenIssuerPort tokenIssuer, DomainEventPublisherPort events,
                                                       TransactionPort transactions, Clock clock) {
        return new AuthenticationService(users, passwordHasher, tokenIssuer, events, transactions, clock);
    }

    @Bean
    public PasswordResetService passwordResetService(UserRepositoryPort users, PasswordHasherPort passwordHasher,
                                                     SecretGeneratorPort secrets, PasswordResetNotifierPort notifier,
                                                     TransactionPort transactions, Clock clock, AppProperties properties) {
        return new PasswordResetService(users, passwordHasher, secrets, notifier, transactions, clock,
                properties.getSecurity().getResetTokenValidity(),
                properties.getSecurity().isExposeResetTokenInResponse());
    }

    @Bean
    public AccountService accountService(UserRepositoryPort users, PasswordHasherPort passwordHasher,
                                         TransactionPort transactions) {
        return new AccountService(users, passwordHasher, transactions);
    }

    @Bean
    public LabService labService(VirtualMachineRepositoryPort machines, HypervisorPort hypervisor,
                                 TransactionPort transactions, Clock clock) {
        return new LabService(machines, hypervisor, transactions, clock);
    }

    @Bean
    public ScoreboardService scoreboardService(BoxRepositoryPort boxes, OwnRepositoryPort owns) {
        return new ScoreboardService(boxes, owns);
    }

    @Bean
    public BoxService boxService(BoxRepositoryPort boxes, OwnRepositoryPort owns, BoxRatingRepositoryPort ratings,
                                 GetPlayerProgressUseCase progress, TransactionPort transactions, Clock clock) {
        return new BoxService(boxes, owns, ratings, progress, transactions, clock);
    }

    @Bean
    public AcademyService academyService(CourseRepositoryPort courses, SectionCompletionRepositoryPort completions,
                                         TransactionPort transactions, Clock clock) {
        return new AcademyService(courses, completions, transactions, clock);
    }

    @Bean
    public ProfileService profileService(BoxRepositoryPort boxes, OwnRepositoryPort owns, CourseRepositoryPort courses,
                                         SectionCompletionRepositoryPort completions) {
        return new ProfileService(boxes, owns, courses, completions);
    }

    @Bean
    public VpnCertificateAuthorityPort vpnCertificateAuthority(AppProperties properties, Clock clock) {
        AppProperties.Vpn vpn = properties.getVpn();
        // Créée à la demande : sans VPN activé, easy-rsa n'est jamais appelé.
        return new EasyRsaCertificateAuthority(
                new EasyRsaSettings(vpn.getEasyrsaBinary(), Path.of(vpn.getDirectory()), vpn.getCaName(),
                        vpn.getServerName(), vpn.getCommandTimeout(), vpn.getCrlRefresh()),
                new ProcessCommandRunner(), clock);
    }

    @Bean
    public VpnService vpnService(VpnProfileRepositoryPort profiles, VpnCertificateAuthorityPort authority,
                                 SecretGeneratorPort secrets, TransactionPort transactions, Clock clock,
                                 AppProperties properties) {
        AppProperties.Vpn vpn = properties.getVpn();
        List<VpnEndpoint> endpoints = new ArrayList<>();
        if (!vpn.getUdpHost().isBlank()) {
            endpoints.add(new VpnEndpoint(VpnProtocol.UDP, vpn.getUdpHost().trim(), vpn.getUdpPort()));
        }
        if (!vpn.getTcpHost().isBlank()) {
            endpoints.add(new VpnEndpoint(VpnProtocol.TCP, vpn.getTcpHost().trim(), vpn.getTcpPort()));
        }
        return new VpnService(profiles, authority, secrets, transactions, clock,
                new VpnSettings(vpn.isEnabled(), endpoints, vpn.getLabNetwork()));
    }
}
