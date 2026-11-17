# Notes de monitoring — AutoLoc

## Architecture

```
 autoloc-api (IntelliJ ou java -jar, port 8080)
   └─ Spring Boot Actuator ── /actuator/health
   └─ Micrometer (registre Prometheus) ── /actuator/prometheus
          ▲ collecte toutes les 5 s (host.docker.internal:8080)
 Prometheus (conteneur, port 9090) ── stocke les séries temporelles
          ▲ requêtes PromQL (http://prometheus:9090, réseau Docker)
 Grafana (conteneur, port 3000) ── tableau « AutoLoc — Application Monitoring »
```

| Composant | Rôle |
|---|---|
| Spring Boot Actuator | expose l'état de santé et les métriques de l'application |
| Micrometer Prometheus Registry | met les métriques au format texte attendu par Prometheus |
| Prometheus | collecte et stocke les métriques sous forme de séries temporelles |
| Grafana | interroge Prometheus et affiche les graphiques |

## Fichiers

- `pom.xml` : `spring-boot-starter-actuator` et `micrometer-registry-prometheus`, versions gérées par Spring Boot.
- `application.properties` : expose `health` et `prometheus`, active l'histogramme HTTP pour le p95 et ajoute le tag `application=autoloc-api` à chaque métrique.
- `monitoring/prometheus.yml` : collecte de `host.docker.internal:8080/actuator/prometheus` toutes les 5 s.
- `docker-compose.yml` : conteneurs Prometheus et Grafana. `extra_hosts` rend `host.docker.internal` joignable aussi sous Linux.
- `monitoring/grafana/provisioning/` et `monitoring/grafana/dashboards/autoloc-monitoring.json` : la source de données Prometheus et le tableau de bord sont **créés automatiquement** au démarrage de Grafana, sans clic manuel à refaire.

## Lancement

```bash
# 1. démarrer l'API (IntelliJ, ou en ligne de commande)
mvn spring-boot:run
# 2. démarrer la supervision
docker compose up -d
docker compose ps
```

- Prometheus : http://localhost:9090 → *Status → Targets*. La cible `autoloc-api` doit être **UP** (capture `docs/captures/02_prometheus_targets.png`).
- Grafana : http://localhost:3000, tableau **AutoLoc — Application Monitoring** (capture `docs/captures/03_grafana_dashboard.png`).
- Arrêt : `docker compose down`, puis arrêt de l'API dans IntelliJ.

## Métriques retenues (6 panneaux)

| Panneau | PromQL | Ce qu'on surveille |
|---|---|---|
| Mémoire JVM utilisée | `sum(jvm_memory_used_bytes) by (area)` | heap et non-heap ; une heap qui ne redescend plus après le GC signale une fuite |
| Utilisation CPU du processus | `process_cpu_usage` | charge de la JVM |
| Requêtes HTTP par seconde | `sum(rate(http_server_requests_seconds_count[1m])) by (uri)` | trafic, détaillé par route |
| Temps de réponse HTTP p95 | `histogram_quantile(0.95, sum by (le)(rate(http_server_requests_seconds_bucket[5m])))` | latence ressentie par 95 % des appels |
| Threads JVM actifs | `jvm_threads_live_threads` | saturation du pool de threads de Tomcat |
| Réponses 401 / 403 par minute | `sum(increase(http_server_requests_seconds_count{status=~"401\|403"}[1m])) by (status)` | refus d'authentification et d'autorisation, signe d'erreurs côté client ou de tentatives d'accès |

Le trafic a été généré par des appels répétés à `/api/public/ping`, `/api/hello`, `/api/public/vehicules`, `/api/admin/report` et `/api/contrats/{id}`. Les courbes réagissent en quelques secondes.

## Problèmes identifiés et solutions

Les points 1, 2 et 4 sont des pièges connus, traités dès la configuration. Le point 3 a été observé pendant les tests.

1. **Panneau p95 vide.** Par défaut, Micrometer ne publie pas les compartiments d'histogramme (`_bucket`), donc `histogram_quantile` ne renvoie rien. **Solution** : `management.metrics.distribution.percentiles-histogram.http.server.requests=true`.
2. **Accès refusé à `/actuator/prometheus` (401).** Spring Security protège toutes les routes par défaut, et Prometheus ne s'authentifie pas. **Solution** : `permitAll()` sur `/actuator/health` et `/actuator/prometheus`, placé **avant** les règles larges. Seuls `health` et `prometheus` sont exposés, et rien d'autre (pas `env` ni `beans`).
3. **Requêtes classées `uri="UNKNOWN"`.** Les appels refusés par Spring Security (401 et 403) n'atteignent jamais un contrôleur, donc Micrometer ne connaît pas leur modèle d'URL. Ce n'est pas une panne : ces requêtes se suivent dans le panneau *Réponses 401 / 403*, filtré par `status` plutôt que par `uri`.
4. **Grafana ne joignait pas Prometheus avec `localhost:9090`.** Dans un conteneur, `localhost` désigne le conteneur lui-même. **Solution** : l'URL `http://prometheus:9090`, qui utilise le nom de service Docker.

## Sécurité de la supervision

- Grafana est ouvert en **lecture seule anonyme** (`GF_AUTH_ANONYMOUS_ORG_ROLE=Viewer`) pour la démonstration en local. L'administration reste protégée. Au premier accès admin, le mot de passe par défaut `admin/admin` doit être changé.
- En production, il faudrait ne pas publier `show-details=always`, protéger `/actuator/prometheus` (réseau interne ou authentification de la collecte) et fixer les versions des images au lieu de `latest`.
