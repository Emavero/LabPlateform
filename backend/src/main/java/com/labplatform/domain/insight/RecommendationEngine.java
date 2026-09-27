package com.labplatform.domain.insight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Traduit des chiffres d'usage en choses à faire.
 * <p>
 * Toutes les règles sont ici, explicites et déterministes : les mêmes chiffres
 * donnent toujours les mêmes conseils, et chacun porte le nombre qui le motive.
 * C'est volontaire — un tableau de bord qui conseille sans montrer sa preuve
 * n'est pas vérifiable, donc pas crédible.
 * <p>
 * Chaque seuil est une décision de produit, pas une constante technique : c'est
 * pourquoi ils sont nommés et commentés un par un.
 */
public final class RecommendationEngine {

    /** En dessous, les chiffres sont du bruit : trois comptes ne font pas une tendance. */
    private static final long MIN_USERS = 5;
    /** Un quiz manqué par plus de deux apprenants sur cinq accuse la section, pas les apprenants. */
    private static final double QUIZ_FAILURE_LIMIT = 0.4;
    /** Plus de deux flags refusés pour un validé : la machine bloque au mauvais endroit. */
    private static final double REFUSAL_RATIO_LIMIT = 2.0;
    /** Sous ce nombre de tentatives, un mauvais ratio ne veut rien dire. */
    private static final long MIN_ATTEMPTS = 6;
    /** Un parcours de paiement abandonné par plus de la moitié mérite un examen. */
    private static final double CHECKOUT_DROP_LIMIT = 0.5;
    /** Au-delà, la plateforme recrute des comptes qu'elle ne sert pas. */
    private static final int DORMANT_SHARE_LIMIT = 30;
    /** Une filière trois fois plus consultée qu'une autre est sous-servie. */
    private static final double TRACK_IMBALANCE = 3.0;
    /** Assez de monde devant une porte fermée pour que la porte compte. */
    private static final long LOCKED_OUT_LIMIT = 5;

    private RecommendationEngine() {
    }

    /** Les plus graves d'abord : c'est l'ordre dans lequel on veut les lire. */
    public static List<Recommendation> from(Signals signals) {
        List<Recommendation> found = new ArrayList<>();
        if (signals.users() < MIN_USERS) {
            found.add(Recommendation.of("not-enough-data", RecommendationSeverity.INFO,
                    "Trop peu de comptes pour conclure",
                    "Les indicateurs deviendront exploitables au-delà d'une poignée de comptes actifs.",
                    signals.users() + " compte(s)"));
            return found;
        }

        quizToRework(signals).ifPresent(found::add);
        boxWhereEveryoneIsStuck(signals).ifPresent(found::add);
        abandonedCheckout(signals).ifPresent(found::add);
        spawnedWithoutValidating(signals).ifPresent(found::add);
        dormantAccounts(signals).ifPresent(found::add);
        lockedOutDemand(signals).ifPresent(found::add);
        underservedTrack(signals).ifPresent(found::add);
        learnersWithoutMachines(signals).ifPresent(found::add);
        thinCatalogue(signals).ifPresent(found::add);

        found.sort(Comparator.comparingInt(recommendation -> recommendation.severity().ordinal()));
        return found;
    }

    /** Une section dont le quiz est massivement manqué est une section à reprendre. */
    private static java.util.Optional<Recommendation> quizToRework(Signals signals) {
        long attempts = signals.quizPassed() + signals.quizFailed();
        if (attempts < MIN_ATTEMPTS || signals.quizFailed() < attempts * QUIZ_FAILURE_LIMIT) {
            return java.util.Optional.empty();
        }
        Signals.Count worst = first(signals.failedQuizzes());
        int rate = (int) Math.round(signals.quizFailed() * 100.0 / attempts);
        return java.util.Optional.of(worst == null
                ? Recommendation.of("quiz-failure-rate", RecommendationSeverity.WARNING,
                        "Les quiz sont majoritairement manqués",
                        "Reprends les sections concernées : un quiz raté par la moitié des apprenants teste autre "
                                + "chose que ce que la section enseigne.",
                        rate + " % d'échec sur " + attempts + " tentatives")
                : Recommendation.about("quiz-failure-rate", RecommendationSeverity.WARNING, worst.subject(),
                        "Un quiz manqué par la plupart des apprenants",
                        "Reprends cette section : soit la question porte sur ce qui n'y est pas expliqué, soit "
                                + "l'explication arrive après la question.",
                        worst.count() + " échecs, " + rate + " % d'échec global"));
    }

    /** Beaucoup de refus pour peu de validations : la difficulté n'est pas là où elle est annoncée. */
    private static java.util.Optional<Recommendation> boxWhereEveryoneIsStuck(Signals signals) {
        Signals.Count worst = first(signals.stuckBoxes());
        if (worst == null || worst.count() < MIN_ATTEMPTS) {
            return java.util.Optional.empty();
        }
        long validated = Math.max(signals.flagsValidated(), 1);
        if (worst.count() < validated * REFUSAL_RATIO_LIMIT && signals.flagsValidated() > 0) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(Recommendation.about("box-refusals", RecommendationSeverity.WARNING,
                worst.subject(), "Une machine où les flags sont surtout refusés",
                "Vérifie que les flags déposés sur la cible sont bien ceux enregistrés, puis ajoute un indice : "
                        + "à ce niveau de refus, les joueurs cherchent au mauvais endroit.",
                worst.count() + " flags refusés"));
    }

