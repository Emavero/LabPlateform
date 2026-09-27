package com.labplatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * Langue d'une requête.
 * <p>
 * Le français est la langue par défaut de la plateforme, et il faut le dire
 * explicitement : sans cela, une requête sans en-tête {@code Accept-Language}
 * hériterait de la locale de la machine qui exécute le serveur — un conteneur
 * en {@code en_US} rendrait toute l'API anglaise sans que personne ne l'ait
 * demandé.
 * <p>
 * Une langue que nous ne servons pas retombe sur le français plutôt que sur un
 * message vide.
 */
@Configuration
public class WebLocaleConfig {

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.FRENCH);
        resolver.setSupportedLocales(List.of(Locale.FRENCH, Locale.ENGLISH));
        return resolver;
    }
}
