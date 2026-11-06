package org.raflab.lms.repositories;

import java.util.List;

import org.raflab.lms.model.Komentar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KomentarRepository extends JpaRepository<Komentar, Long> {

	List<Komentar> findByRadIdOrderByPutanjaFajlaAscLinijaAsc(Long radId);

	void deleteByRadId(Long radId);
}
