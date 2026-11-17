package fr.ece.autoloc.web.controller;

import fr.ece.autoloc.service.VehiculeService;
import fr.ece.autoloc.web.dto.VehiculeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class VehiculeController {

    private final VehiculeService vehiculeService;

    /** Catalogue public : véhicules disponibles à la location. */
    @GetMapping("/api/public/vehicules")
    public List<VehiculeDto> disponibles() {
        return vehiculeService.disponibles();
    }

    /** Parc complet, réservé aux utilisateurs authentifiés. */
    @GetMapping("/api/vehicules")
    public List<VehiculeDto> tous() {
        return vehiculeService.tous();
    }
}
