# Stratégies de chargement et de cascade — AutoLoc

Ce document justifie chaque association JPA du modèle AutoLoc : côté propriétaire, `FetchType`, `CascadeType` et `orphanRemoval`.

## Règles générales retenues

- **Tout est `LAZY`**, y compris les `@ManyToOne` et `@OneToOne`, qui sont `EAGER` par défaut en JPA. Une lecture de véhicule ne doit pas charger toute la chaîne agence → employés → …
- **La cascade de suppression est réservée aux enfants sans cycle de vie propre** : un paiement n'existe pas sans son contrat. Un véhicule, un employé ou un équipement survit à la suppression de ce qui le référence.
- **Les deux côtés sont tenus à jour** par des méthodes utilitaires : `Contrat.ajouterPaiement`, `Client.ajouterReservation`, `Reservation.attacherContrat` et `Vehicule.ajouterMaintenance`.
- **Pas de `@Data` Lombok** sur les entités. Seuls `@Getter`, `@Setter`, `@NoArgsConstructor` et `@AllArgsConstructor` sont utilisés, pour éviter la récursion infinie de `toString`, `equals` et `hashCode` sur les relations bidirectionnelles.
- **`spring.jpa.open-in-view=false`** : aucune requête ne part depuis la vue. Les données nécessaires hors transaction sont chargées par une requête `join fetch` (`ContratRepository.findAvecPaiements`) ou converties en DTO dans le service transactionnel.

## Tableau des associations

| Association | Multiplicité UML | Mapping et côté propriétaire | Clé générée | Fetch | Cascade | Justification |
|---|---|---|---|---|---|---|
| Contrat → Paiement | 1 — * (composition) | `@OneToMany(mappedBy="contrat")` / `@ManyToOne` ; **Paiement** possède la FK | `paiement.id_contrat` | LAZY | ALL + `orphanRemoval = true` | Un paiement dépend entièrement de son contrat. Il est enregistré avec lui, supprimé avec lui, et supprimé s'il est retiré de la liste. |
| Agence → Vehicule | 1 — * | `@OneToMany(mappedBy="agence")` / `@ManyToOne` ; **Vehicule** possède la FK | `vehicule.id_agence` | LAZY | Aucune | Un véhicule survit à la fermeture d'une agence : il peut être réaffecté. Supprimer une agence ne doit jamais supprimer le parc. |
| Agence → Employe | 1 — * | `@OneToMany(mappedBy="agence")` / `@ManyToOne` ; **Employe** possède la FK | `employe.id_agence` | LAZY | Aucune | Un employé peut être muté. La vie de son contrat de travail ne dépend pas de l'agence. |
| Vehicule ↔ Equipement | * — * | `@ManyToMany` + `@JoinTable` ; **Vehicule** possède la table de jointure | `vehicule_equipement(id_vehicule, id_equipement)` | LAZY | Aucune | Les équipements (GPS, climatisation…) sont partagés par plusieurs véhicules. Un `Set` évite les doublons dans la table de jointure. |
| Client → Reservation | 1 — * | `@OneToMany(mappedBy="client")` / `@ManyToOne(optional=false)` ; **Reservation** possède la FK | `reservation.id_client` (NOT NULL) | LAZY | PERSIST | Une réservation créée pour un client est enregistrée avec lui. Elle n'est pas supprimée avec lui : l'historique de location est une donnée comptable. |
| Reservation → Vehicule | * — 1 | `@ManyToOne(optional=false)` unidirectionnel ; **Reservation** possède la FK | `reservation.id_vehicule` (NOT NULL) | LAZY | Aucune | Le véhicule existe indépendamment de ses réservations. La navigation inverse n'est pas utile au métier : on ne charge pas toutes les réservations d'un véhicule. |
| Reservation ↔ Contrat | 1 — 0..1 | `@OneToOne(mappedBy="reservation")` / `@OneToOne` ; **Contrat** possède la FK unique | `contrat.id_reservation` (UNIQUE, NOT NULL) | LAZY | ALL, côté Reservation | Le contrat est la concrétisation d'une réservation. Il n'existe pas sans elle, donc il est créé et supprimé avec elle. |
| Vehicule → Maintenance | 1 — * | `@OneToMany(mappedBy="vehicule")` / `@ManyToOne(optional=false)` ; **Maintenance** possède la FK | `maintenance.id_vehicule` (NOT NULL) | LAZY | PERSIST | Une intervention déclarée sur le véhicule est enregistrée avec lui. Elle n'est pas supprimée par cascade, car l'historique d'entretien doit rester traçable. |

> Les noms de colonnes de clé étrangère sont fixés par `@JoinColumn` (`id_agence`, `id_contrat`, etc.). Sans cette annotation, Hibernate les déduirait du nom de l'attribut et de l'identifiant, par exemple `contrat_id_contrat`.

## Comparaison LAZY / EAGER sur Contrat → Paiement

Mesure faite en passant temporairement `Contrat.paiements` en `EAGER`, puis en revenant à `LAZY`.

| Fetch | SQL observé à la lecture d'un contrat (`findById`) | Conséquence |
|---|---|---|
| LAZY (configuration finale) | `select … from contrat where id_contrat=?`. Les paiements ne sont lus qu'au premier accès à `getPaiements()`. | La lecture est légère. Accéder à la collection après la fin de la transaction lève `LazyInitializationException`. |
| EAGER | La même requête est suivie d'un `select … from paiement where id_contrat=?` (ou d'une jointure), **à chaque** lecture de contrat. | L'accès est immédiat, mais chaque lecture coûte plus cher, même quand les paiements sont inutiles. |

**Choix final** : `LAZY`. Quand les paiements sont nécessaires, `ContratRepository.findAvecPaiements` les charge en une seule requête (`left join fetch`). Cela évite à la fois le problème N+1 et la `LazyInitializationException`.

## Vérification

Au démarrage (`ddl-auto=update`, logs `org.hibernate.SQL=DEBUG`), Hibernate crée les clés étrangères listées ci-dessus et la table de jointure `vehicule_equipement`. La démonstration de la cascade se fait par l'API :

- `POST /api/contrats/{id}/paiements` : le paiement est inséré **sans** `PaiementRepository`, uniquement par la cascade depuis le contrat ;
- `DELETE /api/admin/contrats/{id}` : le contrat et tous ses paiements sont supprimés.
