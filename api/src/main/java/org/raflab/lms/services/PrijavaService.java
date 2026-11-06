package org.raflab.lms.services;

import org.raflab.lms.controllers.request.StudentIdentity;
import org.raflab.lms.controllers.response.SpisakResponse;
import org.raflab.lms.exceptions.ResourceNotFoundException;
import org.raflab.lms.model.Korisnik;
import org.raflab.lms.model.Prijava;
import org.raflab.lms.model.Student;
import org.raflab.lms.model.Termin;
import org.raflab.lms.model.Uloga;
import org.raflab.lms.repositories.KorisnikRepository;
import org.raflab.lms.repositories.PrijavaRepository;
import org.raflab.lms.repositories.StudentRepository;
import org.raflab.lms.repositories.TerminRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * Spisak prijavljenih se vodi po predmetu i roku termina.
 * Novi spisak zamenjuje prethodni; nepoznati studenti se upisuju u bazu i dobijaju nalog
 * (email sa spiska + pocetna lozinka iz app.student.pocetna-lozinka).
 */
@Service
@Transactional
public class PrijavaService {

    final TerminRepository terminRepository;
    final StudentRepository studentRepository;
    final PrijavaRepository prijavaRepository;
    final KorisnikRepository korisnikRepository;
    final PasswordEncoder passwordEncoder;
    final String pocetnaLozinka;

    public PrijavaService(TerminRepository terminRepository, StudentRepository studentRepository,
                          PrijavaRepository prijavaRepository, KorisnikRepository korisnikRepository,
                          PasswordEncoder passwordEncoder, @Value("${app.student.pocetna-lozinka}") String pocetnaLozinka) {
        this.terminRepository = terminRepository;
        this.studentRepository = studentRepository;
        this.prijavaRepository = prijavaRepository;
        this.korisnikRepository = korisnikRepository;
        this.passwordEncoder = passwordEncoder;
        this.pocetnaLozinka = pocetnaLozinka;
    }

    public SpisakResponse postaviSpisak(Long terminId, List<StudentIdentity> studenti) {
        Termin t = terminRepository.findById(terminId).orElseThrow(() -> new ResourceNotFoundException("Termin ne postoji"));

        Map<String, StudentIdentity> poIndeksu = new LinkedHashMap<>();   // isti indeks dva puta u spisku se racuna jednom
        for (StudentIdentity s : studenti) poIndeksu.putIfAbsent(normalizujIndeks(s.studentNumber()), s);

        prijavaRepository.deleteByPredmetIdAndIspitniRokId(t.getPredmet().getId(), t.getIspitniRok().getId());
        prijavaRepository.flush();   // brisanje pre upisa, inace Hibernate prvo radi insert i krsi unique

        int novih = 0;
        for (Map.Entry<String, StudentIdentity> e : poIndeksu.entrySet()) {
            Student student = studentRepository.findByIndeksIgnoreCase(e.getKey()).orElse(null);
            if (student == null) {
                student = noviStudent(e.getKey(), e.getValue());
                novih++;
            }
            Prijava p = new Prijava();
            p.setStudent(student);
            p.setPredmet(t.getPredmet());
            p.setIspitniRok(t.getIspitniRok());
            prijavaRepository.save(p);
        }
        return new SpisakResponse(poIndeksu.size(), novih);
    }

    // "rn  12/2023 " -> "RN 12/2023"
    static String normalizujIndeks(String indeks) {
        return indeks.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    private Student noviStudent(String indeks, StudentIdentity s) {
        Student student = new Student();
        student.setIndeks(indeks);
        student.setIme(s.firstName().trim());
        student.setPrezime(s.lastName().trim());
        student.setEmail(s.email().trim().toLowerCase());
        studentRepository.save(student);
        if (!korisnikRepository.existsByEmailIgnoreCase(student.getEmail())) {
            Korisnik k = new Korisnik();
            k.setEmail(student.getEmail());
            k.setLozinka(passwordEncoder.encode(pocetnaLozinka));
            k.setUloga(Uloga.STUDENT);
            k.setStudent(student);
            korisnikRepository.save(k);
        }
        return student;
    }
}
