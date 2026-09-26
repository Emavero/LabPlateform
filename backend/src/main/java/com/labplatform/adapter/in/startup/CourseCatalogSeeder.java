package com.labplatform.adapter.in.startup;

import com.labplatform.application.port.out.CourseRepositoryPort;
import com.labplatform.domain.academy.Course;
import com.labplatform.domain.academy.CourseLevel;
import com.labplatform.domain.academy.CourseSection;
import com.labplatform.domain.academy.SectionKind;
import com.labplatform.domain.academy.Track;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

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

    @Override
    public void run(ApplicationArguments args) {
        if (courses.count() > 0) {
            return;
        }
        Instant now = clock.instant();
        catalogue(now).forEach(courses::save);
        log.info("Cours initialisés : {} cours", catalogue(now).size());
    }

    private static List<Course> catalogue(Instant now) {
        return List.of(forensicsFundamentals(now), forensicsMemory(now), forensicsTimeline(now),
                defenseHardening(now), defenseDetection(now), defenseResponse(now));
    }

    // ---------------------------------------------------------------- Forensique

    private static Course forensicsFundamentals(Instant now) {
        return Course.create("bases-de-l-investigation", "Bases de l'investigation numérique", Track.FORENSICS,
                CourseLevel.FUNDAMENTAL,
                "Ce qu'il faut faire, et surtout ne pas faire, dans les premières minutes d'un incident : "
                        + "préserver les traces avant de chercher à comprendre.",
                now.minus(Duration.ofDays(40)),
                List.of(
                        new CourseSection(null, "chaine-de-possession", "Chaîne de possession", SectionKind.THEORY, 1, 15,
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
                        new CourseSection(null, "ordre-de-volatilite", "Ordre de volatilité", SectionKind.THEORY, 2, 20,
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
                        new CourseSection(null, "copie-bit-a-bit", "Copie bit à bit d'un support",
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
                        new CourseSection(null, "quiz-fondamentaux", "Quiz : réflexes de collecte",
                                SectionKind.QUIZ, 4, 10,
                                """
                                1. Un poste est soupçonné d'être compromis, il est allumé. Quel est le premier
                                   geste : capturer la mémoire, débrancher le réseau, ou éteindre ? Justifiez
                                   avec l'ordre de volatilité.

                                2. Vous obtenez deux empreintes différentes entre l'original et la copie. Que
                                   pouvez-vous encore affirmer, et que devez-vous refaire ?

                                3. Pourquoi monter une image en lecture seule ne suffit-il pas à garantir
                                   l'intégrité si vous n'avez pas calculé d'empreinte au préalable ?
                                """))); 
    }

    private static Course forensicsMemory(Instant now) {
        return Course.create("analyse-memoire", "Analyse de la mémoire vive", Track.FORENSICS, CourseLevel.MEDIUM,
                "Capturer la mémoire d'un système suspect et y retrouver processus injectés, connexions "
                        + "et secrets restés en clair.",
                now.minus(Duration.ofDays(18)),
                List.of(
                        new CourseSection(null, "capture", "Capturer sans altérer", SectionKind.THEORY, 1, 15,
                                """
                                Capturer la mémoire d'une machine allumée modifie fatalement cette mémoire :
                                l'outil de capture s'y trouve lui aussi. L'objectif n'est pas l'absence
                                d'altération, impossible, mais sa documentation.

                                On écrit la capture sur un support externe, on note l'outil et sa version,
                                l'heure de début et de fin, et on calcule l'empreinte dès la fin du transfert.
                                Sous Linux, un module dédié (LiME) produit une image brute ; sous Windows, un
                                outil signé, lancé depuis un support en lecture seule.

                                Une capture sans son fichier de journalisation vaut beaucoup moins : c'est lui
                                qui explique ce que l'on voit d'anormal dans l'image.
                                """),
                        new CourseSection(null, "processus-et-injections", "Processus et injections",
                                SectionKind.THEORY, 2, 25,
                                """
                                Dans une image mémoire, on cherche d'abord des incohérences, pas des signatures.

                                Un processus dont le parent est mort ou incohérent (un navigateur lancé par un
                                tableur), un exécutable dont le chemin n'existe pas sur le disque, une zone
                                mémoire à la fois inscriptible et exécutable, une bibliothèque chargée depuis un
                                répertoire temporaire : chacun de ces signes se justifie parfois, mais leur
                                accumulation, jamais.

                                La liste des processus obtenue depuis l'image est souvent plus fiable que celle
                                affichée par le système compromis, puisqu'un rootkit filtre la seconde et
                                rarement la première.
                                """),
                        new CourseSection(null, "atelier-volatility", "Atelier : première passe sur une image",
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
                        new CourseSection(null, "quiz-memoire", "Quiz : lecture d'une capture",
                                SectionKind.QUIZ, 4, 10,
                                """
                                1. `psscan` révèle un processus absent de `pslist`. Quelles hypothèses, et
                                   comment les distinguer ?

                                2. Une zone mémoire est marquée RWX dans un processus signé par l'éditeur du
                                   système. Est-ce concluant ? Que regardez-vous ensuite ?

                                3. Pourquoi une capture mémoire peut-elle contenir une clé de chiffrement de
                                   disque que le disque lui-même ne livrera jamais ?
                                """)));
    }

    private static Course forensicsTimeline(Instant now) {
        return Course.create("chronologie-d-incident", "Reconstituer la chronologie", Track.FORENSICS,
                CourseLevel.HARD,
                "Croiser journaux, horodatages de fichiers et artefacts système pour établir ce qui s'est "
                        + "passé, dans quel ordre, et par où c'est entré.",
                now.minus(Duration.ofDays(6)),
                List.of(
                        new CourseSection(null, "sources-et-derives", "Sources et dérive des horloges",
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
                        new CourseSection(null, "artefacts-systeme", "Artefacts qui datent une action",
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
                        new CourseSection(null, "atelier-chronologie", "Atelier : d'un accès à la persistance",
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
                        new CourseSection(null, "quiz-chronologie", "Quiz : cohérence d'un récit",
                                SectionKind.QUIZ, 4, 10,
                                """
                                1. Le journal du serveur web date le dépôt d'un fichier à 14:02 UTC, mais la
                                   date de création du fichier est 13:58. Quelles explications, et laquelle
                                   testez-vous d'abord ?

                                2. Vous ne disposez que d'une source pour l'entrée initiale. Que pouvez-vous
                                   écrire dans le rapport, et sous quelle réserve ?

                                3. Pourquoi une tâche planifiée est-elle une meilleure preuve de persistance
                                   qu'un processus en cours d'exécution ?
                                """)));
    }

    // ------------------------------------------------------------------- Défense

    private static Course defenseHardening(Instant now) {
        return Course.create("durcissement-des-systemes", "Durcir un système exposé", Track.DEFENSE,
                CourseLevel.FUNDAMENTAL,
                "Réduire la surface d'attaque d'un serveur : services, comptes, accès distants et mises à "
                        + "jour, dans l'ordre où cela paie le plus.",
                now.minus(Duration.ofDays(35)),
                List.of(
                        new CourseSection(null, "surface-d-attaque", "Mesurer la surface d'attaque",
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
                        new CourseSection(null, "atelier-durcissement", "Atelier : durcir votre machine Linux",
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
                        new CourseSection(null, "moindre-privilege", "Moindre privilège, en pratique",
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
                        new CourseSection(null, "quiz-durcissement", "Quiz : arbitrages de durcissement",
                                SectionKind.QUIZ, 4, 10,
                                """
                                1. Un service métier exige d'écouter sur toutes les interfaces. Que proposez-
                                   vous sans le casser ?

                                2. Pourquoi `sudo /usr/bin/vim` est-il équivalent à un accès root complet ?

                                3. Vous ne pouvez appliquer qu'une seule mesure ce soir sur cent serveurs
                                   exposés. Laquelle, et sur quel critère ?
                                """)));
    }

    private static Course defenseDetection(Instant now) {
        return Course.create("detection-et-journaux", "Détecter : journaux et règles", Track.DEFENSE,
                CourseLevel.MEDIUM,
                "Centraliser les journaux utiles, écrire des règles qui se déclenchent sur des faits, et "
                        + "mesurer ce qu'elles coûtent en faux positifs.",
                now.minus(Duration.ofDays(14)),
                List.of(
                        new CourseSection(null, "quoi-journaliser", "Ce qui vaut la peine d'être journalisé",
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
                        new CourseSection(null, "ecrire-une-regle", "Écrire une règle qui tient",
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
                        new CourseSection(null, "atelier-detection", "Atelier : de la trace à l'alerte",
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
                        new CourseSection(null, "quiz-detection", "Quiz : qualité d'une détection",
                                SectionKind.QUIZ, 4, 10,
                                """
                                1. Votre règle attrape 9 attaques sur 10 mais sonne 40 fois par jour à vide.
                                   Est-elle bonne ? Que changez-vous ?

                                2. Pourquoi une règle fondée sur un nom de processus vieillit-elle mal ?

                                3. Quel intérêt à conserver les journaux ailleurs que sur la machine
                                   surveillée, alors que c'est plus coûteux ?
                                """)));
    }

    private static Course defenseResponse(Instant now) {
        return Course.create("reponse-a-incident", "Répondre à un incident", Track.DEFENSE, CourseLevel.HARD,
                "Contenir sans détruire les preuves, éradiquer la cause plutôt que le symptôme, et "
                        + "rétablir en sachant pourquoi cela ne recommencera pas.",
                now.minus(Duration.ofDays(3)),
                List.of(
                        new CourseSection(null, "contenir", "Contenir sans effacer",
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
                        new CourseSection(null, "eradiquer", "Éradiquer la cause, pas le symptôme",
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
                        new CourseSection(null, "atelier-post-mortem", "Atelier : rapport de fin d'incident",
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
                        new CourseSection(null, "quiz-reponse", "Quiz : décisions sous pression",
                                SectionKind.QUIZ, 4, 10,
                                """
                                1. Un serveur de production est compromis, l'activité de l'entreprise en
                                   dépend. Isoler, surveiller, ou réinstaller ? Sur quels critères tranchez-
                                   vous, et qui décide ?

                                2. Vous avez retiré le logiciel malveillant et il revient deux jours plus tard.
                                   Qu'a-t-on manqué, et où cherchez-vous ?

                                3. Pourquoi renouveler les secrets même sans preuve qu'ils ont été lus ?
                                """)));
    }
}
