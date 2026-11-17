package fr.ece.autoloc.service;

import fr.ece.autoloc.domain.StatutVehicule;
import fr.ece.autoloc.repository.VehiculeRepository;
import fr.ece.autoloc.web.dto.VehiculeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehiculeService {

    private final VehiculeRepository vehiculeRepository;

    /** La conversion en DTO a lieu dans la transaction : les associations LAZY (agence, équipements) restent accessibles. */
    public List<VehiculeDto> disponibles() {
        return vehiculeRepository.findByStatut(StatutVehicule.DISPONIBLE).stream().map(VehiculeDto::depuis).toList();
    }

    public List<VehiculeDto> tous() {
        return vehiculeRepository.findAll().stream().map(VehiculeDto::depuis).toList();
    }
}
