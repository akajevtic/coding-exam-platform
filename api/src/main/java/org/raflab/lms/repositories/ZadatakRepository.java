package org.raflab.lms.repositories;

import java.util.Optional;

import org.raflab.lms.model.Zadatak;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZadatakRepository extends JpaRepository<Zadatak, Long> {

	Optional<Zadatak> findByGrupaId(Long grupaId);
}
