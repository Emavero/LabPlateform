# cyberMans — Lab Platform

Plateforme d'entraînement à la cybersécurité, dans l'esprit de Hack The Box :
un catalogue de machines à compromettre, deux flags par machine, des points,
un rang, un classement et des cours. Chaque utilisateur dispose en plus de ses
deux machines d'attaque personnelles, une Windows (RDP) et une Linux (SSH), et
d'un profil VPN pour joindre le réseau du lab.

```
LabPlateform/
├── backend/             Spring Boot 3 · architecture hexagonale · PostgreSQL
├── frontend/            React 19 · TypeScript · Vite · Clean Architecture
├── lab-images/linux/     Image Ubuntu + SSH des machines Linux (mode Docker)
├── docker-compose.yml   PostgreSQL + backend + frontend (Nginx)
├── docker-compose.docker-lab.yml   Active les machines Linux réelles
└── .env.example         Variables d'environnement
```

## Démarrage rapide

```bash
cp .env.example .env        # facultatif : toutes les valeurs ont un défaut
docker compose up --build
```

L'application est servie sur <http://localhost:3000>. Créez un compte : vous
êtes redirigé vers le tableau de bord, où vos deux machines sont déjà
provisionnées.

Trois conteneurs démarrent dans l'ordre, chacun attendant que le précédent soit
en bonne santé :

| Service    | Image                         | Rôle |
|------------|-------------------------------|------|
| `db`       | `postgres:17-alpine`          | Base de données. Les tables sont créées automatiquement au premier démarrage à partir de `backend/src/main/resources/db/schema.sql`. |
| `backend`  | build de `backend/Dockerfile` | API REST, non exposée sur l'hôte. |
| `frontend` | build de `frontend/Dockerfile`| Nginx : sert l'application et relaie `/api` vers le backend. |

Les données survivent aux redémarrages (volume `db-data`). Pour repartir d'une
base vide : `docker compose down -v`.

## Machines à compromettre, flags et classement

Le menu **Machines** liste le catalogue : chaque machine affiche son système,
sa difficulté, ce qu'elle rapporte et son adresse dans le réseau du lab.
On la rejoint par le VPN, on la compromet, puis on soumet ses deux flags
depuis sa fiche.

| Difficulté  | Flag utilisateur | Flag root | Total |
|-------------|------------------|-----------|-------|
| Très facile | 4                | 6         | 10    |
| Facile      | 8                | 12        | 20    |
| Moyenne     | 12               | 18        | 30    |
| Difficile   | 16               | 24        | 40    |
| Insane      | 20               | 30        | 50    |

Le flag root vaut 60 % des points : l'élévation de privilèges est la partie
qui rapporte le plus. Une machine dont les deux flags sont validés est
« possédée ».

- **Flags.** Une suite de 32 caractères hexadécimaux. La saisie tolère les
  espaces, les majuscules et un habillage `CYBM{…}`. Un flag n'est jamais
  stocké en clair : seule son empreinte SHA-256 est conservée, et la
  comparaison est faite en temps constant. Un flag déjà validé répond 409,
  un flag faux répond 400, sans jamais dire lequel des deux était attendu.
- **Cible à la demande.** Une machine ne tourne pas en permanence : le joueur
  la lance depuis sa fiche, obtient son adresse, et elle s'éteint d'elle-même
  passé son délai de vie (`APP_BOXES_INSTANCE_LIFETIME`, deux heures par
  défaut). Une seule cible par joueur à la fois. En mode `docker`, chaque
  joueur obtient son propre conteneur, lancé depuis l'image de la machine
  (`labplatform/box-<slug>:latest`) ; en mode simulé, l'adresse déclarée est
  renvoyée après le même délai d'allumage.
- **First blood.** Le premier joueur à valider un flag est distingué sur la
  machine et dans le classement. C'est une distinction, pas un bonus de
  points : les suivants touchent la même chose.
- **Rang.** Il se mesure en part du catalogue possédée, et non en total
  absolu : Noob, Script Kiddie (5 %), Hacker (15 %), Pro Hacker (35 %),
  Elite Hacker (55 %), Guru (75 %), Omniscient (100 %). Publier de nouvelles
  machines ne dégrade donc le rang de personne.
- **Classement.** Les joueurs sont triés aux points ; à égalité, le premier
  arrivé passe devant. Il n'y figure qu'un pseudonyme dérivé du compte,
  jamais l'adresse e-mail.
