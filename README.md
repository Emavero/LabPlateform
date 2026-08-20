# Lab Platform

Plateforme web permettant aux utilisateurs de s'inscrire, se connecter, puis
d'accéder à un tableau de bord donnant accès à un module **Lab** (gestion de
machines virtuelles Windows / Linux hébergées à distance).

Ce dépôt contient uniquement les premières briques du produit : authentification,
tableau de bord, et l'interface Labs (liste des VM, démarrage/arrêt, informations
de connexion). L'architecture est volontairement modulaire pour permettre
d'ajouter de nouveaux modules (quotas, facturation, notifications, provisionnement
réel des VM...) sans recoder l'existant.

```
project/
├── backend/     Spring Boot 3 + Spring Security (JWT) + H2
└── frontend/    React 18 + Vite + React Router
```

## Architecture

### Backend — modulaire par package métier

| Package    | Rôle |
|------------|------|
| `user`     | Entité `User` (implémente `UserDetails`), `Role`, `UserRepository`, `UserDetailsServiceImpl` |
| `security` | Brique technique JWT : `JwtService`, `JwtAuthFilter` — ne connaît rien du métier |
| `config`   | `AppProperties` (config typée), `SecurityConfig` (filter chain, CORS, BCrypt) |
| `auth`     | DTOs, `AuthService`, `AuthController` — inscription / connexion, publie `UserRegisteredEvent` |
| `lab`      | `VirtualMachine`, `LabService`, `LabController` — écoute `UserRegisteredEvent` pour provisionner 2 VM par utilisateur, sans dépendre du module `auth` |
| `common`   | `GlobalExceptionHandler`, `ApiError`, `BusinessException` — gestion d'erreurs uniforme pour tous les modules |

Le découplage `auth` → `lab` se fait via un **événement Spring**
(`UserRegisteredEvent`), pas un appel direct : un futur module réagissant à une
inscription (ex: envoi d'e-mail de bienvenue) s'ajoute sans toucher à `AuthService`.

### Frontend — par couche + par module

- `api/` : `axiosClient.js` (instance unique, injection auto du token) puis un
  fichier par domaine (`authApi.js`, `labApi.js`) — un nouveau module ajoute
  simplement `xxxApi.js`
- `context/AuthContext.jsx` : état de session global
- `components/` : briques réutilisables (`FormField`, `AuthLayout`, `AppTopbar`,
  `VmCard`, `ProtectedRoute`)
- `pages/` : `Login`, `Register`, `Dashboard`, `Labs`

## Lancer le projet en local

### Prérequis
- Java 17+
- Maven 3.9+ (ou utiliser le wrapper si vous en ajoutez un)
- Node.js 18+

### 1. Backend

```bash
cd backend
mvn spring-boot:run
```

L'API démarre sur `http://localhost:8080`.
- Base H2 en mémoire (aucune installation requise, les données sont perdues au redémarrage)
- Console H2 disponible sur `http://localhost:8080/h2-console`
  (JDBC URL : `jdbc:h2:mem:labplatform`, user `sa`, pas de mot de passe)

Le secret JWT par défaut est défini dans `application.yml` pour le développement.
En production, définissez la variable d'environnement `APP_JWT_SECRET`.

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```

Le frontend démarre sur `http://localhost:5173` et proxifie automatiquement les
appels `/api/*` vers `http://localhost:8080` (voir `vite.config.js`).

### 3. Tester

1. Ouvrir `http://localhost:5173`
2. Créer un compte (e-mail + mot de passe + confirmation)
3. Vous êtes redirigé vers le tableau de bord
4. Cliquer sur le panneau **Lab** → deux VM (Windows, Linux) sont déjà
   provisionnées automatiquement lors de l'inscription
5. Démarrer une VM pour afficher ses informations de connexion (IP, port,
   identifiants) — simulées pour le développement

## Points d'extension prévus

- Provisionnement réel des VM : remplacer la simulation dans `LabService.start()`
  par un appel à une API d'orchestration (hyperviseur, cloud), sans changer le contrat exposé par `LabController`
- Rôles/permissions : le `Role` enum et `SecurityConfig` sont prêts pour des rôles
  supplémentaires (ex: `ADMIN`) et des règles d'autorisation plus fines
- Nouveaux modules métier (quotas, historique, facturation...) : suivre le même
  patron que `lab` (package dédié, événements pour le découplage, DTOs propres)
# LabPlateform
