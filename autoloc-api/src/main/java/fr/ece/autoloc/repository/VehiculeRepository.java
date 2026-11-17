package fr.ece.autoloc.repository;

import fr.ece.autoloc.domain.Vehicule;
import fr.ece.autoloc.domain.StatutVehicule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehiculeRepository extends JpaRepository<Vehicule, Long> {

    List<Vehicule> findByStatut(StatutVehicule statut);

    Optional<Vehicule> findByImmatriculation(String immatriculation);
}