- **Comptes rendus.** Une fois la machine possédée, le joueur écrit sa
  méthode depuis la fiche. La publier ne la rend pas publique : elle devient
  lisible par les joueurs qui ont eux aussi possédé la machine. Ceux qui
  cherchent encore ne voient rien — la plateforme ne distribue pas les
  solutions. Un compte rendu par joueur et par machine, modifiable et
  supprimable par son seul auteur, signé d'un pseudonyme.
- **Difficulté ressentie.** Une fois la machine possédée, on note la
  difficulté qu'on lui a trouvée. La fiche affiche la moyenne des votes à côté
  de la difficulté annoncée, et signale l'écart. Voter avant d'avoir validé
  les deux flags répond 409 : on n'a pas vu la moitié du travail.

### Essayer le parcours

Les machines du catalogue sont des cibles simulées : il n'y a pas de système
réel où aller lire un flag. Pour pouvoir essayer le parcours de bout en bout,
le mode démonstration écrit les flags tirés au premier démarrage dans les
journaux du backend — il est actif dans `docker-compose.yml` et dans le profil
`local`, et désactivé par défaut ailleurs.

```bash
docker compose logs backend | grep '\[démo\]'
```

Sur une infrastructure réelle, les flags sont déposés sur la machine cible à
sa construction et cette option reste à `false` (`APP_BOXES_LOG_SEEDED_FLAGS`).
Le catalogue est semé une seule fois, au premier démarrage : la table `box`
déjà peuplée n'est plus touchée.

## Cours : forensique et défense

Le menu **Cours** ouvre deux filières, chacune avec sa propre page :

| Filière | Ce qu'on y apprend | Cours |
|---------|--------------------|-------|
| **Forensique** (`/cours/forensique`) | Collecte de traces, mémoire, chronologie d'incident | Bases de l'investigation numérique, Analyse de la mémoire vive, Reconstituer la chronologie |
| **Défense** (`/cours/defense`) | Durcissement, détection, réponse à incident | Durcir un système exposé, Détecter : journaux et règles, Répondre à un incident |

Chaque cours est découpé en sections — du cours, des ateliers à jouer sur les
machines du lab, et un quiz — que l'on coche au fur et à mesure. Une section
porte un texte, une vidéo, un quiz, ou plusieurs de ces trois. L'avancement se calcule par cours et
par filière, et il est réversible : rouvrir une section la décompte.

**Quiz corrigés automatiquement.** Une section peut porter des questions à
choix multiples. La copie est corrigée par le serveur : les bonnes réponses ne
sortent jamais avant, et n'apparaissent qu'avec la correction. Une question
n'est acquise que si l'apprenant coche exactement les bonnes propositions —
cocher tout ne rapporte rien. À 70 % de bonnes réponses, la section est
validée sans avoir à la cocher.

Les six cours livrés sont un point de départ, semé au premier démarrage. La
suite se publie depuis le tableau de bord d'administration, sans redémarrage.

Les ateliers renvoient aux machines du catalogue : on durcit sa propre machine
Linux, puis on reconstitue depuis la défense ce que l'on vient de faire en
attaque. Ajouter une filière revient à ajouter une constante dans
`domain/academy/Track` et une entrée de menu.

## Abonnement Pro et paiement

Le catalogue se **consulte** sans abonnement : chaque machine montre son nom,
son système, sa difficulté et ce qu'elle rapporte. Ce qui sert à l'attaquer est
réservé aux abonnés — l'adresse dans le réseau du lab, la cible à la demande,
la soumission des flags et les comptes rendus. Les cours, le classement et le
profil restent ouverts à tous, et l'administration peut ouvrir une machine à
tous (case *Réservée aux abonnés* décochée) : le catalogue livré en contient
deux, pour qu'un compte gratuit sache ce qu'il achèterait.

Un appel sur une machine réservée répond **402 Payment Required** plutôt que
403 : l'appelant n'est pas indésirable, il lui manque un abonnement, et
l'interface propose donc de le prendre au lieu d'afficher un refus.

- **Deux moyens de paiement, deux devises.** La carte bancaire ne couvre pas
  l'Afrique de l'Ouest, où le paiement passe par un portefeuille mobile : la
  carte (Stripe) et **Wave** coexistent. Les réseaux bancaires n'acceptant pas
  le franc CFA, chaque moyen a son tarif — `APP_BILLING_CURRENCY` pour la base
  (5 000 F CFA/mois par défaut), `APP_BILLING_CARD_*` pour la carte (8 €/mois).
  Ajouter un opérateur, c'est une classe dans `adapter/out/payment` et une
  ligne de configuration : ni le domaine ni les cas d'usage ne bougent.
