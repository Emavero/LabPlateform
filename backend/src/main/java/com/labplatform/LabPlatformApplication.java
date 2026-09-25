package com.labplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Point d'entrée. Architecture hexagonale (ports & adaptateurs) :
 * <ul>
 *   <li>domain      — entités et règles métier pures, aucune dépendance framework</li>
 *   <li>application — cas d'usage (ports entrants) et besoins externes (ports sortants)</li>
 *   <li>adapter.in  — REST, événements : pilotent l'application</li>
 *   <li>adapter.out — persistance JPA, JWT/BCrypt, hyperviseur : servent l'application</li>
 *   <li>config      — composition (câblage des cas d'usage) et sécurité HTTP</li>
 * </ul>
 * L'authentification est gérée par nos propres cas d'usage : l'auto-configuration
 * UserDetailsService de Spring Boot est donc désactivée.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class LabPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(LabPlatformApplication.class, args);
    }
}
