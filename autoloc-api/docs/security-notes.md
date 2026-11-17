# Notes de sécurité — AutoLoc

## Ce qui est en place

- `spring-boot-starter-security`, avec une `SecurityFilterChain` déclarée dans `config/SecurityConfig.java`.
- Authentification **HTTP Basic** (Postman : *Authorization → Basic Auth*).
- Deux comptes en mémoire (`InMemoryUserDetailsManager`), réservés au TP :

| Compte | Rôle | Accès |
|---|---|---|
| `student` | `USER` | routes publiques et `/api/**` (hors admin) |
| `admin` | `ADMIN` | toutes les routes |

## Règles de routage (dans l'ordre d'évaluation)

| Motif | Règle | Pourquoi |
|---|---|---|
| `/actuator/health`, `/actuator/prometheus` | `permitAll()` | Prometheus collecte sans compte. Ces routes sont déclarées avant les règles larges. |
| `/api/public/**` | `permitAll()` | Le catalogue et le ping sont accessibles à tous. |
| `/api/admin/**` | `hasRole("ADMIN")` | Rapport confidentiel et suppression de contrat. |
| `/api/**` | `hasAnyRole("USER","ADMIN")` | Le reste de l'API exige d'être authentifié. |
| toute autre requête | `authenticated()` | Refus par défaut. |

### Pourquoi `/api/admin/**` doit précéder `/api/**`

Spring Security applique **la première règle qui correspond**. Si `/api/**` était déclaré en premier, `/api/admin/report` y correspondrait déjà : un simple `USER` obtiendrait l'accès, et la règle `ADMIN` ne serait jamais lue. Les motifs se déclarent donc du plus précis au plus général.

## Résultats observés (tests manuels avec curl, sans mock)

| Requête | Anonyme | USER | ADMIN |
|---|---|---|---|
| `GET /api/public/ping` | 200 | 200 | 200 |
| `GET /api/hello` | **401** | 200 | 200 |
| `GET /api/admin/report` | **401** | **403** | 200 |

Capture : `docs/captures/01_securite_200_401_403.png`. Les mêmes requêtes se rejouent dans Postman avec *Basic Auth*.

### 401 ou 403 ?

- **401 Unauthorized** : l'appelant n'est **pas authentifié**, qu'il n'ait envoyé aucun identifiant ou de mauvais identifiants. La réponse porte l'en-tête `WWW-Authenticate: Basic`, qui invite à s'identifier.
- **403 Forbidden** : l'appelant **est authentifié**, mais son rôle ne l'autorise pas. S'identifier de nouveau ne change rien.

## Encodage des mots de passe avec BCrypt

Les mots de passe sont stockés sous forme de **hachés BCrypt** (`BCryptPasswordEncoder`), jamais en clair :

- le hachage est **à sens unique**. Une fuite de la base ne révèle pas les mots de passe ;
- chaque haché contient un **sel aléatoire** : deux mots de passe identiques donnent deux hachés différents, ce qui rend inutiles les tables précalculées ;
- le **facteur de coût** (10 par défaut) rend chaque essai lent, donc les attaques par force brute coûteuses ;
- à la connexion, Spring hache le mot de passe saisi et compare les deux hachés.

## Limites des comptes en mémoire

Les comptes en mémoire suffisent pour ce TP : la configuration est immédiate et aucune table n'est nécessaire. Ils ne conviennent pas à la production :

- les comptes sont écrits dans le code. Changer un mot de passe ou ajouter un utilisateur demande de recompiler et de redéployer ;
- ils disparaissent à l'arrêt de l'application et ne se partagent pas entre plusieurs instances ;
- les mots de passe d'exemple (`student123`, `admin123`) sont visibles dans le dépôt Git : **ce ne sont pas de vrais identifiants** ;
- il n'y a ni inscription, ni verrouillage après plusieurs échecs, ni expiration.

## Charger les utilisateurs depuis MySQL

1. Créer une entité `Utilisateur` (login unique, haché BCrypt, actif) et une entité `Role`, ou une énumération, reliées en `@ManyToMany`.
2. Créer `UtilisateurRepository.findByLogin(String login)`.
3. Implémenter `UserDetailsService.loadUserByUsername()`, qui lit l'utilisateur et le convertit en `UserDetails` avec ses rôles (`ROLE_USER`, `ROLE_ADMIN`).
4. Supprimer le bean `InMemoryUserDetailsManager` et garder le `PasswordEncoder` BCrypt pour hacher à l'inscription.
5. Pour une application web ou mobile, remplacer HTTP Basic par des jetons (JWT / OAuth2 Resource Server) et passer en HTTPS.

## Note sur CSRF

L'API est **sans état** (`SessionCreationPolicy.STATELESS`) : aucun cookie de session n'est émis, et le navigateur ne peut pas envoyer d'identifiants à l'insu de l'utilisateur. La protection CSRF est donc désactivée **uniquement sur `/api/**`**. Si un front-end venait à utiliser une session par cookie, il faudrait la réactiver.
