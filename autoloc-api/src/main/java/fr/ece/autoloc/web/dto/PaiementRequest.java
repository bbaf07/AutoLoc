package fr.ece.autoloc.web.dto;

import fr.ece.autoloc.domain.ModePaiement;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaiementRequest(
        @NotNull @DecimalMin(value = "0.01", message = "le montant doit être positif") BigDecimal montant,
        @NotNull ModePaiement modePaiement) {
}
