package com.labplatform.adapter.in.startup;

import com.labplatform.application.port.out.CourseRepositoryPort;
import com.labplatform.domain.academy.AttackPath;
import com.labplatform.domain.academy.AttackStage;
import com.labplatform.domain.academy.Course;
import com.labplatform.domain.academy.CourseBriefing;
import com.labplatform.domain.academy.CourseDesigner;
import com.labplatform.domain.academy.CourseLevel;
import com.labplatform.domain.academy.CourseTopic;
import com.labplatform.domain.academy.RealWorldCase;
import com.labplatform.domain.academy.SectionKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import com.labplatform.domain.academy.CourseSection;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Sème les cours au premier démarrage, une seule fois : une table déjà
 * peuplée n'est plus touchée, les sections cochées par les apprenants
 * restent donc valables.
 */
@Component
public class CourseCatalogSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CourseCatalogSeeder.class);

    private final CourseRepositoryPort courses;
    private final Clock clock;

    public CourseCatalogSeeder(CourseRepositoryPort courses, Clock clock) {
        this.courses = courses;
        this.clock = clock;
    }

    /**
     * Contenu d'une section réduit à un nombre : c'est l'identifiant d'un
     * « large object » PostgreSQL, laissé par une version où le contenu était
     * annoté @Lob. Ces lignes sont illisibles ; le catalogue est resemé.
     */
    private static final Pattern ORPHAN_LARGE_OBJECT = Pattern.compile("^\\d+$");

    @Override
    public void run(ApplicationArguments args) {
        if (courses.count() > 0) {
            if (!isCorrupted()) {
                return;
            }
            log.warn("Contenu des cours illisible (large objects orphelins) : le catalogue est resemé");
            courses.deleteAll();
        }
        Instant now = clock.instant();
        List<Course> catalogue = catalogue(now);
        catalogue.forEach(courses::save);
        log.info("Cours initialisés : {} cours", catalogue.size());
    }

    private boolean isCorrupted() {
        return courses.findAll().stream()
                .flatMap(course -> course.getSections().stream())
                .map(CourseSection::content)
                .anyMatch(content -> ORPHAN_LARGE_OBJECT.matcher(content.trim()).matches());
    }

    private static List<Course> catalogue(Instant now) {
        return List.of(forensicsFundamentals(now), forensicsMemory(now), forensicsTimeline(now),
                defenseHardening(now), defenseDetection(now), defenseResponse(now));
    }

    // ---------------------------------------------------------------- Forensique

    private static Course forensicsFundamentals(Instant now) {
        return Course.create("bases-de-l-investigation", "Bases de l'investigation numérique",
                CourseTopic.EVIDENCE_HANDLING, CourseLevel.FUNDAMENTAL,
                "Ce qu'il faut faire, et surtout ne pas faire, dans les premières minutes d'un incident : "
                        + "préserver les traces avant de chercher à comprendre.",
                now.minus(Duration.ofDays(40)),
                List.of(
                        CourseSection.of(null, "chaine-de-possession", "Chaîne de possession", SectionKind.THEORY, 1, 15,
                                """
                                Une preuve n'a de valeur que si l'on peut raconter tout ce qui lui est arrivé.
                                La chaîne de possession est ce récit : qui a saisi quoi, quand, où, avec quel
                                outil, et qui l'a détenu ensuite.

                                Trois règles tiennent l'essentiel. On note l'heure de chaque geste, avec le
                                décalage du fuseau. On calcule une empreinte (SHA-256) de chaque élément dès la
                                copie, et on la recalcule à chaque transfert : deux empreintes identiques
                                prouvent que rien n'a bougé. On travaille sur une copie, jamais sur l'original,
                                et on conserve l'original hors ligne.

                                Une preuve dont la chaîne est rompue n'est pas une preuve fausse : elle est
                                inutilisable, ce qui revient au même.
                                """),
                        CourseSection.of(null, "ordre-de-volatilite", "Ordre de volatilité", SectionKind.THEORY, 2, 20,
                                """
                                Les traces ne meurent pas à la même vitesse. On collecte donc du plus fugace au
                                plus durable : registres et cache du processeur, mémoire vive, état du réseau et
                                connexions établies, processus en cours, systèmes de fichiers temporaires, puis
                                les disques, et enfin les sauvegardes et les journaux distants.

                                Conséquence pratique : éteindre une machine « pour la préserver » détruit ce
                                qu'elle avait de plus précieux. Un débranchement fait perdre la mémoire vive,
                                donc les clés de chiffrement en clair, les processus injectés et les connexions
                                actives. Isoler du réseau, oui ; couper le courant, seulement après la capture
                                mémoire.

                                À l'inverse, s'acharner sur un disque pendant qu'un processus malveillant tourne
                                laisse l'attaquant agir. L'arbitrage se décide à l'avance, pas dans l'urgence.
                                """),
                        CourseSection.of(null, "copie-bit-a-bit", "Copie bit à bit d'un support",
                                SectionKind.LAB, 3, 25,
                                """
                                Objectif : produire une copie vérifiable d'un support, depuis votre machine
                                Linux du lab.

                                Préparez un support de test, puis copiez-le secteur par secteur :

                                    dd if=/dev/sdb of=piece-01.dd bs=4M conv=noerror,sync status=progress

                                Empreintes avant et après, qui doivent être identiques :

                                    sha256sum /dev/sdb | tee piece-01.source.sha256
                                    sha256sum piece-01.dd | tee piece-01.copie.sha256

                                Montez ensuite la copie en lecture seule, jamais en écriture :

                                    mount -o ro,noexec,nodev,noatime piece-01.dd /mnt/piece

                                `noatime` évite de réécrire les dates d'accès, `ro` protège la copie, et
                                `noexec` empêche d'exécuter par accident ce que vous analysez.
                                """),
                        CourseSection.of(null, "quiz-fondamentaux", "Quiz : réflexes de collecte",
                                SectionKind.QUIZ, 4, 10,
                                """
                                1. Un poste est soupçonné d'être compromis, il est allumé. Quel est le premier
                                   geste : capturer la mémoire, débrancher le réseau, ou éteindre ? Justifiez
                                   avec l'ordre de volatilité.

                                2. Vous obtenez deux empreintes différentes entre l'original et la copie. Que
                                   pouvez-vous encore affirmer, et que devez-vous refaire ?

                                3. Pourquoi monter une image en lecture seule ne suffit-il pas à garantir
                                   l'intégrité si vous n'avez pas calculé d'empreinte au préalable ?
                                """)),
                evidenceBriefing());
    }

    private static Course forensicsMemory(Instant now) {
        return Course.create("analyse-memoire", "Analyse de la mémoire vive", CourseTopic.MEMORY_ANALYSIS,
                CourseLevel.MEDIUM,
                "Capturer la mémoire d'un système suspect et y retrouver processus injectés, connexions "
                        + "et secrets restés en clair.",
                now.minus(Duration.ofDays(18)),
                List.of(
                        CourseSection.of(null, "capture", "Capturer sans altérer", SectionKind.THEORY, 1, 15,
                                """
                                ## Pourquoi l'altération est inévitable

                                Capturer la mémoire d'une machine allumée modifie fatalement cette mémoire :
                                l'outil de capture s'y trouve lui aussi. L'objectif n'est pas l'absence
                                d'altération, impossible, mais sa documentation.

                                ## Le geste, pas à pas

                                On écrit la capture sur un support externe, on note l'outil et sa version,
                                l'heure de début et de fin, et on calcule l'empreinte dès la fin du transfert.
                                Sous Linux, un module dédié (LiME) produit une image brute ; sous Windows, un
                                outil signé, lancé depuis un support en lecture seule.

                                ## Ce qui rend une capture exploitable

                                Une capture sans son fichier de journalisation vaut beaucoup moins : c'est lui
                                qui explique ce que l'on voit d'anormal dans l'image.
                                """),
                        CourseSection.of(null, "processus-et-injections", "Processus et injections",
                                SectionKind.THEORY, 2, 25,
                                """
                                Dans une image mémoire, on cherche d'abord des incohérences, pas des signatures.

                                ## Les signes qui comptent

                                Un processus dont le parent est mort ou incohérent (un navigateur lancé par un
                                tableur), un exécutable dont le chemin n'existe pas sur le disque, une zone
                                mémoire à la fois inscriptible et exécutable, une bibliothèque chargée depuis un
                                répertoire temporaire : chacun de ces signes se justifie parfois, mais leur
                                accumulation, jamais.

                                ## Pourquoi croire l'image plutôt que le système

                                La liste des processus obtenue depuis l'image est souvent plus fiable que celle
                                affichée par le système compromis, puisqu'un rootkit filtre la seconde et
                                rarement la première.
                                """),
                        CourseSection.of(null, "atelier-volatility", "Atelier : première passe sur une image",
                                SectionKind.LAB, 3, 30,
                                """
                                Sur une image de mémoire Linux ou Windows, enchaînez ces quatre questions.

                                Quels processus tournaient ?

                                    vol.py -f image.raw windows.pslist
                                    vol.py -f image.raw windows.pstree

                                Lesquels sont cachés à l'outil du système ?

                                    vol.py -f image.raw windows.psscan

                                Quelles connexions étaient ouvertes, et vers où ?

                                    vol.py -f image.raw windows.netscan

                                Quelles lignes de commande ont été lancées ?

                                    vol.py -f image.raw windows.cmdline

                                Notez chaque écart entre `pslist` et `psscan` : un processus visible par le
                                second et pas par le premier est, au minimum, à expliquer.
                                """),
                        CourseSection.of(null, "quiz-memoire", "Quiz : lecture d'une capture",
                                SectionKind.QUIZ, 4, 10,
                                """
                                1. `psscan` révèle un processus absent de `pslist`. Quelles hypothèses, et
                                   comment les distinguer ?

                                2. Une zone mémoire est marquée RWX dans un processus signé par l'éditeur du
                                   système. Est-ce concluant ? Que regardez-vous ensuite ?

                                3. Pourquoi une capture mémoire peut-elle contenir une clé de chiffrement de
                                   disque que le disque lui-même ne livrera jamais ?
                                """)),
                memoryBriefing());
    }

    private static Course forensicsTimeline(Instant now) {
        return Course.create("chronologie-d-incident", "Reconstituer la chronologie", CourseTopic.TIMELINE,
                CourseLevel.HARD,
                "Croiser journaux, horodatages de fichiers et artefacts système pour établir ce qui s'est "
                        + "passé, dans quel ordre, et par où c'est entré.",
                now.minus(Duration.ofDays(6)),
                List.of(
                        CourseSection.of(null, "sources-et-derives", "Sources et dérive des horloges",
                                SectionKind.THEORY, 1, 20,
                                """
                                Une chronologie mélange des sources qui ne partagent ni format, ni fuseau, ni
                                précision : journaux applicatifs à la seconde, journaux système à la
                                milliseconde, horodatages de fichiers parfois arrondis, équipements réseau dont
                                l'horloge dérive de plusieurs minutes.

                                Premier travail : ramener tout le monde en UTC et mesurer la dérive de chaque
                                source, en identifiant un événement présent dans deux journaux différents. Sans
                                cela, un enchaînement de causes peut apparaître inversé.

                                Second travail : distinguer l'heure où une chose s'est produite de l'heure où
                                elle a été écrite. Un journal envoyé par lots ment sur la seconde, pas sur
                                l'ordre.
                                """),
                        CourseSection.of(null, "artefacts-systeme", "Artefacts qui datent une action",
                                SectionKind.THEORY, 2, 25,
                                """
                                Les quatre horodatages d'un fichier (création, modification du contenu, accès,
                                modification des métadonnées) ne bougent pas pour les mêmes raisons. Un contenu
                                modifié après la date de création des métadonnées trahit souvent une tentative
                                de maquillage.

                                Au-delà du système de fichiers, beaucoup d'artefacts datent une action sans que
                                l'attaquant y pense : historique des interpréteurs de commandes, journaux
                                d'authentification, fichiers de service et tâches planifiées, caches de
                                navigateur, journaux du serveur web, artefacts d'exécution côté Windows.

                                Le recoupement fait la preuve : un même fait vu par trois sources
                                indépendantes n'est plus une hypothèse.
                                """),
                        CourseSection.of(null, "atelier-chronologie", "Atelier : d'un accès à la persistance",
                                SectionKind.LAB, 3, 35,
                                """
                                À partir d'une machine du catalogue que vous avez compromise, reconstituez ce
                                que vous avez fait, vu depuis la défense.

                                Journaux d'accès du service exposé :

                                    grep -iE 'POST|\\.php|\\.jsp' /var/log/nginx/access.log | tail -50

                                Authentifications réussies et échouées :

                                    journalctl -u ssh --since '-2 hours' -o short-iso

                                Fichiers récemment modifiés dans les répertoires servis :

                                    find /var/www -newermt '-2 hours' -type f -printf '%T+ %p\\n' | sort

                                Persistances les plus fréquentes :

                                    systemctl list-timers --all
                                    crontab -l; ls -la /etc/cron.*

                                Rangez vos trouvailles en une seule table (heure UTC, source, fait observé,
                                certitude) : c'est le livrable, pas les commandes.
                                """),
                        CourseSection.of(null, "quiz-chronologie", "Quiz : cohérence d'un récit",
                                SectionKind.QUIZ, 4, 10,
                                """
                                1. Le journal du serveur web date le dépôt d'un fichier à 14:02 UTC, mais la
                                   date de création du fichier est 13:58. Quelles explications, et laquelle
                                   testez-vous d'abord ?

                                2. Vous ne disposez que d'une source pour l'entrée initiale. Que pouvez-vous
                                   écrire dans le rapport, et sous quelle réserve ?

                                3. Pourquoi une tâche planifiée est-elle une meilleure preuve de persistance
                                   qu'un processus en cours d'exécution ?
                                """)),
                timelineBriefing());
    }

    // ------------------------------------------------------------------- Défense

    private static Course defenseHardening(Instant now) {
        return Course.create("durcissement-des-systemes", "Durcir un système exposé", CourseTopic.HARDENING,
                CourseLevel.FUNDAMENTAL,
                "Réduire la surface d'attaque d'un serveur : services, comptes, accès distants et mises à "
                        + "jour, dans l'ordre où cela paie le plus.",
                now.minus(Duration.ofDays(35)),
                List.of(
                        CourseSection.of(null, "surface-d-attaque", "Mesurer la surface d'attaque",
                                SectionKind.THEORY, 1, 15,
                                """
                                On ne durcit pas ce qu'on n'a pas inventorié. La surface d'attaque d'un serveur
                                se lit dans trois listes : les ports en écoute, les comptes qui peuvent
                                s'authentifier, et les logiciels installés avec leur version.

                                Chaque élément doit avoir une raison d'être, formulée en une phrase. Un service
                                dont personne ne sait à quoi il sert est un service à éteindre : sa valeur est
                                nulle et son risque ne l'est pas.

                                Le même raisonnement s'applique aux comptes. Un compte de service qui peut
                                ouvrir une session interactive, ou un compte d'un ancien prestataire, coûtent
                                plus qu'ils n'apportent.
                                """),
                        CourseSection.of(null, "atelier-durcissement", "Atelier : durcir votre machine Linux",
                                SectionKind.LAB, 2, 30,
                                """
                                Sur votre machine Linux du lab, dressez l'inventaire puis réduisez-le.

                                Ce qui écoute, et pour qui :

                                    ss -tulpen

                                Ce qui peut s'authentifier :

                                    awk -F: '$3 >= 1000 || $3 == 0 {print $1, $3, $7}' /etc/passwd

                                Durcissez l'accès distant dans `/etc/ssh/sshd_config` :

                                    PermitRootLogin no
                                    PasswordAuthentication no
                                    KbdInteractiveAuthentication no
                                    AllowUsers labuser

                                Puis vérifiez avant de recharger, pour ne pas vous enfermer dehors :

                                    sshd -t && systemctl reload ssh

                                Refaites l'inventaire et comparez : le livrable est l'écart entre les deux.
                                """),
                        CourseSection.of(null, "moindre-privilege", "Moindre privilège, en pratique",
                                SectionKind.THEORY, 3, 20,
                                """
                                Le moindre privilège n'est pas « retirer les droits » mais « accorder exactement
                                ce qui est nécessaire, et rien de plus, pour le temps nécessaire ».

                                Trois leviers dans cet ordre : faire tourner chaque service sous son propre
                                compte sans interpréteur de commandes, remplacer les droits permanents par une
                                élévation tracée et limitée à quelques commandes, et cloisonner ce qui reste
                                (conteneur, espaces de noms, capacités réduites plutôt que root).

                                Une règle `sudo` qui autorise un éditeur de texte ou un interpréteur équivaut à
                                donner root : ces programmes savent lancer un interpréteur. C'est l'erreur la
                                plus fréquente des chaînes d'élévation de privilèges.
                                """),
                        CourseSection.of(null, "quiz-durcissement", "Quiz : arbitrages de durcissement",
                                SectionKind.QUIZ, 4, 10,
                                """
                                1. Un service métier exige d'écouter sur toutes les interfaces. Que proposez-
                                   vous sans le casser ?

                                2. Pourquoi `sudo /usr/bin/vim` est-il équivalent à un accès root complet ?

                                3. Vous ne pouvez appliquer qu'une seule mesure ce soir sur cent serveurs
                                   exposés. Laquelle, et sur quel critère ?
                                """)),
                hardeningBriefing());
    }

    private static Course defenseDetection(Instant now) {
        return Course.create("detection-et-journaux", "Détecter : journaux et règles", CourseTopic.SIEM_SOC,
                CourseLevel.MEDIUM,
                "Centraliser les journaux utiles, écrire des règles qui se déclenchent sur des faits, et "
                        + "mesurer ce qu'elles coûtent en faux positifs.",
                now.minus(Duration.ofDays(14)),
                List.of(
                        CourseSection.of(null, "quoi-journaliser", "Ce qui vaut la peine d'être journalisé",
                                SectionKind.THEORY, 1, 20,
                                """
                                Tout journaliser revient à ne rien surveiller : le volume rend l'analyse
                                impossible et le stockage arbitre à votre place. On part donc des scénarios que
                                l'on veut détecter, et on remonte aux traces qu'ils laissent.

                                Quatre familles paient presque toujours : l'authentification (succès et échecs,
                                avec source), l'élévation de privilèges, les créations de persistance (services,
                                tâches planifiées, clés d'autorisation) et les connexions sortantes vers des
                                destinations inhabituelles.

                                Pour chaque source, fixez une durée de conservation et vérifiez qu'un attaquant
                                local ne peut pas l'effacer : un journal qui reste sur la machine compromise
                                n'est pas un journal, c'est un brouillon.
                                """),
                        CourseSection.of(null, "ecrire-une-regle", "Écrire une règle qui tient",
                                SectionKind.THEORY, 2, 25,
                                """
                                Une bonne règle décrit un fait, pas un outil. « Un processus du serveur web
                                lance un interpréteur de commandes » survit au changement d'outillage de
                                l'attaquant ; « le processus s'appelle nc.exe » ne survit pas à un renommage.

                                Une règle se juge sur trois nombres : ce qu'elle attrape, ce qu'elle laisse
                                passer, et ce qu'elle réveille pour rien. La troisième mesure décide de son
                                sort : une règle qui déclenche vingt fois par jour sans incident sera ignorée
                                en une semaine, donc elle ne protège plus rien.

                                Chaque règle porte aussi ce qu'il faut faire quand elle sonne. Sans cela, elle
                                produit de l'inquiétude, pas de la défense.
                                """),
                        CourseSection.of(null, "atelier-detection", "Atelier : de la trace à l'alerte",
                                SectionKind.LAB, 3, 30,
                                """
                                Rejouez vos propres actions d'attaque et voyez ce qu'elles laissent.

                                Échecs d'authentification groupés par source :

                                    journalctl -u ssh --since '-24 hours' | grep -i 'failed password' \\
                                      | awk '{print $(NF-3)}' | sort | uniq -c | sort -rn | head

                                Interpréteur lancé par le serveur web, le fait qui compte :

                                    ps -eo user,ppid,pid,comm --sort=ppid | awk '$1 ~ /www|nginx|apache/'

                                Persistances apparues récemment :

                                    find /etc/systemd /etc/cron.d -newermt '-24 hours' -type f

                                Pour chaque trace, écrivez la règle en une phrase, puis estimez à la main
                                combien de fois elle aurait sonné cette semaine sans incident.
                                """),
                        CourseSection.of(null, "quiz-detection", "Quiz : qualité d'une détection",
                                SectionKind.QUIZ, 4, 10,
                                """
                                1. Votre règle attrape 9 attaques sur 10 mais sonne 40 fois par jour à vide.
                                   Est-elle bonne ? Que changez-vous ?

                                2. Pourquoi une règle fondée sur un nom de processus vieillit-elle mal ?

                                3. Quel intérêt à conserver les journaux ailleurs que sur la machine
                                   surveillée, alors que c'est plus coûteux ?
                                """)),
                detectionBriefing());
    }

    private static Course defenseResponse(Instant now) {
        return Course.create("reponse-a-incident", "Répondre à un incident", CourseTopic.INCIDENT_RESPONSE,
                CourseLevel.HARD,
                "Contenir sans détruire les preuves, éradiquer la cause plutôt que le symptôme, et "
                        + "rétablir en sachant pourquoi cela ne recommencera pas.",
                now.minus(Duration.ofDays(3)),
                List.of(
                        CourseSection.of(null, "contenir", "Contenir sans effacer",
                                SectionKind.THEORY, 1, 20,
                                """
                                Contenir, c'est empêcher l'attaquant d'aller plus loin tout en gardant de quoi
                                comprendre. Les deux objectifs s'opposent : chaque minute gagnée pour l'analyse
                                est une minute laissée à l'adversaire.

                                L'isolation réseau est presque toujours le bon premier geste : elle coupe le
                                canal de commande sans rien détruire, et laisse la mémoire intacte pour la
                                capture. Couper l'alimentation, au contraire, détruit ce que l'analyse aurait
                                exploité.

                                Décidez aussi ce que vous ne faites pas tout de suite : changer tous les mots
                                de passe avant d'avoir compris l'entrée initiale prévient l'attaquant et
                                l'incite à activer ses accès de secours.
                                """),
                        CourseSection.of(null, "eradiquer", "Éradiquer la cause, pas le symptôme",
                                SectionKind.THEORY, 2, 25,
                                """
                                Supprimer un fichier malveillant ne répond pas à la question qui compte :
                                comment est-il arrivé là, et qu'a-t-il fait entre-temps ?

                                Une éradication se juge sur trois points : l'entrée initiale est fermée
                                (vulnérabilité corrigée, identifiant révoqué), toutes les persistances sont
                                trouvées et retirées, et les secrets exposés sont renouvelés — clés, jetons,
                                mots de passe de service, y compris ceux qu'un attaquant aurait pu lire en
                                mémoire.

                                Sur un système dont la compromission a atteint l'administration, la
                                réinstallation depuis une source saine est souvent plus rapide et plus sûre que
                                le nettoyage. Le nettoyage laisse un doute ; le doute coûte plus cher que la
                                réinstallation.
                                """),
                        CourseSection.of(null, "atelier-post-mortem", "Atelier : rapport de fin d'incident",
                                SectionKind.LAB, 3, 30,
                                """
                                Rédigez le rapport d'une machine du catalogue que vous avez compromise, vu
                                depuis la défense. Quatre parties, deux pages au total.

                                Ce qui s'est passé : la chronologie en UTC, une ligne par fait établi, avec sa
                                source. Pas d'hypothèse dans cette partie.

                                Comment c'est entré : l'entrée initiale et la manière dont elle a permis
                                l'élévation. Dites ce que vous savez et ce que vous supposez.

                                Ce que cela a permis : données atteintes, comptes exposés, rebonds possibles.

                                Ce qui change : les mesures, chacune rattachée à un fait de la chronologie, et
                                pour chacune la détection qui aurait dû sonner. Une mesure qui ne se rattache à
                                aucun fait est une mesure de confort.
                                """),
                        CourseSection.of(null, "quiz-reponse", "Quiz : décisions sous pression",
                                SectionKind.QUIZ, 4, 10,
                                """
                                1. Un serveur de production est compromis, l'activité de l'entreprise en
                                   dépend. Isoler, surveiller, ou réinstaller ? Sur quels critères tranchez-
                                   vous, et qui décide ?

                                2. Vous avez retiré le logiciel malveillant et il revient deux jours plus tard.
                                   Qu'a-t-on manqué, et où cherchez-vous ?

                                3. Pourquoi renouveler les secrets même sans preuve qu'ils ont été lus ?
                                """)),
                responseBriefing());
    }

    // ------------------------------------------------- Dossiers des cours
    //
    // Chaque cours porte un chemin d'attaque, un cas d'usage réel et ses
    // concepteurs. C'est du contenu, pas de la configuration : il est écrit ici
    // comme le sont les sections, et l'éditeur d'administration le reprend.
    //
    // Les avatars sont laissés vides : la page présente alors un concepteur par
    // ses initiales. Un administrateur peut y mettre une adresse d'image.

    private static CourseBriefing evidenceBriefing() {
        return new CourseBriefing(
                AttackPath.of("Une pièce jointe ouverte sur un poste de bureau, puis un attaquant qui s'installe et "
                                + "efface derrière lui. Le cours remonte cette chaîne à l'envers : ce qui a été "
                                + "touché, dans quel ordre, et ce qu'il en reste de prouvable.",
                        List.of(
                                stage(1, "Hameçonnage ciblé",
                                        "Un devis en pièce jointe, au nom d'un fournisseur connu. Le document "
                                                + "demande l'activation des macros pour « afficher les montants ».",
                                        "T1566.001 — Pièce jointe malveillante"),
                                stage(2, "Exécution de la charge",
                                        "La macro télécharge et lance un implant en mémoire. Rien n'est écrit sur "
                                                + "le disque à ce stade : c'est ce qui rend la capture mémoire "
                                                + "décisive avant tout redémarrage.",
                                        "T1059.005 — Interpréteur de commandes"),
                                stage(3, "Persistance",
                                        "Une tâche planifiée et une clé de registre relancent l'implant à chaque "
                                                + "ouverture de session. Deux points d'ancrage plutôt qu'un, pour "
                                                + "survivre au retrait du premier.",
                                        "T1053.005 — Tâche planifiée"),
                                stage(4, "Effacement des traces",
                                        "Les journaux de sécurité sont vidés et les horodatages des fichiers "
                                                + "déposés sont recopiés sur ceux d'un binaire système. L'absence "
                                                + "de trace devient elle-même une trace.",
                                        "T1070.001 — Suppression des journaux"))),
                RealWorldCase.of("Cabinet comptable, 40 postes",
                        "Un vendredi de clôture, une comptable signale que son poste « rame » depuis l'ouverture "
                                + "d'un devis reçu le matin. Le prestataire informatique, appelé en renfort, "
                                + "commence par redémarrer la machine puis lance un antivirus : la mémoire est "
                                + "perdue, et avec elle l'implant qui n'existait que là.",
                        "Les déclarations de 300 clients sont sur ce poste, et l'obligation de notification court à "
                                + "partir du moment où la compromission est connue. Sans preuve de ce qui a été "
                                + "consulté, c'est la totalité du portefeuille qu'il faut déclarer.",
                        "La chaîne de possession, tenue à partir du deuxième poste touché, a permis de délimiter "
                                + "l'accès à un seul répertoire. La notification a porté sur onze clients au lieu de "
                                + "trois cents."),
                List.of(
                        designer(1, "Awa Diallo", "Analyste forensique, réponse à incident"),
                        designer(2, "Marc Lefèvre", "Expert judiciaire près la cour d'appel")));
    }

    private static CourseBriefing memoryBriefing() {
        return new CourseBriefing(
                AttackPath.of("Un attaquant qui ne touche pas au disque : sa charge vit dans le processus d'un "
                                + "logiciel légitime. Tout ce qui le démasque est en mémoire, et disparaît à "
                                + "l'extinction.",
                        List.of(
                                stage(1, "Accès initial par service exposé",
                                        "Un service d'accès distant accepte encore un mot de passe réutilisé "
                                                + "ailleurs. Aucun fichier n'est déposé : la session suffit.",
                                        "T1078 — Comptes valides"),
                                stage(2, "Injection dans un processus de confiance",
                                        "Le code est écrit dans l'espace mémoire d'un processus signé, qui garde "
                                                + "son nom et sa signature. La liste des processus ne montre rien "
                                                + "d'anormal.",
                                        "T1055 — Injection de processus"),
                                stage(3, "Récolte de secrets en clair",
                                        "Mots de passe, jetons de session et clés privées sont lus dans la mémoire "
                                                + "d'autres processus, là où le chiffrement du disque ne protège "
                                                + "plus rien.",
                                        "T1003 — Vol d'identifiants"),
                                stage(4, "Canal de commande discret",
                                        "Les instructions passent par des requêtes HTTPS vers un domaine "
                                                + "récemment enregistré, au rythme d'une toutes les dix minutes.",
                                        "T1071.001 — Protocole applicatif"))),
                RealWorldCase.of("Éditeur de logiciel, 250 salariés",
                        "Le centre de supervision remonte une alerte faible : un processus système ouvre une "
                                + "connexion sortante vers un domaine créé la semaine précédente. Le poste est sain "
                                + "pour l'antivirus, aucun fichier suspect n'est trouvé. La capture mémoire, prise "
                                + "avant l'isolement, contient l'implant entier et la liste des dépôts de code "
                                + "clonés.",
                        "Le code source du produit et les clés de signature des mises à jour sont sur le réseau "
                                + "atteint. Une signature volée permettrait de livrer un logiciel malveillant à "
                                + "toute la base de clients.",
                        "Les chaînes retrouvées en mémoire ont donné les dépôts consultés et l'heure exacte du "
                                + "dernier accès. Les clés de signature, sur un support hors ligne, n'avaient pas "
                                + "été atteintes — ce qui a évité le rappel de toutes les mises à jour."),
                List.of(
                        designer(1, "Awa Diallo", "Analyste forensique, réponse à incident"),
                        designer(2, "Ibrahim Touré", "Ingénieur rétro-ingénierie")));
    }

    private static CourseBriefing timelineBriefing() {
        return new CourseBriefing(
                AttackPath.of("Une intrusion étalée sur six semaines, dont chaque geste est isolément banal. C'est "
                                + "l'ordre des gestes qui la dénonce, et cet ordre ne se lit que sur une "
                                + "chronologie unique.",
                        List.of(
                                stage(1, "Reconnaissance lente",
                                        "Des connexions au portail extranet, une par jour, depuis des adresses "
                                                + "différentes. Chacune passe sous le seuil d'alerte.",
                                        "T1595 — Balayage actif"),
                                stage(2, "Premier accès et attente",
                                        "Un compte de prestataire est utilisé une nuit, puis plus rien pendant "
                                                + "onze jours. L'attente est délibérée : elle sort de la fenêtre de "
                                                + "corrélation des règles.",
                                        "T1078.004 — Comptes dans le nuage"),
                                stage(3, "Déplacement latéral",
                                        "Trois sauts en deux heures, chacun avec un compte différent, vers le "
                                                + "serveur de sauvegarde. Les journaux existent, mais sur trois "
                                                + "machines aux horloges décalées de plusieurs minutes.",
                                        "T1021 — Services distants"),
                                stage(4, "Effacement sélectif",
                                        "Seules les entrées de la nuit du premier accès sont retirées. Le trou "
                                                + "dans la séquence des identifiants d'événement est ce qui "
                                                + "trahit la manipulation.",
                                        "T1070 — Effacement d'indicateurs"))),
                RealWorldCase.of("Collectivité territoriale, 1 200 agents",
                        "Les sauvegardes sont chiffrées un lundi matin. La question posée à l'équipe n'est pas "
                                + "« comment » mais « depuis quand » : tant que la date d'entrée n'est pas établie, "
                                + "aucune sauvegarde ne peut être déclarée saine, donc aucune ne peut être "
                                + "restaurée.",
                        "Trois semaines d'état civil et de facturation d'eau dépendent du choix de la sauvegarde. "
                                + "Restaurer une image déjà compromise, c'est remettre l'attaquant en place avec "
                                + "les données.",
                        "La chronologie croisée — journaux de pare-feu, horodatages NTFS, historique du "
                                + "contrôleur de domaine — a daté le premier accès à quarante et un jours. La "
                                + "sauvegarde du quarante-cinquième jour a été retenue, et vérifiée avant "
                                + "remontée."),
                List.of(
                        designer(1, "Marc Lefèvre", "Expert judiciaire près la cour d'appel"),
                        designer(2, "Sofia Benali", "Responsable de la réponse à incident")));
    }

    private static CourseBriefing hardeningBriefing() {
        return new CourseBriefing(
                AttackPath.of("La chaîne que le durcissement casse, et à quel maillon. Chaque étape ci-dessous "
                                + "correspond à une porte que ce cours apprend à fermer, dans l'ordre où cela "
                                + "paie le plus.",
                        List.of(
                                stage(1, "Découverte du service oublié",
                                        "Un balayage d'Internet trouve une interface d'administration laissée "
                                                + "ouverte après une migration. Elle ne figure dans aucun "
                                                + "inventaire.",
                                        "T1595.002 — Balayage de vulnérabilités"),
                                stage(2, "Authentification par force brute",
                                        "Le compte par défaut n'a pas été renommé et aucune limitation de tentatives "
                                                + "n'est en place. Quatre mille essais passent en une heure.",
                                        "T1110 — Force brute"),
                                stage(3, "Élévation locale",
                                        "Un noyau non mis à jour depuis onze mois donne les droits "
                                                + "d'administration. Le correctif existait depuis neuf mois.",
                                        "T1068 — Exploitation pour élévation"),
                                stage(4, "Installation d'un accès permanent",
                                        "Une clé publique est ajoutée au compte de service, et le service de mise "
                                                + "à jour automatique est désactivé pour que la porte reste "
                                                + "ouverte.",
                                        "T1098.004 — Ajout de clés autorisées"))),
                RealWorldCase.of("Commerce en ligne, 60 salariés",
                        "Un audit avant renouvellement d'assurance recense les machines exposées. Il en trouve "
                                + "dix-sept, là où l'équipe en déclarait neuf : deux serveurs de recette montés "
                                + "pour une démonstration, jamais éteints, avec une copie de la base de "
                                + "production.",
                        "La base contient les commandes et les adresses de livraison de 80 000 clients. L'écart "
                                + "entre l'inventaire déclaré et la réalité est, à lui seul, un motif de refus de "
                                + "garantie.",
                        "Les deux serveurs ont été retirés d'Internet le jour même. Le durcissement du reste — "
                                + "services inutiles coupés, accès distant derrière un point d'entrée unique, "
                                + "correctifs remis à jour — a ramené la surface exposée de dix-sept services à "
                                + "quatre."),
                List.of(
                        designer(1, "Sofia Benali", "Responsable de la réponse à incident"),
                        designer(2, "Yao Kouassi", "Administrateur systèmes et durcissement")));
    }

    private static CourseBriefing detectionBriefing() {
        return new CourseBriefing(
                AttackPath.of("Une attaque qui se déroule entièrement dans le champ des journaux existants, et que "
                                + "personne ne voit : les faits étaient enregistrés, aucune règle ne les "
                                + "rapprochait.",
                        List.of(
                                stage(1, "Pulvérisation de mots de passe",
                                        "Un seul mot de passe courant, essayé sur huit cents comptes. Chaque "
                                                + "compte n'enregistre qu'un échec : sous le seuil de "
                                                + "verrouillage, et sous celui de l'alerte.",
                                        "T1110.003 — Pulvérisation de mots de passe"),
                                stage(2, "Connexion réussie hors horaires",
                                        "Un compte de la comptabilité se connecte à 3 h 20, depuis un pays où "
                                                + "l'entreprise n'a pas d'activité. L'événement est journalisé, et "
                                                + "noyé dans quarante mille lignes.",
                                        "T1078 — Comptes valides"),
                                stage(3, "Règle de boîte aux lettres",
                                        "Une règle déplace vers un dossier archivé tout message contenant "
                                                + "« virement » ou « IBAN ». Le titulaire ne verra plus passer les "
                                                + "demandes de confirmation.",
                                        "T1564.008 — Règles de messagerie"),
                                stage(4, "Fraude au virement",
                                        "Un faux changement de coordonnées bancaires est envoyé à un client, "
                                                + "depuis la boîte légitime, dans un fil de discussion réel.",
                                        "T1534 — Hameçonnage interne"))),
                RealWorldCase.of("Industrie mécanique, 400 salariés",
                        "Un client appelle pour confirmer un changement d'IBAN que personne n'a demandé. Les "
                                + "journaux de la messagerie contenaient, depuis dix jours, la connexion nocturne "
                                + "et la création de la règle — mais aucune règle de détection ne portait sur ces "
                                + "deux événements.",
                        "Trois virements sont déjà partis, pour 240 000 euros. Le délai de rappel bancaire se "
                                + "compte en heures, pas en jours : chaque heure passée à chercher dans les "
                                + "journaux est une heure perdue pour le rappel.",
                        "Deux règles ont été écrites après coup : création de règle de messagerie par un compte "
                                + "non administrateur, et connexion réussie depuis un pays jamais vu pour ce "
                                + "compte. Elles ont depuis déclenché deux fois, dont une vraie tentative, "
                                + "arrêtée en vingt minutes."),
                List.of(
                        designer(1, "Yao Kouassi", "Administrateur systèmes et durcissement"),
                        designer(2, "Claire Mendy", "Analyste SOC, ingénierie de détection")));
    }

    private static CourseBriefing responseBriefing() {
        return new CourseBriefing(
                AttackPath.of("Un rançongiciel déployé par un opérateur qui est entré trois semaines plus tôt. Ce "
                                + "cours suit la chaîne du point de vue de celui qui doit l'arrêter pendant "
                                + "qu'elle se déroule.",
                        List.of(
                                stage(1, "Accès acheté à un courtier",
                                        "L'opérateur n'a pas conduit l'intrusion initiale : il a acheté un accès "
                                                + "déjà en place, par un implant installé un mois plus tôt.",
                                        "T1133 — Services distants externes"),
                                stage(2, "Prise du contrôleur de domaine",
                                        "Un compte d'administration de sauvegarde, membre des administrateurs du "
                                                + "domaine, donne la main sur l'annuaire entier en une nuit.",
                                        "T1078.002 — Comptes de domaine"),
                                stage(3, "Destruction des sauvegardes",
                                        "Les points de restauration et les copies locales sont supprimés avant "
                                                + "tout chiffrement. C'est l'étape qui décide du rapport de force, "
                                                + "et elle précède l'attaque visible.",
                                        "T1490 — Blocage de la restauration"),
                                stage(4, "Chiffrement et double extorsion",
                                        "Les fichiers sont chiffrés un samedi à 2 h, après exfiltration de "
                                                + "quarante gigaoctets. La menace de publication vaut autant que "
                                                + "le chiffrement.",
                                        "T1486 — Données chiffrées pour impact"))),
                RealWorldCase.of("Clinique privée, 180 lits",
                        "Le dossier patient informatisé est inaccessible à 6 h du matin ; les blocs opératoires "
                                + "ouvrent à 7 h 30. L'équipe doit décider, sans savoir jusqu'où l'attaquant est "
                                + "allé, si l'on isole tout le réseau — ce qui arrête aussi l'imagerie et les "
                                + "pompes connectées.",
                        "Le passage au dossier papier est tenable une demi-journée, pas une semaine. Isoler trop "
                                + "large arrête des soins ; isoler trop peu laisse le chiffrement se propager aux "
                                + "services encore sains.",
                        "Le confinement par segment, décidé en quarante minutes sur la base des seuls "
                                + "identifiants de comptes compromis, a préservé l'imagerie et la pharmacie. La "
                                + "reprise a pris six jours, sans rançon versée : une sauvegarde hors ligne, "
                                + "hebdomadaire, avait échappé à l'étape 3."),
                List.of(
                        designer(1, "Sofia Benali", "Responsable de la réponse à incident"),
                        designer(2, "Claire Mendy", "Analyste SOC, ingénierie de détection"),
                        designer(3, "Marc Lefèvre", "Expert judiciaire près la cour d'appel")));
    }

    private static AttackStage stage(int position, String name, String description, String technique) {
        return AttackStage.of(null, position, name, description, technique);
    }

    /** Sans avatar : la page présente le concepteur par ses initiales. */
    private static CourseDesigner designer(int position, String name, String role) {
        return CourseDesigner.of(null, position, name, role, null);
    }
}
