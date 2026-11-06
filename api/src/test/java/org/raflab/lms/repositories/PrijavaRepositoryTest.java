package org.raflab.lms.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.raflab.lms.model.IspitniRok;
import org.raflab.lms.model.Predmet;
import org.raflab.lms.model.Prijava;
import org.raflab.lms.model.Student;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class PrijavaRepositoryTest {

	@Autowired
	PrijavaRepository repo;

	@Autowired
	TestEntityManager em;

	Student ana, petar;
	Predmet rs;
	IspitniRok januar, februar;

	@BeforeEach
	void setUp() {
		ana = TestData.student(em, "Ana", "RN 1/2023");
		petar = TestData.student(em, "Petar", "RN 3/2023");
		rs = TestData.predmet(em, "RS");
		januar = TestData.rok(em, "Januarski");
		februar = TestData.rok(em, "Februarski");
		repo.save(prijava(ana, rs, januar));
		em.flush();
		em.clear();
	}

	@Test
	void prijavljenStudentJeNaSpisku() {
		assertThat(repo.existsByStudentIdAndPredmetIdAndIspitniRokId(ana.getId(), rs.getId(), januar.getId())).isTrue();
	}

	@Test
	void neprijavljenStudentIliDrugiRokNisuNaSpisku() {
		assertThat(repo.existsByStudentIdAndPredmetIdAndIspitniRokId(petar.getId(), rs.getId(), januar.getId())).isFalse();
		assertThat(repo.existsByStudentIdAndPredmetIdAndIspitniRokId(ana.getId(), rs.getId(), februar.getId())).isFalse();
	}

	@Test
	void istaPrijavaNeMozeDvaPuta() {
		assertThatThrownBy(() -> repo.saveAndFlush(prijava(ana, rs, januar)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private static Prijava prijava(Student s, Predmet p, IspitniRok r) {
		Prijava pr = new Prijava();
		pr.setStudent(s);
		pr.setPredmet(p);
		pr.setIspitniRok(r);
		return pr;
	}
}
