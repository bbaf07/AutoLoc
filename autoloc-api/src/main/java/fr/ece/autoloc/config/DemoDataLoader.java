package fr.ece.autoloc.config;

import fr.ece.autoloc.domain.CategorieVehicule;
import fr.ece.autoloc.domain.StatutVehicule;
import fr.ece.autoloc.domain.Vehicule;
import fr.ece.autoloc.repository.VehiculeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Inserts a few demo vehicles at startup (dev profile only, and only if the table is empty).
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DemoDataLoader implements CommandLineRunner {

    private final VehiculeRepository vehiculeRepository;

    @Override
    public void run(String... args) {
        if (vehiculeRepository.count() > 0) {
            return;
        }
        List<Vehicule> demoVehicules = List.of(
                new Vehicule(null, "AB-123-CD", "Renault", "Clio", CategorieVehicule.CITADINE,
                        new BigDecimal("35.00"), StatutVehicule.DISPONIBLE),
                new Vehicule(null, "EF-456-GH", "Peugeot", "508", CategorieVehicule.BERLINE,
                        new BigDecimal("60.00"), StatutVehicule.DISPONIBLE),
                new Vehicule(null, "IJ-789-KL", "Dacia", "Duster", CategorieVehicule.SUV,
                        new BigDecimal("55.00"), StatutVehicule.MAINTENANCE)
        );
        vehiculeRepository.saveAll(demoVehicules);
        log.info("{} demo vehicles inserted", demoVehicules.size());
    }
}
