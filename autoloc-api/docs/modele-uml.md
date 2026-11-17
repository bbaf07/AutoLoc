# Modèle conceptuel AutoLoc (diagramme de classes)

Ce diagramme correspond exactement aux entités JPA du paquet `fr.ece.autoloc.domain`. Les flèches indiquent la navigabilité (côté qui connaît l'autre), et le losange plein la composition Contrat ◆— Paiement.

```mermaid
classDiagram
    direction LR
    class Agence {
        Long idAgence
        String nom
        String ville
        String adresse
        String telephone
    }
    class Employe {
        Long idEmploye
        String nom
        String prenom
        RoleEmploye role
    }
    class Vehicule {
        Long idVehicule
        String immatriculation
        String marque
        String modele
        CategorieVehicule categorie
        BigDecimal tarifJournalier
        StatutVehicule statut
    }
    class Equipement {
        Long idEquipement
        String libelle
    }
    class Client {
        Long idClient
        String nom
        String prenom
        String email
        String telephone
        String numPermis
        LocalDate dateInscription
    }
    class Reservation {
        Long idReservation
        LocalDate dateDebut
        LocalDate dateFin
        StatutReservation statut
    }
    class Contrat {
        Long idContrat
        LocalDate dateSignature
        BigDecimal montantTotal
        boolean valide
    }
    class Paiement {
        Long idPaiement
        BigDecimal montant
        LocalDate datePaiement
        ModePaiement modePaiement
    }
    class Maintenance {
        Long idMaintenance
        LocalDate dateDebut
        LocalDate dateFin
        String description
    }

    Agence "1" <--> "*" Vehicule : possède
    Agence "1" <--> "*" Employe : emploie
    Vehicule "*" <--> "*" Equipement : équipé de
    Client "1" <--> "*" Reservation : effectue
    Reservation "*" --> "1" Vehicule : concerne
    Reservation "1" <--> "0..1" Contrat : donne lieu à
    Contrat "1" *-- "*" Paiement : réglé par
    Vehicule "1" <--> "*" Maintenance : subit
```

| Énumération | Valeurs |
|---|---|
| `StatutVehicule` | DISPONIBLE, LOUE, MAINTENANCE |
| `CategorieVehicule` | CITADINE, BERLINE, SUV, UTILITAIRE |
| `RoleEmploye` | AGENT, MANAGER |
| `StatutReservation` | EN_ATTENTE, CONFIRMEE, ANNULEE, TERMINEE |
| `ModePaiement` | CARTE, ESPECES, VIREMENT |

## Correspondance avec RentCar (TP2)

| RentCar (TP2, conception UML) | AutoLoc (workshops Spring Boot) |
|---|---|
| Vehicule, Categorie, Tarif | Vehicule (`categorie` en énumération, `tarifJournalier`) |
| Client | Client |
| Employe | Employe (+ rôle AGENT / MANAGER) |
| Reservation | Reservation |
| Location (contrat) et Paiement | Contrat 1 — * Paiement (composition) |
| (absent) | Agence, Equipement, Maintenance |