    /** Des paiements engagés qui n'aboutissent pas : le problème est dans le parcours, pas dans l'envie. */
    private static java.util.Optional<Recommendation> abandonedCheckout(Signals signals) {
        if (signals.checkoutsStarted() < MIN_ATTEMPTS) {
            return java.util.Optional.empty();
        }
        long completed = signals.subscriptionsStarted();
        double dropped = 1.0 - (double) completed / signals.checkoutsStarted();
        if (dropped < CHECKOUT_DROP_LIMIT) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(Recommendation.of("checkout-drop", RecommendationSeverity.WARNING,
                "Des paiements engagés n'aboutissent pas",
                "Regarde les moyens de paiement proposés et les échecs déclarés : l'envie est là, c'est le "
                        + "parcours qui perd les gens.",
                (int) Math.round(dropped * 100) + " % d'abandon sur " + signals.checkoutsStarted()
                        + " paiements engagés, " + signals.paymentsFailed() + " refusés"));
    }

    /** Une cible lancée puis abandonnée sans un seul flag : il manque un accompagnement. */
    private static java.util.Optional<Recommendation> spawnedWithoutValidating(Signals signals) {
        if (signals.targetsSpawned() < MIN_ATTEMPTS || signals.flagsValidated() >= signals.targetsSpawned() / 2) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(Recommendation.of("spawn-without-flag", RecommendationSeverity.OPPORTUNITY,
                "Des cibles lancées sans aucun flag validé",
                "Ajoute une aide au démarrage sur les machines faciles — rappel du VPN, premier port à regarder : "
                        + "les joueurs arrivent sur la cible et repartent sans rien.",
                signals.targetsSpawned() + " cibles lancées pour " + signals.flagsValidated() + " flags validés"));
    }

    /** Des comptes recrutés puis jamais servis. */
    private static java.util.Optional<Recommendation> dormantAccounts(Signals signals) {
        long dormant = signals.profileCount(UsageProfile.DORMANT);
        int share = signals.shareOfUsers(dormant);
        if (share < DORMANT_SHARE_LIMIT) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(Recommendation.of("dormant-share", RecommendationSeverity.WARNING,
                "Beaucoup de comptes n'ont jamais rien fait",
                "Guide le premier pas depuis le tableau de bord : une machine d'initiation ouverte à tous et une "
                        + "première section de cours, plutôt qu'un catalogue entier.",
                share + " % des comptes sont dormants (" + dormant + ")"));
    }

    /** Des comptes gratuits qui se heurtent à une machine réservée : une demande chiffrée. */
    private static java.util.Optional<Recommendation> lockedOutDemand(Signals signals) {
        Signals.Count wanted = first(signals.lockedOutBoxes());
        if (wanted == null || wanted.count() < LOCKED_OUT_LIMIT) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(Recommendation.about("locked-out-demand", RecommendationSeverity.OPPORTUNITY,
                wanted.subject(), "Une machine réservée attire les comptes gratuits",
                "Ouvre une machine d'initiation du même genre, ou mets celle-ci en avant sur la page "
                        + "d'abonnement : la demande est déjà là.",
                wanted.count() + " tentatives de comptes sans abonnement"));
    }

    /** Une filière très consultée mais peu fournie. */
    private static java.util.Optional<Recommendation> underservedTrack(Signals signals) {
        if (signals.trackViews().size() < 2) {
            return java.util.Optional.empty();
        }
        Map.Entry<String, Long> most = signals.trackViews().entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow();
        Map.Entry<String, Long> least = signals.trackViews().entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .orElseThrow();
        if (least.getValue() == 0 || most.getValue() < least.getValue() * TRACK_IMBALANCE) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(Recommendation.about("track-imbalance", RecommendationSeverity.OPPORTUNITY,
                most.getKey(), "Une filière bien plus consultée que l'autre",
                "Publie la prochaine série de cours dans cette filière : c'est là que les comptes vont.",
                most.getValue() + " consultations contre " + least.getValue()));
    }

    /** Des apprenants qui n'ont jamais touché une machine : le pont manque. */
    private static java.util.Optional<Recommendation> learnersWithoutMachines(Signals signals) {
        long learners = signals.profileCount(UsageProfile.LEARNER);
        if (learners == 0 || learners <= signals.profileCount(UsageProfile.HUNTER)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(Recommendation.of("learners-without-machines", RecommendationSeverity.OPPORTUNITY,
                "Plus d'apprenants que de joueurs",
                "Termine chaque filière par une machine d'initiation qui applique ce qu'elle enseigne : ces "
                        + "comptes lisent, ils n'attaquent pas encore.",
                learners + " apprenants pour " + signals.profileCount(UsageProfile.HUNTER) + " joueurs"));
    }

    /** Pas assez de contenu pour retenir qui que ce soit. */
    private static java.util.Optional<Recommendation> thinCatalogue(Signals signals) {
        if (signals.boxesPublished() >= 6 && signals.coursesPublished() >= 4) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(Recommendation.of("thin-catalogue", RecommendationSeverity.INFO,
                "Le catalogue est encore mince",
                "Compte une machine par niveau de difficulté et deux cours par filière : en dessous, un compte "
                        + "assidu a tout fini en une semaine.",
                signals.boxesPublished() + " machines, " + signals.coursesPublished() + " cours"));
    }

    private static Signals.Count first(List<Signals.Count> counts) {
        return counts == null || counts.isEmpty() ? null : counts.get(0);
    }
}