- **La plateforme ne décide jamais qu'un paiement a réussi.** Elle le demande
  au prestataire (au retour du payeur) ou reçoit sa notification signée. Une
  requête du navigateur sur l'URL de retour ne suffit donc pas à s'offrir un
  abonnement.
- **Tout est idempotent.** Les prestataires répètent leurs notifications ; un
  paiement ne se règle qu'une fois, et une échéance ne se crédite qu'une fois.
  Renouveler d'avance prolonge le terme en cours au lieu de le remettre à zéro.
- **Notifications authentifiées par signature.** Le corps brut est vérifié en
  HMAC-SHA256 contre l'horodatage de l'en-tête, comparé à temps constant, avec
  une tolérance de cinq minutes qui interdit le rejeu. Un corps non signé n'est
  pas une notification.
- **Résilier ne coupe pas l'accès** : le terme déjà payé reste dû, seul le
  renouvellement s'arrête.

Par défaut, `APP_BILLING_MODE=simulated` : le parcours complet se déroule sans
compte marchand, ce qui permet de l'essayer — mais **tout compte peut alors
s'attribuer un abonnement en cliquant**, et le démarrage l'annonce dans les
journaux. Une installation qui facture réellement passe en `live` et renseigne
`STRIPE_SECRET_KEY` / `WAVE_API_KEY` et leurs secrets de notification.

```bash
APP_BILLING_MODE=live STRIPE_SECRET_KEY=sk_live_… WAVE_API_KEY=wave_… docker compose up -d
```

## Administration

L'administrateur ne voit pas les pages de joueur : son menu ne contient que
l'administration et les réglages de son compte, et taper `/machines` le renvoie
à son tableau de bord. La séparation est volontaire — il publie le contenu, il
ne le consomme pas, et il n'a ni abonnement ni progression.

Le menu **Administration** n'apparaît que pour les comptes administrateurs.
Il ouvre un tableau de bord (ce qui est publié, ce qui est utilisé) et
l'éditeur de cours.

Publier un cours, c'est remplir un formulaire : un titre, une filière, un
niveau, un résumé, puis les sections dans l'ordre de lecture. Chaque section
porte un titre, un type, une durée, un texte et, si besoin, l'adresse d'une
vidéo. Le cours est visible par les apprenants dès l'enregistrement.

- **Vidéos.** Deux possibilités. **Téléverser** le fichier sur la plateforme
  (MP4, WebM ou Ogg, 256 Mo au plus) : il est rangé dans le volume
  `media-data`, servi par `/api/media/{id}` aux seuls utilisateurs connectés,
  avec les requêtes par plage pour pouvoir s'y déplacer. Ou **coller une
  adresse** : YouTube et Vimeo s'affichent dans la page par leur adresse
  d'intégration, toute autre devient un lien, jamais un cadre. La liste des
  plateformes intégrées est la même côté application et dans la politique de
  sécurité de contenu servie par Nginx.
- **Renommer sans casser.** L'identifiant d'URL d'un cours est dérivé de son
  titre à la création et ne change plus : les liens partagés restent valides.
  Une section garde son identifiant tant que l'éditeur le renvoie, donc la
  retitrer, la déplacer ou lui ajouter une vidéo n'efface l'avancement de
  personne. La retirer du cours, en revanche, supprime l'avancement qui s'y
  rapportait.
- **Devenir administrateur.** Le rôle vient de `APP_ADMIN_EMAILS`, une liste
  d'adresses séparées par des virgules, et de là seulement : aucune route ne
  l'accorde, pas même à un administrateur. Les comptes listés sont promus au
  démarrage s'ils existent déjà, à l'inscription sinon.

```bash
APP_ADMIN_EMAILS=vous@exemple.fr docker compose up --build
```

Le tableau de bord gère aussi **les machines du catalogue** : nom, système,
difficulté, adresse et flags. À la publication, les flags laissés vides sont
tirés au hasard et **affichés une seule fois** — le temps de les déposer sur
la cible, car seule leur empreinte est conservée. À la modification, un champ
de flag vide veut dire « ne change rien ».

## Profil, hauts faits et activité

La page **Profil** rassemble le rang, les hauts faits et l'activité récente.

- **Hauts faits.** Dix distinctions, du premier flag validé à la machine
  insane possédée, en passant par les cours terminés. Ils se déduisent
  entièrement du palmarès : rien n'est stocké, donc rien ne peut manquer ni se
  décerner deux fois. Ceux qui manquent restent affichés, avec leur condition.
- **Activité.** Flags validés et sections terminées, du plus récent au plus
  ancien, avec les points gagnés et les first bloods.

## Machines Linux réelles (mode Docker)

