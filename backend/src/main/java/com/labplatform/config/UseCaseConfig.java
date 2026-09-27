package com.labplatform.config;

import com.labplatform.application.port.in.billing.GetEffectivePlanUseCase;
import com.labplatform.application.port.in.scoring.GetPlayerProgressUseCase;
import com.labplatform.application.port.in.box.GetBoxUseCase;
import com.labplatform.application.port.out.BoxInstanceRepositoryPort;
import com.labplatform.application.port.out.BoxRatingRepositoryPort;
import com.labplatform.application.port.out.CourseRepositoryPort;
import com.labplatform.application.port.out.MediaAssetRepositoryPort;
import com.labplatform.application.port.out.MediaStoragePort;
import com.labplatform.application.port.out.QuizRepositoryPort;
import com.labplatform.application.port.out.WriteupRepositoryPort;
import com.labplatform.application.port.out.SectionCompletionRepositoryPort;
import com.labplatform.application.port.out.AccessTokenIssuerPort;
import com.labplatform.application.port.out.BoxRepositoryPort;
import com.labplatform.application.port.out.OwnRepositoryPort;
import com.labplatform.application.port.out.DomainEventPublisherPort;
import com.labplatform.application.port.out.HypervisorPort;
import com.labplatform.application.port.out.JournalPort;
import com.labplatform.application.port.out.PasswordHasherPort;
import com.labplatform.application.port.out.PasswordResetNotifierPort;
import com.labplatform.application.port.out.PaymentGatewayPort;
import com.labplatform.application.port.out.PaymentRepositoryPort;
import com.labplatform.application.port.out.SecretGeneratorPort;
import com.labplatform.application.port.out.SubscriptionRepositoryPort;
import com.labplatform.application.port.out.TicketRepositoryPort;
import com.labplatform.application.port.out.TransactionPort;
import com.labplatform.application.port.out.UserRepositoryPort;
import com.labplatform.application.port.out.VirtualMachineRepositoryPort;
import com.labplatform.application.service.AcademyService;
import com.labplatform.application.service.AdminAnalyticsService;
import com.labplatform.application.service.JournalService;
import com.labplatform.application.service.BillingService;
import com.labplatform.application.service.BillingSettings;
import com.labplatform.application.service.AccountService;
import com.labplatform.application.service.CourseAdminService;
import com.labplatform.application.service.ExposureService;
import com.labplatform.application.service.MediaService;
import com.labplatform.application.service.WriteupService;
import com.labplatform.application.service.BoxAdminService;
import com.labplatform.application.service.BoxInstanceService;
import com.labplatform.application.service.BoxService;
import com.labplatform.application.service.ProfileService;
import com.labplatform.application.service.ScoreboardService;
import com.labplatform.application.service.SupportService;
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
import java.util.Set;

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
                                                       TransactionPort transactions, Clock clock,
                                                       AppProperties properties) {
        return new AuthenticationService(users, passwordHasher, tokenIssuer, events, transactions, clock,
                Set.copyOf(properties.getSecurity().getAdminEmails()));
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
                                 JournalPort journal, TransactionPort transactions, Clock clock) {
        return new LabService(machines, hypervisor, journal, transactions, clock);
    }

    @Bean
    public ScoreboardService scoreboardService(BoxRepositoryPort boxes, OwnRepositoryPort owns) {
        return new ScoreboardService(boxes, owns);
    }

    @Bean
    public BillingService billingService(SubscriptionRepositoryPort subscriptions, PaymentRepositoryPort payments,
                                         PaymentGatewayPort gateway, SecretGeneratorPort secrets,
                                         JournalPort journal, TransactionPort transactions, Clock clock,
                                         BillingSettings settings) {
        return new BillingService(subscriptions, payments, gateway, secrets, journal, transactions, clock, settings);
    }

    @Bean
    public BoxService boxService(BoxRepositoryPort boxes, OwnRepositoryPort owns, BoxRatingRepositoryPort ratings,
                                 BoxInstanceRepositoryPort instances, GetPlayerProgressUseCase progress,
                                 GetEffectivePlanUseCase plans, JournalPort journal, TransactionPort transactions,
                                 Clock clock) {
        return new BoxService(boxes, owns, ratings, instances, progress, plans, journal, transactions, clock);
    }

    @Bean
    public BoxInstanceService boxInstanceService(BoxRepositoryPort boxes, BoxInstanceRepositoryPort instances,
                                                 HypervisorPort hypervisor, GetBoxUseCase boxView,
                                                 GetEffectivePlanUseCase plans, JournalPort journal,
                                                 TransactionPort transactions, Clock clock,
                                                 AppProperties properties) {
        return new BoxInstanceService(boxes, instances, hypervisor, boxView, plans, journal, transactions, clock,
                properties.getBoxes().getInstanceLifetime());
    }

    @Bean
    public AcademyService academyService(CourseRepositoryPort courses, SectionCompletionRepositoryPort completions,
                                         QuizRepositoryPort quizzes, JournalPort journal,
                                         TransactionPort transactions, Clock clock) {
        return new AcademyService(courses, completions, quizzes, journal, transactions, clock);
    }

    @Bean
    public BoxAdminService boxAdminService(BoxRepositoryPort boxes, SecretGeneratorPort secrets,
                                           TransactionPort transactions, Clock clock) {
        return new BoxAdminService(boxes, secrets, transactions, clock);
    }

    @Bean
    public CourseAdminService courseAdminService(CourseRepositoryPort courses,
                                                 SectionCompletionRepositoryPort completions,
                                                 QuizRepositoryPort quizzes, UserRepositoryPort users,
                                                 BoxRepositoryPort boxes, OwnRepositoryPort owns,
                                                 TransactionPort transactions, Clock clock) {
        return new CourseAdminService(courses, completions, quizzes, users, boxes, owns, transactions, clock);
    }

    @Bean
    public MediaService mediaService(MediaAssetRepositoryPort assets, MediaStoragePort storage,
                                     SecretGeneratorPort secrets, TransactionPort transactions, Clock clock,
                                     AppProperties properties) {
        return new MediaService(assets, storage, secrets, transactions, clock,
                properties.getMedia().getMaxFileSize().toBytes());
    }

    @Bean
    public WriteupService writeupService(BoxRepositoryPort boxes, OwnRepositoryPort owns,
                                         WriteupRepositoryPort writeups, UserRepositoryPort users,
                                         JournalPort journal, TransactionPort transactions, Clock clock) {
        return new WriteupService(boxes, owns, writeups, users, journal, transactions, clock);
    }

    @Bean
    public AdminAnalyticsService adminAnalyticsService(UserRepositoryPort users, BoxRepositoryPort boxes,
                                                      CourseRepositoryPort courses,
                                                      SubscriptionRepositoryPort subscriptions,
                                                      PaymentRepositoryPort payments, JournalPort journal,
                                                      Clock clock) {
        return new AdminAnalyticsService(users, boxes, courses, subscriptions, payments, journal, clock);
    }

    @Bean
    public JournalService journalService(JournalPort journal, UserRepositoryPort users) {
        return new JournalService(journal, users);
    }

    @Bean
    public ExposureService exposureService(BoxRepositoryPort boxes, OwnRepositoryPort owns, JournalPort journal,
                                           GetEffectivePlanUseCase plans, Clock clock) {
        return new ExposureService(boxes, owns, journal, plans, clock);
    }

    @Bean
    public SupportService supportService(TicketRepositoryPort tickets, UserRepositoryPort users, JournalPort journal,
                                         TransactionPort transactions, Clock clock) {
        return new SupportService(tickets, users, journal, transactions, clock);
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
                                 SecretGeneratorPort secrets, JournalPort journal, TransactionPort transactions,
                                 Clock clock, AppProperties properties) {
        AppProperties.Vpn vpn = properties.getVpn();
        List<VpnEndpoint> endpoints = new ArrayList<>();
        if (!vpn.getUdpHost().isBlank()) {
            endpoints.add(new VpnEndpoint(VpnProtocol.UDP, vpn.getUdpHost().trim(), vpn.getUdpPort()));
        }
        if (!vpn.getTcpHost().isBlank()) {
            endpoints.add(new VpnEndpoint(VpnProtocol.TCP, vpn.getTcpHost().trim(), vpn.getTcpPort()));
        }
        return new VpnService(profiles, authority, secrets, journal, transactions, clock,
                new VpnSettings(vpn.isEnabled(), endpoints, vpn.getLabNetwork()));
    }
}
