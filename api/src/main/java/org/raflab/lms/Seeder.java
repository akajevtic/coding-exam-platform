package org.raflab.lms;

import lombok.RequiredArgsConstructor;
import org.raflab.lms.model.*;
import org.raflab.lms.repositories.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/*
 * Probni podaci za razvoj, samo kad je baza prazna.
 * Termin traje od sada do +3h, da bi preuzimanje i predaja radili odmah.
 * ana i marko su na spisku, petar nije (za proveru odbijanja).
 */
@Component
@RequiredArgsConstructor
public class Seeder implements CommandLineRunner {

    private final KorisnikRepository korisnikRepository;
    private final NastavnikRepository nastavnikRepository;
    private final StudentRepository studentRepository;
    private final PredmetRepository predmetRepository;
    private final IspitniRokRepository ispitniRokRepository;
    private final TerminRepository terminRepository;
    private final GrupaRepository grupaRepository;
    private final ZadatakRepository zadatakRepository;
    private final PrijavaRepository prijavaRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment env;

    @Override
    public void run(String... args) {
        if (korisnikRepository.count() > 0) return;   // baza je vec popunjena
        String lozinka = passwordEncoder.encode(env.getRequiredProperty("app.seed.lozinka"));

        Nastavnik n = new Nastavnik();
        n.setIme("Bojana");
        n.setPrezime("Dimic Surla");
        n.setEmail("nastavnik@raf.rs");
        nastavnikRepository.save(n);
        Korisnik nNalog = nalog(n.getEmail(), lozinka, Uloga.NASTAVNIK);
        nNalog.setNastavnik(n);
        korisnikRepository.save(nNalog);

        Predmet p = new Predmet();
        p.setSifra("RS");
        p.setNaziv("Razvoj softvera");
        predmetRepository.save(p);

        IspitniRok rok = new IspitniRok();
        rok.setNaziv("Kolokvijumska nedelja 1");
        rok.setSkolskaGodina("2026/2027");
        ispitniRokRepository.save(rok);

        Instant sada = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        Termin t = new Termin();
        t.setPredmet(p);
        t.setIspitniRok(rok);
        t.setPocetak(sada);
        t.setKraj(sada.plus(Duration.ofHours(3)));
        t.setUcionica("RAF1");
        terminRepository.save(t);

        for (String naziv : new String[]{"Grupa 1", "Grupa 2"}) {
            Grupa g = new Grupa();
            g.setTermin(t);
            g.setNaziv(naziv);
            grupaRepository.save(g);
            Zadatak z = new Zadatak();
            z.setGrupa(g);
            z.setNaziv("Kolokvijum 1 - " + naziv);
            zadatakRepository.save(z);
        }

        String[][] studenti = {
                {"Ana", "Anic", "RN 1/2023", "ana@raf.rs"},
                {"Marko", "Markovic", "RN 2/2023", "marko@raf.rs"},
                {"Petar", "Petrovic", "RN 3/2023", "petar@raf.rs"},
        };
        for (String[] s : studenti) {
            Student st = new Student();
            st.setIme(s[0]);
            st.setPrezime(s[1]);
            st.setIndeks(s[2]);
            st.setEmail(s[3]);
            studentRepository.save(st);
            Korisnik k = nalog(st.getEmail(), lozinka, Uloga.STUDENT);
            k.setStudent(st);
            korisnikRepository.save(k);
            if (!st.getIme().equals("Petar")) {
                Prijava pr = new Prijava();
                pr.setStudent(st);
                pr.setPredmet(p);
                pr.setIspitniRok(rok);
                prijavaRepository.save(pr);
            }
        }
    }

    private static Korisnik nalog(String email, String lozinka, Uloga uloga) {
        Korisnik k = new Korisnik();
        k.setEmail(email);
        k.setLozinka(lozinka);
        k.setUloga(uloga);
        return k;
    }
}
