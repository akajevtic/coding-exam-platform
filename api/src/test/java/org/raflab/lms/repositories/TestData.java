package org.raflab.lms.repositories;

import org.raflab.lms.model.*;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.Duration;
import java.time.Instant;

// pravi minimalne entitete za testove repozitorijuma
class TestData {

	static Student student(TestEntityManager em, String ime, String indeks) {
		Student s = new Student();
		s.setIme(ime);
		s.setPrezime("Test");
		s.setIndeks(indeks);
		s.setEmail(ime.toLowerCase() + "@raf.rs");
		return em.persist(s);
	}

	static Predmet predmet(TestEntityManager em, String sifra) {
		Predmet p = new Predmet();
		p.setSifra(sifra);
		p.setNaziv("Predmet " + sifra);
		return em.persist(p);
	}

	static IspitniRok rok(TestEntityManager em, String naziv) {
		IspitniRok r = new IspitniRok();
		r.setNaziv(naziv);
		r.setSkolskaGodina("2026/2027");
		return em.persist(r);
	}

	static Zadatak zadatak(TestEntityManager em, Predmet p, IspitniRok r) {
		Termin t = new Termin();
		t.setPredmet(p);
		t.setIspitniRok(r);
		t.setPocetak(Instant.now());
		t.setKraj(Instant.now().plus(Duration.ofHours(2)));
		em.persist(t);
		Grupa g = new Grupa();
		g.setTermin(t);
		g.setNaziv("Grupa 1");
		em.persist(g);
		Zadatak z = new Zadatak();
		z.setGrupa(g);
		z.setNaziv("Zadatak");
		return em.persist(z);
	}
}
