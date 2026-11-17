package fr.ece.autoloc.service;

import fr.ece.autoloc.domain.Contrat;
import fr.ece.autoloc.domain.Paiement;
import fr.ece.autoloc.repository.ContratRepository;
import fr.ece.autoloc.web.dto.ContratDto;
import fr.ece.autoloc.web.dto.PaiementRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ContratService {

    private final ContratRepository contratRepository;

    @Transactional(readOnly = true)
    public ContratDto consulter(Long id) {
        return ContratDto.depuis(trouverAvecPaiements(id));
    }

    /** Le paiement est enregistré par cascade depuis le contrat (CascadeType.ALL), sans PaiementRepository. */
    @Transactional
    public ContratDto payer(Long id, PaiementRequest demande) {
        Contrat contrat = trouverAvecPaiements(id);
        Paiement paiement = new Paiement();
        paiement.setMontant(demande.montant());
        paiement.setModePaiement(demande.modePaiement());
        paiement.setDatePaiement(LocalDateTime.now());
        contrat.ajouterPaiement(paiement);
        return ContratDto.depuis(contratRepository.saveAndFlush(contrat));
    }

    /** Supprime le contrat ; ses paiements disparaissent avec lui (composition). */
    @Transactional
    public void supprimer(Long id) {
        Contrat contrat = trouverAvecPaiements(id);
        contrat.getReservation().setContrat(null);
        contratRepository.delete(contrat);
    }

    private Contrat trouverAvecPaiements(Long id) {
        return contratRepository.findAvecPaiements(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrat " + id + " introuvable"));
    }
}
