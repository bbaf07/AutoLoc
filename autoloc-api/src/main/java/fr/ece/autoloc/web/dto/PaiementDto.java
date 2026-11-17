package fr.ece.autoloc.web.dto;

import fr.ece.autoloc.domain.ModePaiement;
import fr.ece.autoloc.domain.Paiement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaiementDto(Long id, BigDecimal montant, LocalDateTime datePaiement, ModePaiement modePaiement) {

    public static PaiementDto depuis(Paiement p) {
        return new PaiementDto(p.getIdPaiement(), p.getMontant(), p.getDatePaiement(), p.getModePaiement());
    }
}
