# Machine cible sur Google Compute Engine

Le bouton « Démarrer / Arrêter » du lab pilote une instance Compute Engine.
Cette page dit quoi créer côté Google, quels droits donner, et quelles variables
définir. Rien de tout cela n'est nécessaire pour développer : sans
configuration, la plateforme utilise une cible simulée qui traverse réellement
ses états.

## 1. Le compte de service

Depuis la console Google Cloud, projet dans lequel vit l'instance :

```bash
# Le compte qui agira au nom de la plateforme.
gcloud iam service-accounts create cybermans-lab \
    --display-name="cyberMans — pilotage des cibles"

# Le droit d'allumer, d'éteindre et de lire l'état d'une instance.
gcloud projects add-iam-policy-binding VOTRE_PROJET \
    --member="serviceAccount:cybermans-lab@VOTRE_PROJET.iam.gserviceaccount.com" \
    --role="roles/compute.instanceAdmin.v1"
```

`roles/compute.instanceAdmin.v1` suffit : il couvre `compute.instances.start`,
`compute.instances.stop` et `compute.instances.get`. Ne donnez pas
`roles/editor`, qui ouvrirait le projet entier à un service dont le seul travail
est d'actionner un interrupteur.

## 2. Les identifiants

Deux façons, selon l'endroit où tourne la plateforme.

**La plateforme tourne sur GCP** (Compute Engine, Cloud Run, GKE) : attachez le
compte de service à la ressource, et ne créez **aucune clé**. Les identifiants
sont obtenus et renouvelés automatiquement. C'est la méthode à préférer — une
clé qui n'existe pas ne fuit pas.

**La plateforme tourne ailleurs** : créez une clé JSON et désignez-la.

```bash
gcloud iam service-accounts keys create cle-lab.json \
    --iam-account=cybermans-lab@VOTRE_PROJET.iam.gserviceaccount.com

export GOOGLE_APPLICATION_CREDENTIALS=/chemin/vers/cle-lab.json
```

Ce fichier est un secret de même nature qu'un mot de passe d'administration :
il reste sur le serveur, n'entre jamais dans le dépôt, et n'est jamais servi au
navigateur. La plateforme ne le lit pas elle-même — c'est la bibliothèque
Google qui s'en charge, à partir de cette seule variable.

## 3. Les variables d'environnement

| Variable | Exemple | Rôle |
|---|---|---|
| `GCP_PROJECT_ID` | `mon-projet-lab` | Projet qui héberge l'instance |
| `GCP_ZONE` | `europe-west1-b` | Zone de l'instance |
| `GCP_INSTANCE_NAME` | `target-01` | Instance pilotée par le bouton |
| `APP_MACHINE_PROVIDER` | `gcp` | `simulated` par défaut ; `gcp` pour brancher Compute Engine |
| `GOOGLE_APPLICATION_CREDENTIALS` | `/etc/lab/cle.json` | Clé du compte de service, hors GCP seulement |

Deux variables facultatives :

| Variable | Défaut | Rôle |
|---|---|---|
| `GCP_OPERATION_TIMEOUT` | `60s` | Au-delà, un démarrage rend la main avec un message plutôt que de bloquer |
| `GCP_TARGET_NAME` | `target-{slug}` | Nom d'instance d'une machine du catalogue ; `{slug}` est remplacé par son identifiant |
| `APP_MACHINE_ADDRESS` | *(vide)* | Adresse montrée à l'apprenant, en remplacement de celle que Compute Engine rapporte (§5) |

Rien n'est codé en dur, et aucun de ces paramètres n'atteint le navigateur : le
client ne reçoit qu'un état et une adresse interne.

## 4. Les cibles du catalogue (facultatif)

`APP_MACHINE_PROVIDER=gcp` ne concerne que la cible unique de `/api/machine`.
Pour que **chaque machine du catalogue** soit elle aussi une instance Compute
Engine, ajoutez :

```bash
APP_HYPERVISOR_MODE=gcp
```

Le nom de l'instance se déduit alors de l'identifiant de la machine par
`GCP_TARGET_NAME` : la machine `obsidian` cherche l'instance `target-obsidian`.
C'est le même procédé que pour l'image Docker d'une cible — quelle instance
héberge quelle machine est un détail de déploiement, pas une donnée du
catalogue.

Deux modèles, selon `APP_GCP_TARGET_MODE`.

### `shared` (défaut) : une instance par machine

L'instance préexiste, vous la créez une fois, la plateforme ne fait que
l'allumer et l'éteindre.

- **Elle est partagée.** Si deux joueurs démarrent la même cible, ils
  travaillent sur la même machine, et le second à l'arrêter l'éteint pour les
  deux. Acceptable pour un lab d'équipe ou une démonstration, pas pour une
  promotion.
- **Elle peut être gratuite**, si l'instance tient dans la couche offerte.

### `per-user` : une instance par apprenant

C'est le modèle de Hack The Box. La plateforme **crée** l'instance quand
l'apprenant clique sur « Démarrer » et la **détruit** quand il l'arrête. Chacun
a sa cible : ce que l'un casse ne dérange personne, et personne n'éteint la
machine d'un autre.

```bash
APP_GCP_TARGET_MODE=per-user
# {user} est obligatoire : sans lui, tous les apprenants viseraient le même nom.
# La plateforme refuse de démarrer si vous l'oubliez.
GCP_TARGET_NAME=target-{slug}-{user}
# Image de la cible, préparée par vous. {slug} est remplacé par l'identifiant
# de la machine : « sentinel » cherche l'image box-sentinel.
APP_GCP_TARGET_IMAGE=projects/mon-projet/global/images/box-{slug}
APP_GCP_TARGET_MACHINE_TYPE=e2-small
APP_GCP_TARGET_DISK_SIZE=20
APP_GCP_TARGET_TAGS=lab-target
```

