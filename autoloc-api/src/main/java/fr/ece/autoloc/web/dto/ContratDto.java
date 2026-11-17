package fr.ece.autoloc.web.dto;

import fr.ece.autoloc.domain.Contrat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ContratDto(Long id, LocalDate dateSignature, BigDecimal montantTotal, boolean valide,
                         Long idReservation, BigDecimal totalPaye, List<PaiementDto> paiements) {

    public static ContratDto depuis(Contrat c) {
        List<PaiementDto> paiements = c.getPaiements().stream().map(PaiementDto::depuis).toList();
        BigDecimal totalPaye = paiements.stream().map(PaiementDto::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ContratDto(c.getIdContrat(), c.getDateSignature(), c.getMontantTotal(), c.isValide(),
                c.getReservation().getIdReservation(), totalPaye, paiements);
    }
}
