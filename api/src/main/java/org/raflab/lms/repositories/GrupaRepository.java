package org.raflab.lms.repositories;

import java.util.Optional;
import org.raflab.lms.model.Grupa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrupaRepository extends JpaRepository<Grupa, Long> {

	Optional<Grupa> findByTerminIdAndNazivIgnoreCase(Long terminId, String naziv);
}