Par défaut, les machines sont simulées : l'interface affiche des accès, mais
aucune machine ne tourne derrière. Le mode Docker rend la machine Linux réelle.
Chaque démarrage crée un conteneur Ubuntu 24.04 neuf, accessible en SSH. Chaque
arrêt le supprime.

```bash
docker compose -f docker-compose.yml -f docker-compose.docker-lab.yml up --build
```

Démarrez ensuite la machine Linux depuis l'interface et collez la commande
affichée, par exemple `ssh labuser@localhost -p 32771`. Le mot de passe
temporaire s'affiche dans le panneau d'accès.

| Réglage (`.env`) | Défaut | Rôle |
|------------------|--------|------|
| `DOCKER_GID` | `0` | Groupe du socket Docker. Sur Linux : `stat -c %g /var/run/docker.sock`. Docker Desktop : `0`. |
| `LAB_PUBLIC_HOST` | `localhost` | Adresse affichée dans la commande ssh (IP ou nom du serveur) |
| `LAB_BIND_ADDRESS` | `127.0.0.1` | `127.0.0.1` = accessible depuis ce poste ; `0.0.0.0` = depuis le réseau |
| `LAB_MEMORY` / `LAB_CPUS` | `512m` / `1.0` | Ressources de chaque machine |

Fonctionnement :

- **Image.** Elle est construite depuis `lab-images/linux/` et contient
  OpenSSH, sudo, python3, nmap, tcpdump, curl et netcat.
- **Compte.** L'utilisateur `labuser` est membre de `sudo`. Il est verrouillé
  jusqu'à ce que le backend injecte le mot de passe temporaire via
  `docker exec … chpasswd`. Le mot de passe n'apparaît ni dans les arguments
  de commande, ni dans l'environnement du conteneur.
- **Port.** Le port SSH est choisi librement par Docker, ce qui évite toute
  collision entre utilisateurs.
- **Isolation.** Chaque conteneur est limité en mémoire, en CPU et en nombre
  de processus.
- **Données.** Rien n'est conservé d'une session à l'autre : un arrêt efface
  la machine.
- **Windows.** La machine Windows reste simulée : Windows ne peut pas tourner
  en conteneur sur un hôte Linux. Il faut un hyperviseur (Proxmox, VMware,
  cloud).

Le backend pilote Docker via le socket de l'hôte, ce qui équivaut à un accès
administrateur sur la machine. Réservez ce mode à un serveur dédié aux labs.

Pour supprimer tous les conteneurs de lab restants :

```bash
docker rm -f $(docker ps -aq --filter label=labplatform.vm-id)
```

## Accès VPN des utilisateurs (façon Hack The Box)

La page **VPN Access** permet à chaque utilisateur de télécharger en un clic
son profil OpenVPN personnel (`cyberMans-lab-udp.ovpn` ou `cyberMans-lab-tcp.ovpn`).
Une fois connecté au VPN, il joint ses machines par leur adresse dans le réseau
du lab.

- **Profil personnel.** Chaque profil contient un certificat client unique,
  émis automatiquement au premier téléchargement.
- **UDP ou TCP.** UDP est recommandé ; TCP sert pour les réseaux qui bloquent
  l'UDP, ou pour passer par ngrok.
- **Régénérer.** Le bouton révoque le certificat actuel, ce qui rend l'ancien
  fichier inutilisable partout, puis en émet un nouveau.
- **Autorité de certification intégrée.** cyberMans joue le rôle d'autorité de
  certification grâce à easy-rsa, avec une clé `tls-crypt` pour protéger les
  échanges. Tout est créé automatiquement au premier usage, dans le volume
  `vpn-data`. Sauvegardez ce volume : le perdre invalide tous les profils.

Pour l'activer, renseignez dans `.env` :

```bash
APP_VPN_ENABLED=true
APP_VPN_UDP_HOST=vpn.mondomaine.fr     # adresse publique de la passerelle OpenVPN
APP_VPN_TCP_HOST=                      # optionnel (ex. 5.tcp.eu.ngrok.io + APP_VPN_TCP_PORT)
APP_VPN_LAB_NETWORK=10.10.10.0/24
```

La passerelle OpenVPN doit faire confiance à l'autorité de cyberMans. Après un
premier téléchargement, qui crée l'autorité, copiez ces fichiers sur la
passerelle dans `/etc/openvpn/server/` :

```bash
docker compose cp backend:/var/lib/labplatform/vpn/pki/ca.crt .
docker compose cp backend:/var/lib/labplatform/vpn/pki/issued/serveur.crt .
docker compose cp backend:/var/lib/labplatform/vpn/pki/private/serveur.key .
docker compose cp backend:/var/lib/labplatform/vpn/ta.key .
```

