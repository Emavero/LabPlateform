package com.labplatform.adapter.in.startup;

import com.labplatform.application.port.out.TransactionPort;
import com.labplatform.application.port.out.UserRepositoryPort;
import com.labplatform.config.AppProperties;
import com.labplatform.domain.user.Email;
import com.labplatform.domain.user.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Promeut au démarrage les comptes listés dans {@code app.security.admin-emails}.
 * <p>
 * Les comptes créés après coup sont promus à l'inscription ; celui-ci couvre
 * ceux qui existaient déjà. Le rôle ne s'obtient que par cette configuration :
 * aucune route ne l'accorde, pas même à un administrateur.
 */
@Component
public class AdminAccountPromoter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountPromoter.class);

    private final UserRepositoryPort users;
    private final TransactionPort transactions;
    private final AppProperties properties;

    public AdminAccountPromoter(UserRepositoryPort users, TransactionPort transactions, AppProperties properties) {
        this.users = users;
        this.transactions = transactions;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String raw : properties.getSecurity().getAdminEmails()) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            promote(raw.trim());
        }
    }

    private void promote(String raw) {
        Email email;
        try {
            email = Email.of(raw);
        } catch (RuntimeException invalid) {
            log.warn("Adresse d'administrateur ignorée, format invalide : {}", raw);
            return;
        }
        transactions.inTransaction(() -> users.findByEmail(email).ifPresent(user -> {
            if (user.getRole() != Role.ADMIN) {
                user.assignRole(Role.ADMIN);
                users.save(user);
                log.info("Compte promu administrateur : {}", email.value());
            }
        }));
    }
}
