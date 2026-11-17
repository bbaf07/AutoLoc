package fr.ece.autoloc.web.controller;

import fr.ece.autoloc.service.ContratService;
import fr.ece.autoloc.web.dto.ContratDto;
import fr.ece.autoloc.web.dto.PaiementRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ContratController {

    private final ContratService contratService;

    @GetMapping("/api/contrats/{id}")
    public ContratDto consulter(@PathVariable Long id) {
        return contratService.consulter(id);
    }

    @PostMapping("/api/contrats/{id}/paiements")
    @ResponseStatus(HttpStatus.CREATED)
    public ContratDto payer(@PathVariable Long id, @Valid @RequestBody PaiementRequest demande) {
        return contratService.payer(id, demande);
    }

    @DeleteMapping("/api/admin/contrats/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        contratService.supprimer(id);
    }
}