La configuration du serveur OpenVPN doit contenir `tls-crypt ta.key` et
`crl-verify crl.pem`. La liste de révocation est publique sur
`/api/vpn/crl.pem`. Sur la passerelle, une tâche cron la récupère chaque
minute, pour que les profils régénérés soient refusés sans délai :

```bash
* * * * * curl -fsS https://cyberMans.mondomaine.fr/api/vpn/crl.pem -o /etc/openvpn/server/crl.pem.new && mv /etc/openvpn/server/crl.pem.new /etc/openvpn/server/crl.pem
```

Vérifié avec OpenVPN 2.6 : un profil généré par cyberMans se connecte en UDP et
en TCP, reçoit la route du lab, et un profil régénéré est refusé
(« certificate revoked »).

## Développement local

Backend (base H2 en mémoire, aucun PostgreSQL requis) :

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
mvn test
```

Frontend (le serveur Vite relaie `/api` vers `http://localhost:8080`) :

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173
npm test             # tests unitaires (Vitest)
npm run build        # vérification des types + build de production
```

Pour viser un autre backend : `VITE_API_PROXY_TARGET=http://hote:8080 npm run dev`.

## Architecture du backend : hexagonale

> Une documentation d'architecture complète, pensée pour l'accueil d'un
> nouvel arrivant et pour faire évoluer la plateforme, est dans
> [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) (et sa version imprimable
> `docs/ARCHITECTURE.pdf`). Ce qui suit en est le résumé.

Le cœur métier ne dépend d'aucun framework. Spring n'apparaît que dans les
adaptateurs et dans la configuration.

```
com.labplatform
├── domain/            Modèle métier pur Java
│   ├── user/          User, Email, PasswordPolicy, PasswordResetToken, Role
│   ├── lab/           VirtualMachine (machine à états), ConnectionInfo,
│   │                  OperatingSystem, AccessProtocol, VmAccessPolicy
│   ├── box/           Box (machine à compromettre), Flag, Difficulty,
│   │                  FlagKind, Own
│   ├── scoring/       Rank, PlayerProgress, PlayerScore, Handle
│   ├── academy/       Course (agrégat), CourseSection, Track, CourseProgress, Slug
│   ├── achievement/   Achievement (règles), PlayerRecord
│   └── user/          … AdminPolicy (qui publie le contenu)
│   └── shared/        Exceptions métier (validation, conflit, introuvable…)
├── application/
│   ├── port/in/       Cas d'usage : RegisterUser, AuthenticateUser, StartVm,
│   │                  StopVm, ListBoxes, SubmitFlag, GetPlayerProgress,
│   │                  GetLeaderboard, ListCourses, TrackSectionProgress,
│   │                  RateBox, ManageCourses, GetAdminOverview…
│   ├── port/out/      Besoins du métier : UserRepositoryPort, HypervisorPort,
│   │                  BoxRepositoryPort, OwnRepositoryPort, CourseRepositoryPort…
│   └── service/       Implémentations des cas d'usage
├── adapter/
│   ├── in/web/        Contrôleurs REST, filtre de session, gestion d'erreurs
│   ├── in/event/      Provisionnement des machines à l'inscription
│   ├── in/startup/    Semis du catalogue et des cours, promotion des administrateurs
│   └── out/           JPA/PostgreSQL, JWT, BCrypt, hyperviseur simulé…
└── config/            Racine de composition (câblage des cas d'usage)
```

Principes appliqués :

- Les règles vivent dans le domaine. `VirtualMachine` refuse de démarrer une
  machine déjà démarrée, et garantit qu'une machine a des accès si et seulement
  si elle tourne. La même contrainte est doublée en base (`ck_vm_state`).
- Un utilisateur ne voit que ses machines. Une machine d'un autre compte
  répond 404, pas 403, pour ne pas révéler son existence.
- Les hauts faits ne sont pas stockés : ils se déduisent du palmarès à chaque
  lecture, donc ils ne peuvent pas se désynchroniser, et en ajouter un
  n'exige aucune migration.
- Un flag n'est comparé que par l'agrégat `Box`, seul détenteur des
  empreintes, et le barème découle de la seule difficulté : aucun nombre de
  points n'est écrit dans un service ou un contrôleur. Les points sont figés
  dans la possession, donc rééquilibrer une machine ne réécrit pas le passé
  des joueurs.
- Les appels à l'hyperviseur, potentiellement lents, sont faits hors
  transaction de base de données.
