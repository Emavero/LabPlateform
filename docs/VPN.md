# Accès VPN : deux façons de distribuer le profil

Pour joindre une machine du lab, l'apprenant monte un tunnel VPN. Le fichier
`.ovpn` qui le permet peut venir de deux endroits, et le choix n'est pas
anodin.

| | `generated` (défaut) | `uploaded` |
|---|---|---|
| Qui produit le fichier | La plateforme, via easy-rsa | Votre serveur OpenVPN |
| Un profil par | Apprenant | Plateforme entière |
| Révoquer un accès | Oui, individuellement | Non : seulement remplacer pour tous |
| À installer | easy-rsa, une autorité de certification | Rien |
| Mise en place | Quelques réglages | Déposer un fichier |

```bash
APP_VPN_ENABLED=true
APP_VPN_SOURCE=uploaded     # ou « generated »
```

## Mode `uploaded` : déposer un profil

1. Depuis votre serveur OpenVPN, exportez un **profil client** avec ses
   certificats intégrés (un seul fichier, pas une configuration de serveur).
2. Connectez-vous en administrateur, ouvrez **Administration → Profil VPN**.
3. Déposez le fichier. Il devient aussitôt téléchargeable depuis « Accès VPN ».

La plateforme vérifie au dépôt que le fichier est bien un profil client : texte,
moins de 512 Ko, et portant les directives `client` et `remote`. L'erreur la
plus fréquente — déposer la configuration du serveur au lieu de celle du client —
est signalée tout de suite, avec son nom.

### La route vers le réseau du lab

C'est le piège de ce mode. Un profil qui ne route pas le réseau des machines
laisse le tunnel monter **sans rien joindre** : l'adresse interne affichée par
le bouton « Démarrer » ne répond pas, et cela ressemble à une machine en panne.

L'écran d'administration le signale quand le profil déposé ne contient ni

```
route 10.10.10.0 255.255.255.0
```

ni `redirect-gateway`. Ce n'est qu'un avertissement : votre serveur OpenVPN
peut pousser la route lui-même, auquel cas il n'y a rien à faire.

`APP_VPN_LAB_NETWORK` doit décrire le réseau réel de vos machines. En mode GCP,
c'est le sous-réseau du VPC — voir [`GCP.md`](GCP.md).

### Ce que ce mode ne permet pas

Tout le monde télécharge **le même fichier**. On ne peut donc pas couper
l'accès d'une seule personne : il faut remplacer le profil, ce qui coupe
l'accès de tous jusqu'à ce que chacun reprenne le nouveau. Pour un lab
d'équipe ou une formation courte, c'est sans conséquence. Pour une plateforme
ouverte, préférez `generated`.

Le profil déposé est rangé dans `APP_VPN_DIRECTORY/uploaded/`, hors de portée
du serveur web. Il contient une clé privée : ce dossier est à sauvegarder avec
les mêmes précautions qu'un secret.

## Mode `generated` : un certificat par apprenant

La plateforme tient une autorité de certification et émet un certificat par
personne, révocable individuellement. Il faut alors `easy-rsa` sur le serveur
et un serveur OpenVPN configuré pour accepter cette autorité.

```bash
APP_VPN_SOURCE=generated
APP_VPN_UDP_HOST=vpn.exemple.fr
APP_VPN_UDP_PORT=1194
APP_VPN_EASYRSA=/usr/share/easy-rsa/easyrsa
APP_VPN_DIRECTORY=/var/lib/labplatform/vpn
```

Le profil produit porte la route vers `APP_VPN_LAB_NETWORK` : il n'y a rien à
ajouter à la main.

Régénérer un profil depuis la page « Accès VPN » révoque l'ancien certificat :
l'ancien fichier cesse de fonctionner immédiatement.
