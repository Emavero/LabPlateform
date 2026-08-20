package com.labplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Point d'entrée de la plateforme de Labs.
 * <p>
 * Architecture modulaire par package métier (feature-based) :
 * - user     : gestion des comptes utilisateurs
 * - auth     : inscription / connexion / émission de JWT
 * - security : brique technique JWT (filtre + service de tokens)
 * - lab      : gestion des machines virtuelles (Labs)
 * - config   : configuration transverse (sécurité, propriétés applicatives)
 * - common   : éléments partagés (gestion d'erreurs, DTO génériques)
 * <p>
 * Chaque module communique avec les autres via des interfaces de service et
 * des événements Spring plutôt que des dépendances directes fortes, afin de
 * pouvoir ajouter de nouveaux modules (ex: quotas, facturation, notifications)
 * sans modifier le code existant.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LabPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(LabPlatformApplication.class, args);
    }
}
