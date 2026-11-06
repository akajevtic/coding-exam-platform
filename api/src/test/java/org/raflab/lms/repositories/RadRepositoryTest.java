package org.raflab.lms.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.raflab.lms.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class RadRepositoryTest {

	@Autowired
	RadRepository repo;

	@Autowired
	ZadatakRepository zadatakRepository;

	@Autowired
	KomentarRepository komentarRepository;

	@Autowired
	TestEntityManager em;

	Student ana;
	Zadatak zadatak;
	Rad rad;
	Instant preuzeto = Instant.now().truncatedTo(ChronoUnit.SECONDS);

	@BeforeEach
	void setUp() {
		ana = TestData.student(em, "Ana", "RN 1/2023");
		zadatak = TestData.zadatak(em, TestData.predmet(em, "RS"), TestData.rok(em, "Januarski"));
		rad = repo.save(rad(ana, zadatak));
		em.flush();
		em.clear();
	}

	@Test
	void radSeNalaziPoStudentuIZadatku() {
		Rad r = repo.findByStudentIdAndZadatakId(ana.getId(), zadatak.getId()).orElseThrow(AssertionError::new);
		assertThat(r.getStatus()).isEqualTo(StatusRada.U_IZRADI);
		assertThat(r.getVremePreuzimanja()).isEqualTo(preuzeto);
		assertThat(r.getZadatak().getGrupa().getTermin().getPredmet().getSifra()).isEqualTo("RS");
	}

	@Test
	void studentImaJedanRadPoZadatku() {
		assertThatThrownBy(() -> repo.saveAndFlush(rad(ana, zadatak)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void grupaImaJedanZadatak() {
		Zadatak drugi = new Zadatak();
		drugi.setGrupa(zadatakRepository.findById(zadatak.getId()).orElseThrow().getGrupa());
		drugi.setNaziv("Drugi");
		assertThatThrownBy(() -> zadatakRepository.saveAndFlush(drugi))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void ocenaIKomentariSeCuvaju() {
		Rad r = repo.findById(rad.getId()).orElseThrow();
		r.setStatus(StatusRada.PREDATO);
		r.setVremePredaje(preuzeto.plusSeconds(3600));
		r.setPoeni(new BigDecimal("7.50"));
		repo.save(r);
		komentarRepository.save(komentar(r, "src/Main.java", 12));
		komentarRepository.save(komentar(r, "src/Main.java", 3));
		em.flush();
		em.clear();

		assertThat(repo.findById(rad.getId()).orElseThrow().getPoeni()).isEqualByComparingTo("7.5");
		assertThat(komentarRepository.findByRadIdOrderByPutanjaFajlaAscLinijaAsc(rad.getId()))
				.extracting(Komentar::getLinija).containsExactly(3, 12);
	}

	private Rad rad(Student s, Zadatak z) {
		Rad r = new Rad();
		r.setStudent(s);
		r.setZadatak(z);
		r.setStatus(StatusRada.U_IZRADI);
		r.setVremePreuzimanja(preuzeto);
		return r;
	}

	private static Komentar komentar(Rad r, String fajl, int linija) {
		Komentar k = new Komentar();
		k.setRad(r);
		k.setPutanjaFajla(fajl);
		k.setLinija(linija);
		k.setTekst("komentar");
		return k;
	}
}
