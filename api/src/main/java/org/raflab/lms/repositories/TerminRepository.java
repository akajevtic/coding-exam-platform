package org.raflab.lms.repositories;

import java.util.List;
import java.time.Instant;
import org.raflab.lms.model.Termin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TerminRepository extends JpaRepository<Termin, Long> {

	List<Termin> findByKrajAfterOrderByPocetak(Instant vreme);
}