- Changer d'infrastructure revient à écrire un adaptateur. Deux existent :
  `simulated`, le défaut, et `docker`, qui rend la machine Linux réelle.
  Pour piloter de vraies VMs Windows (Proxmox, vSphere, cloud…), il suffit
  d'implémenter `HypervisorPort` et de l'activer avec `APP_HYPERVISOR_MODE`.
  L'envoi d'e-mails de réinitialisation se branche de la même façon via
  `PasswordResetNotifierPort`.

### API

| Méthode | Route                          | Description |
|---------|--------------------------------|-------------|
| POST    | `/api/auth/register`           | Inscription (201) : e-mail valide et unique, mot de passe confirmé |
| POST    | `/api/auth/login`              | Connexion : pose le cookie de session |
| POST    | `/api/auth/logout`             | Déconnexion : efface le cookie (204) |
| POST    | `/api/auth/forgot-password`    | Demande de réinitialisation (réponse identique que le compte existe ou non) |
| POST    | `/api/auth/reset-password`     | Nouveau mot de passe à partir du jeton |
| GET     | `/api/users/me`                | Profil de l'utilisateur connecté |
| PUT     | `/api/users/me/password`       | Changement de mot de passe |
| GET     | `/api/vpn`                     | État de l'accès VPN (profil émis, serveurs, réseau du lab) |
| GET     | `/api/vpn/profile?protocol=udp` | Téléchargement du profil `.ovpn` personnel (émis au premier appel) |
| POST    | `/api/vpn/profile/regenerate`  | Révocation du profil actuel et émission d'un nouveau |
| GET     | `/api/vpn/crl.pem`             | Liste de révocation (publique, pour la passerelle) |
| GET     | `/api/labs/vms`                | Machines de l'utilisateur |
| GET     | `/api/labs/vms/{id}`           | Détail d'une machine |
| GET     | `/api/labs/vms/{id}/logs`      | Journal de console |
| POST    | `/api/labs/vms/{id}/start`     | Démarrage (409 si déjà démarrée) |
| POST    | `/api/labs/vms/{id}/stop`      | Arrêt (409 si déjà arrêtée) |
| GET     | `/api/boxes`                   | Catalogue, enrichi de ce que l'appelant a validé |
| GET     | `/api/boxes/{slug}`            | Fiche d'une machine |
| POST    | `/api/boxes/{slug}/flags`      | Soumission d'un flag (400 incorrect, 409 déjà validé) |
| POST    | `/api/boxes/{slug}/instance`   | Lance la cible (409 si une autre tourne déjà) |
| DELETE  | `/api/boxes/{slug}/instance`   | Arrête la cible |
| GET     | `/api/billing`                 | Abonnement du compte : formule, échéance, offre, paiements |
| POST    | `/api/billing/checkout`        | Ouvre le paiement et rend l'adresse du prestataire |
| POST    | `/api/billing/confirm`         | Relit l'état du paiement au retour du payeur |
| DELETE  | `/api/billing`                 | Résilie (l'accès court jusqu'à l'échéance payée) |
| POST    | `/api/billing/webhooks/{method}` | Notification du prestataire, authentifiée par signature |
| GET     | `/api/boxes/{slug}/writeups`   | Comptes rendus lisibles par l'appelant |
| PUT     | `/api/boxes/{slug}/writeups/mine` | Écrit ou révise le sien (409 si non possédée) |
| DELETE  | `/api/boxes/{slug}/writeups/mine` | Supprime le sien (204) |
| GET     | `/api/scoreboard/me`           | Progression : points, rang, machines possédées |
| GET     | `/api/scoreboard?limit=20`     | Classement public |
| PUT     | `/api/boxes/{slug}/rating`     | Note de difficulté (409 si la machine n'est pas possédée) |
| GET     | `/api/courses/tracks`          | Filières de cours |
| GET     | `/api/courses?track=forensique` | Cours d'une filière, avec l'avancement |
| GET     | `/api/courses/{slug}`          | Cours complet : sections et contenu |
| GET     | `/api/courses/progress`        | Avancement par filière |
| POST    | `/api/courses/{slug}/sections/{section}/quiz` | Rend une copie : correction et bonnes réponses |
| POST    | `/api/courses/{slug}/sections/{section}/completion` | Marque une section comme terminée |
| DELETE  | `/api/courses/{slug}/sections/{section}/completion` | Rouvre une section |
| GET     | `/api/profile/achievements`    | Hauts faits, obtenus ou non |
| GET     | `/api/profile/activity?limit=20` | Activité récente |
| GET     | `/api/admin/overview`          | Chiffres du tableau de bord (403 hors administrateur) |
| POST    | `/api/admin/courses`           | Publication d'un cours (201) |
| PUT     | `/api/admin/courses/{slug}`    | Refonte d'un cours, avancement préservé |
| DELETE  | `/api/admin/courses/{slug}`    | Suppression d'un cours (204) |
| GET     | `/api/admin/boxes`             | Catalogue complet, machines retirées comprises |
| POST    | `/api/admin/boxes`             | Publication d'une machine (201, flags affichés une fois) |
| PUT     | `/api/admin/boxes/{slug}`      | Modification d'une machine |
| DELETE  | `/api/admin/boxes/{slug}`      | Suppression d'une machine (204) |
| GET     | `/api/admin/courses/{slug}`    | Fiche d'un cours, bonnes réponses comprises |
| POST    | `/api/admin/media`             | Téléversement d'une vidéo (multipart, 201) |
| GET     | `/api/media/{id}`              | Lecture d'une vidéo téléversée (requêtes par plage) |

Toutes les erreurs suivent le même format :
`{ timestamp, status, error, message, path, details }`.

## Architecture du frontend : Clean Architecture

```
src/
├── domain/          Aucune dépendance à React ni à HTTP
│   ├── models/      User, VirtualMachine, Box, Progress, Course, Profile, Admin, Video
│   ├── repositories/ Interfaces (AuthRepository, LabRepository, BoxRepository…)
│   ├── usecases/    Login, Register, StartVm, StopVm, SubmitFlag,
│   │                GetProgress, GetLeaderboard, ToggleSection…
│   └── validation/  Règles de saisie (miroir des règles serveur)
├── data/            Implémentations HTTP des repositories (axios)
├── di/container.ts  Racine de composition : seul fichier qui connaît les implémentations
├── presentation/
│   ├── design-system/  Button, TextField, Panel, StatusIndicator, CopyField, Icon…
│   ├── layouts/        AppShell, Sidebar, UserMenu, AuthLayout
│   ├── navigation/     Menu déclaratif
│   ├── features/lab/   VmCard, ConnectionDetails, guides d'accès par protocole
│   ├── features/box/   BoxCard, FlagForm, DifficultyMeter, ProgressPanel, RatingPicker
│   ├── features/course/ CourseCard, TrackProgress, VideoPlayer
│   ├── hooks/ state/   État de session, état du lab, actions asynchrones
│   ├── pages/          Une page par route
│   └── styles/         Jetons de design et feuilles de style
└── app/App.tsx      Routage
```

Correspondance avec SOLID :

- **SRP.** Un cas d'usage fait une chose. Un composant du design system ignore
  tout du métier.
- **OCP.** Une nouvelle entrée de menu s'ajoute dans `navigation.ts`, y compris
  une filière de cours (le menu accepte des groupes à deux niveaux) et une
  entrée réservée à un rôle (`roles: ['ADMIN']`). Un nouveau
  protocole d'accès (VNC, console web…) s'ajoute dans `accessGuides.ts`. Aucun
  composant n'est modifié.
- **DIP.** Les pages reçoivent des cas d'usage par injection
  (`useDependencies`), qui dépendent eux-mêmes d'interfaces. Remplacer l'API
  HTTP par un mock ne touche que `di/container.ts`.

Les entrées du menu Exposure Analysis, Attack Paths, Events, Scenario Designer,
Administration, Report Center et Support mènent à une page d'attente commune,
prête à être remplacée module par module.

## Sécurité

- **Session.** Le JWT (8 h par défaut) est stocké dans un cookie `HttpOnly`,
  `SameSite=Strict`, limité au chemin `/api`. Il est illisible en JavaScript et
  n'est jamais renvoyé dans le corps des réponses.
- **CSRF.** La protection par jeton est désactivée. `SameSite=Strict` empêche
  l'envoi du cookie depuis un autre site, et Nginx sert l'application et l'API
  sur la même origine.
- **HTTPS.** Derrière HTTPS, passez `APP_COOKIE_SECURE=true`.
- **Mots de passe.** Ils sont hachés avec BCrypt (coût 12). La connexion prend
  le même temps que le compte existe ou non. Les jetons de réinitialisation ne
  sont stockés que sous forme d'empreinte SHA-256 et expirent après 15 minutes.
- **Secret JWT.** Définissez `APP_JWT_SECRET` (32 octets minimum) hors
  développement.
- **Mode démonstration.** Sans serveur d'e-mail, `APP_EXPOSE_RESET_TOKEN=true`
  affiche directement le lien de réinitialisation. Il est activé dans
  `docker-compose.yml` pour la démo et désactivé par défaut dans le backend.
- **Flags.** Ils ne sont stockés que sous forme d'empreinte SHA-256 et
  comparés en temps constant. Une réponse du catalogue ne contient ni le flag,
  ni son empreinte. Une soumission mal formée est rejetée avant toute requête
  en base, et l'unicité `(joueur, machine, flag)` est tenue en base, ce qui
  interdit de compter deux fois les mêmes points même en cas de double
  soumission simultanée.
- **Classement.** Il n'expose qu'un pseudonyme dérivé de la partie locale de
  l'e-mail, jamais l'adresse complète.
- **Quiz.** Les bonnes réponses ne sortent du serveur qu'avec la correction,
  ou pour un administrateur : la fiche du cours servie à l'apprenant porte
  `correct: null`. La correction est faite côté serveur, jamais dans le
  navigateur.
- **Administration.** `/api/admin/**` exige le rôle ADMIN dans la chaîne de
  sécurité, et chaque cas d'usage le revérifie : une règle métier ne dépend
  pas de la configuration d'un framework. Le menu masque ces entrées aux
  autres comptes, mais ce n'est qu'un confort d'affichage. Une adresse de
  vidéo doit être en http(s) et n'est intégrée que si elle vient d'une
  plateforme connue, ce qui ferme `javascript:` et `data:`.
- **En-têtes.** Nginx ajoute les en-têtes de sécurité (CSP,
  `X-Frame-Options`, `nosniff`…).

## Variables d'environnement

| Variable                   | Défaut (compose)   | Rôle |
|----------------------------|--------------------|------|
| `FRONTEND_PORT`            | `3000`             | Port publié de l'application |
| `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` | `labplatform` | Base PostgreSQL |
| `APP_JWT_SECRET`           | valeur de dev      | Clé de signature des jetons |
| `APP_JWT_VALIDITY`         | `8h`               | Durée de session |
| `APP_COOKIE_SECURE`        | `false`            | Cookie réservé à HTTPS |
| `APP_EXPOSE_RESET_TOKEN`   | `true`             | Lien de réinitialisation affiché à l'écran |
| `APP_ADMIN_EMAILS`         | vide               | Comptes administrateurs, séparés par des virgules |
| `APP_BOXES_LOG_SEEDED_FLAGS` | `true`           | Flags du catalogue écrits dans les journaux au premier démarrage (démo) |
| `APP_BOXES_LEADERBOARD_SIZE` | `20`             | Nombre de joueurs affichés dans le classement |
| `APP_BOXES_INSTANCE_LIFETIME` | `2h`            | Durée de vie d'une cible lancée à la demande |
| `APP_MEDIA_MAX_FILE_SIZE`  | `256MB`            | Taille maximale d'une vidéo téléversée |
| `APP_BILLING_ENABLED`      | `true`             | `false` ouvre toute la plateforme, sans contenu réservé |
| `APP_BILLING_MODE`         | `simulated`        | `live` appelle réellement les prestataires |
| `APP_BILLING_METHODS`      | `CARD,WAVE`        | Moyens de paiement proposés |
| `APP_BILLING_CURRENCY`     | `XOF`              | Devise et tarif de base (`_MONTHLY`, `_YEARLY`) |
| `APP_BILLING_CARD_CURRENCY`| `EUR`              | Tarif propre à la carte (`_MONTHLY`, `_YEARLY`) |
| `STRIPE_SECRET_KEY`        | vide               | Clé Stripe, requise en mode `live` |
| `WAVE_API_KEY`             | vide               | Clé Wave, requise en mode `live` |

## Intégration continue

`.github/workflows/tests.yml` exécute les tests backend deux fois — sur H2,
puis sur PostgreSQL, la base de production — et les tests frontend
(`npm test`, puis `npm run build`, qui vérifie aussi les types).

La seconde exécution n'est pas un luxe : certaines erreurs de correspondance
objet-relationnel ne se voient que sur PostgreSQL. Un `@Lob` sur une chaîne y
écrit un « large object » et ne stocke que son identifiant, là où H2 range le
texte sans broncher. Les tests locaux visent H2 par défaut ; renseigner
`TEST_DB_URL`, `TEST_DB_USERNAME` et `TEST_DB_PASSWORD` rejoue la même suite
sur PostgreSQL.

## Pistes

- Brancher un hyperviseur réel via `HypervisorPort` pour la machine Windows.
- Brancher un envoi d'e-mails via `PasswordResetNotifierPort`.
- Implémenter les modules du menu encore en attente.
