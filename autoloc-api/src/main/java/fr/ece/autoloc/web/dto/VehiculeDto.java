package fr.ece.autoloc.web.dto;

import fr.ece.autoloc.domain.CategorieVehicule;
import fr.ece.autoloc.domain.StatutVehicule;
import fr.ece.autoloc.domain.Vehicule;

import java.math.BigDecimal;
import java.util.List;

public record VehiculeDto(Long id, String immatriculation, String marque, String modele,
                          CategorieVehicule categorie, BigDecimal tarifJournalier, StatutVehicule statut,
                          String agence, List<String> equipements) {

    public static VehiculeDto depuis(Vehicule v) {
        return new VehiculeDto(v.getIdVehicule(), v.getImmatriculation(), v.getMarque(), v.getModele(),
                v.getCategorie(), v.getTarifJournalier(), v.getStatut(),
                v.getAgence() == null ? null : v.getAgence().getNom(),
                v.getEquipements().stream().map(e -> e.getLibelle()).sorted().toList());
    }
}