> **Le coût.** Chaque apprenant actif est une machine facturée. Cela **sort de
> la couche gratuite** dès le deuxième apprenant. Trois garde-fous existent,
> mais ce sont des garde-fous, pas une gratuité :
> - un apprenant ne peut avoir **qu'une cible à la fois** (la couche
>   application le refuse) ;
> - une cible oubliée **expire** au bout de `APP_BOXES_INSTANCE_LIFETIME`
>   (2 h par défaut) et est détruite ;
> - l'arrêt **détruit** l'instance au lieu de l'éteindre, donc son disque
>   cesse d'être facturé. Une instance seulement éteinte continue de coûter.
>
> Il n'y a **pas** de plafond global du nombre d'instances : trente apprenants
> connectés en même temps, c'est trente machines. Surveillez votre budget GCP,
> et posez une alerte de facturation avant d'ouvrir à une promotion.

La cible créée **n'a pas d'adresse externe**, et ce n'est pas un réglage :
l'adaptateur ne pose aucune `AccessConfig` sur sa carte réseau, et il n'y a pas
de variable pour en ajouter. Une machine volontairement vulnérable n'a rien à
faire sur Internet ; on ne la joint qu'en entrant dans le réseau du lab par le
VPN. Prévoyez donc une règle de pare-feu qui autorise la passerelle VPN à
atteindre les cibles, désignées par `APP_GCP_TARGET_TAGS`.

### Dans les deux modèles

**Les machines d'attaque restent simulées.** Elles sont personnelles — une par
joueur, créée à la demande — alors qu'une instance Compute Engine préexiste.
Les provisionner pour de bon demandera un adaptateur de plus.

Le compte de service a besoin de `roles/compute.instanceAdmin.v1` dans les deux
cas : en `per-user`, ce rôle couvre aussi la création et la suppression
d'instances.

## 5. Le VPN : sans lui, l'adresse ne mène nulle part

Le bouton affiche l'adresse **interne** de la machine (par exemple
`10.10.10.10`). Elle n'est joignable que depuis le réseau du lab, donc à travers
le profil `.ovpn` que chaque utilisateur télécharge depuis la page « Accès VPN ».

Ce profil contient désormais la route vers le réseau du lab, construite à partir
de :

```bash
APP_VPN_LAB_NETWORK=10.10.10.0/24
```

**Cette valeur doit correspondre au sous-réseau VPC de vos instances GCP.** Si
elles vivent dans `10.128.0.0/20` et que la plateforme annonce `10.10.10.0/24`,
le tunnel montera, l'adresse s'affichera, et rien ne répondra — une panne
silencieuse qui ressemble à une machine en panne. Vérifiez le sous-réseau :

```bash
gcloud compute networks subnets describe VOTRE_SOUS_RESEAU \
    --region=europe-west1 --format='value(ipCidrRange)'
```

et reportez exactement cette plage dans `APP_VPN_LAB_NETWORK`.

Il faut aussi que la passerelle OpenVPN puisse atteindre le réseau GCP : par un
tunnel site à site, par un VPN Cloud, ou en plaçant la passerelle elle-même dans
le VPC. Router une plage dans un profil client ne crée pas la route réseau qui
manque en amont.

### Quand la passerelle et la cible sont la même machine

Le montage le plus simple — et le seul qui tienne dans une VM gratuite — place
le serveur OpenVPN **sur la cible elle-même**. L'apprenant ne joint alors pas
l'adresse du VPC : il joint l'extrémité du tunnel, `10.8.0.1`. Compute Engine,
lui, continue de rapporter l'adresse de la carte réseau (`10.128.0.2`).

Sans réglage, le bouton afficherait `10.128.0.2`, que le tunnel ne route pas :
rien ne répondrait, et la machine passerait pour en panne alors qu'elle tourne.
D'où :

```bash
APP_MACHINE_ADDRESS=10.8.0.1
APP_VPN_LAB_NETWORK=10.8.0.0/24
```

L'adresse n'est substituée que pour une machine **en marche** : un état de
passage n'en porte pas, et en inventer une ferait croire qu'on peut déjà s'y
connecter. Laissée vide, la variable ne change rien et l'adresse de l'hébergeur
est conservée.

Marche à suivre complète de ce montage, de la création du projet au clic de
l'apprenant : `docs/lab/lab-gcp-htb.tex`.

## 6. Vérifier

```bash
# L'état, sans rien allumer.
curl -s --cookie "LAB_SESSION=..." http://localhost:8080/api/machine/status

# Démarrage : la réponse est généralement STAGING, puis RUNNING quelques
# dizaines de secondes plus tard.
curl -s -X POST --cookie "LAB_SESSION=..." http://localhost:8080/api/machine/start
```

Les trois routes exigent une session : sans cookie, elles répondent 401.

Messages d'erreur possibles :

| Message | Cause |
|---|---|
| `La machine est déjà en cours d'exécution` | Démarrage demandé sur une machine allumée. Compute Engine accepterait sans rien faire ; la plateforme le dit. |
| `La machine change d'état, attendez la fin de l'opération en cours` | Un ordre précédent n'est pas terminé. |
| `Machine « X » introuvable chez l'hébergeur` | Nom, zone ou projet erroné — ou droits manquants sur le compte de service. |
| `Le démarrage de « X » dépasse le délai prévu` | `GCP_OPERATION_TIMEOUT` atteint. L'ordre a pu aboutir malgré tout : relisez l'état. |
