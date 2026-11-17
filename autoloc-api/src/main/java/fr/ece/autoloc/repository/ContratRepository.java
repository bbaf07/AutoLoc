package fr.ece.autoloc.repository;

import fr.ece.autoloc.domain.Contrat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContratRepository extends JpaRepository<Contrat, Long> {

    /** Contrat et paiements en une seule requête, utilisable hors transaction. */
    @Query("select c from Contrat c left join fetch c.paiements where c.idContrat = :id")
    Optional<Contrat> findAvecPaiements(@Param("id") Long id);
}
