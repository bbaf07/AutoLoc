package fr.ece.autoloc.config;

import fr.ece.autoloc.domain.*;
import fr.ece.autoloc.repository.AgenceRepository;
import fr.ece.autoloc.repository.ClientRepository;
import fr.ece.autoloc.repository.EmployeRepository;
import fr.ece.autoloc.repository.EquipementRepository;
import fr.ece.autoloc.repository.VehiculeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/** Jeu de démonstration inséré au démarrage en profil « dev », uniquement si la base est vide. */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DonneesDemo implements CommandLineRunner {

    private final AgenceRepository agenceRepository;
    private final EmployeRepository employeRepository;
    private final EquipementRepository equipementRepository;
    private final VehiculeRepository vehiculeRepository;
    private final ClientRepository clientRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (agenceRepository.count() > 0) {
            return;   // déjà initialisé
        }
        Agence agence = new Agence();
        agence.setNom("AutoLoc Paris Centre");
        agence.setVille("Paris");
        agence.setAdresse("10 rue Sextius Michel, 75015 Paris");
        agence.setTelephone("01 44 39 06 00");
        agenceRepository.save(agence);

        employeRepository.save(employe("Petit", "Sophie", RoleEmploye.AGENT, agence));
        employeRepository.save(employe("Martin", "Paul", RoleEmploye.MANAGER, agence));

        Equipement gps = equipementRepository.save(equipement("GPS"));
        Equipement clim = equipementRepository.save(equipement("Climatisation"));
        Equipement siege = equipementRepository.save(equipement("Siège enfant"));

        Vehicule clio = vehicule("AB-123-CD", "Renault", "Clio", CategorieVehicule.CITADINE, "39.00", agence, Set.of(clim));
        Vehicule golf = vehicule("KC-332-VG", "Volkswagen", "Golf", CategorieVehicule.BERLINE, "49.00", agence, Set.of(gps, clim));
        Vehicule c3 = vehicule("GA-214-RT", "Citroën", "C3 Aircross", CategorieVehicule.SUV, "55.00", agence, Set.of(gps, clim, siege));
        Vehicule master = vehicule("MN-905-FT", "Renault", "Master", CategorieVehicule.UTILITAIRE, "79.00", agence, Set.of());
        master.setStatut(StatutVehicule.MAINTENANCE);
        Maintenance vidange = new Maintenance();
        vidange.setDateDebut(LocalDate.now());
        vidange.setDescription("Vidange et contrôle des freins");
        master.ajouterMaintenance(vidange);   // enregistrée par cascade PERSIST
        vehiculeRepository.saveAll(java.util.List.of(clio, golf, c3, master));

        Client client = new Client();
        client.setNom("Durand");
        client.setPrenom("Karim");
        client.setEmail("karim.durand@example.org");
        client.setTelephone("06 12 34 56 78");
        client.setNumPermis("12AB34567");
        client.setDateInscription(LocalDate.now().minusMonths(3));

        Reservation reservation = new Reservation();
        reservation.setDateDebut(LocalDate.now());
        reservation.setDateFin(LocalDate.now().plusDays(3));
        reservation.setStatut(StatutReservation.CONFIRMEE);
        reservation.setVehicule(golf);
        client.ajouterReservation(reservation);   // enregistrée par cascade PERSIST

        Contrat contrat = new Contrat();
        contrat.setDateSignature(LocalDate.now());
        contrat.setMontantTotal(new BigDecimal("147.00"));
        contrat.setValide(true);
        reservation.attacherContrat(contrat);   // enregistré par cascade ALL depuis la réservation

        Paiement acompte = new Paiement();
        acompte.setMontant(new BigDecimal("50.00"));
        acompte.setDatePaiement(LocalDateTime.now());
        acompte.setModePaiement(ModePaiement.CARTE);
        contrat.ajouterPaiement(acompte);   // enregistré par cascade ALL depuis le contrat

        golf.setStatut(StatutVehicule.LOUE);
        clientRepository.save(client);
        log.info("Données de démonstration AutoLoc insérées : 1 agence, 4 véhicules, 1 client, 1 contrat");
    }

    private static Employe employe(String nom, String prenom, RoleEmploye role, Agence agence) {
        Employe e = new Employe();
        e.setNom(nom);
        e.setPrenom(prenom);
        e.setRole(role);
        e.setAgence(agence);
        return e;
    }

    private static Equipement equipement(String libelle) {
        Equipement e = new Equipement();
        e.setLibelle(libelle);
        return e;
    }

    private Vehicule vehicule(String immat, String marque, String modele, CategorieVehicule categorie,
                                     String tarif, Agence agence, Set<Equipement> equipements) {
        // Un véhicule déjà saisi au Workshop 1 garde ses valeurs : on le rattache seulement à l'agence
        Vehicule existant = vehiculeRepository.findByImmatriculation(immat).orElse(null);
        if (existant != null) {
            existant.setAgence(agence);
            existant.getEquipements().addAll(equipements);
            return existant;
        }
        Vehicule v = new Vehicule();
        v.setImmatriculation(immat);
        v.setMarque(marque);
        v.setModele(modele);
        v.setCategorie(categorie);
        v.setTarifJournalier(new BigDecimal(tarif));
        v.setStatut(StatutVehicule.DISPONIBLE);
        v.setAgence(agence);
        v.getEquipements().addAll(equipements);
        return v;
    }
}
