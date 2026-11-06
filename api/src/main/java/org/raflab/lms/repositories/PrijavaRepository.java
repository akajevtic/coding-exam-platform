package org.raflab.lms.repositories;

import java.util.List;
import org.raflab.lms.model.Prijava;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrijavaRepository extends JpaRepository<Prijava, Long> {

	// da li je student na spisku za predmet i rok termina
	boolean existsByStudentIdAndPredmetIdAndIspitniRokId(Long studentId, Long predmetId, Long ispitniRokId);

	void deleteByPredmetIdAndIspitniRokId(Long predmetId, Long ispitniRokId);

	List<Prijava> findByPredmetIdAndIspitniRokId(Long predmetId, Long ispitniRokId);
}
