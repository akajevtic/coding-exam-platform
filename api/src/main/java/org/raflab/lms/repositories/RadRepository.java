package org.raflab.lms.repositories;

import org.springframework.data.jpa.repository.Query;
import org.raflab.lms.model.StatusRada;
import java.util.List;
import java.util.Optional;

import org.raflab.lms.model.Rad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RadRepository extends JpaRepository<Rad, Long> {

	Optional<Rad> findByStudentIdAndZadatakId(Long studentId, Long zadatakId);

	// rad studenta u bilo kojoj grupi termina (student polaze u jednoj grupi)
	Optional<Rad> findByStudentIdAndZadatakGrupaTerminId(Long studentId, Long terminId);

	List<Rad> findByZadatakGrupaTerminId(Long terminId);

	List<Rad> findByStudentIdOrderByVremePreuzimanjaDesc(Long studentId);

	// kriterijumi selekcije za nastavnika; null znaci "bilo koji"
	@Query("""
			select r from Rad r
			where (:predmetId is null or r.zadatak.grupa.termin.predmet.id = :predmetId)
			  and (:rokId is null or r.zadatak.grupa.termin.ispitniRok.id = :rokId)
			  and (:terminId is null or r.zadatak.grupa.termin.id = :terminId)
			  and (:grupaId is null or r.zadatak.grupa.id = :grupaId)
			  and (:ucionica is null or upper(r.zadatak.grupa.termin.ucionica) = upper(:ucionica))
			  and (:indeks is null or upper(r.student.indeks) = upper(:indeks))
			  and (:status is null or r.status = :status)
			order by r.zadatak.grupa.termin.pocetak desc, r.zadatak.grupa.naziv, r.student.indeks""")
	List<Rad> pretraga(Long predmetId, Long rokId, Long terminId, Long grupaId, String ucionica, String indeks, StatusRada status);
}
