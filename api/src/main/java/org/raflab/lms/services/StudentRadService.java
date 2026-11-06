package org.raflab.lms.services;

import lombok.RequiredArgsConstructor;
import org.raflab.lms.controllers.request.PrijavaNaPolaganjeRequest;
import org.raflab.lms.exceptions.ConflictException;
import org.raflab.lms.exceptions.ForbiddenException;
import org.raflab.lms.exceptions.ResourceNotFoundException;
import org.raflab.lms.model.*;
import org.raflab.lms.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/*
 * Studentski tok: prijava na polaganje -> preuzimanje zadatka -> predaja -> povratna informacija.
 * Sva pravila (spisak, vremenski prozor, vlasnistvo rada) proveravaju se ovde, na serverskom satu.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StudentRadService {

    final StudentRepository studentRepository;
    final TerminRepository terminRepository;
    final GrupaRepository grupaRepository;
    final ZadatakRepository zadatakRepository;
    final PrijavaRepository prijavaRepository;
    final RadRepository radRepository;
    final KomentarRepository komentarRepository;
    final SkladisteService skladisteService;

    @Transactional(readOnly = true)
    public Student student(Long studentId) {
        return studentRepository.findById(studentId).orElseThrow(() -> new ResourceNotFoundException("Student ne postoji"));
    }

    // termini koji jos nisu zavrseni, za predmet i rok na koje je student prijavljen
    @Transactional(readOnly = true)
    public List<Termin> dostupniTermini(Long studentId) {
        return terminRepository.findByKrajAfterOrderByPocetak(Instant.now()).stream()
                .filter(t -> naSpisku(studentId, t))
                .toList();
    }

    public Rad prijaviSe(Long studentId, PrijavaNaPolaganjeRequest request) {
        Student student = student(studentId);
        if (!PrijavaService.normalizujIndeks(request.studentNumber()).equals(student.getIndeks()))
            throw new ForbiddenException("Uneti indeks ne odgovara prijavljenom nalogu");
        Termin termin = terminRepository.findById(request.termId())
                .orElseThrow(() -> new ResourceNotFoundException("Termin ne postoji"));
        Grupa grupa = grupaRepository.findByTerminIdAndNazivIgnoreCase(termin.getId(), request.group().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Grupa ne postoji u ovom terminu"));
        if (!naSpisku(studentId, termin))
            throw new ForbiddenException("Niste na spisku prijavljenih za ovo polaganje");
        proveriDaJeUToku(termin);

        // ponovna prijava (npr. sa drugog racunara) vraca isti rad i ne menja vreme preuzimanja
        Rad postojeci = radRepository.findByStudentIdAndZadatakGrupaTerminId(studentId, termin.getId()).orElse(null);
        if (postojeci != null) {
            if (!postojeci.getZadatak().getGrupa().getId().equals(grupa.getId()))
                throw new ConflictException("Vec polazete u grupi " + postojeci.getZadatak().getGrupa().getNaziv());
            return postojeci;
        }
        Zadatak zadatak = zadatakRepository.findByGrupaId(grupa.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Grupa nema zadatak"));
        if (zadatak.getPutanjaArhive() == null) throw new ConflictException("Nastavnik jos nije postavio zadatak");

        Rad rad = new Rad();
        rad.setStudent(student);
        rad.setZadatak(zadatak);
        rad.setStatus(StatusRada.U_IZRADI);
        rad.setVremePreuzimanja(Instant.now());
        return radRepository.save(rad);
    }

    @Transactional(readOnly = true)
    public List<Rad> mojiRadovi(Long studentId) {
        return radRepository.findByStudentIdOrderByVremePreuzimanjaDesc(studentId);
    }

    // tudji rad se prijavljuje kao nepostojeci, da se ne otkriva da postoji
    @Transactional(readOnly = true)
    public Rad mojRad(Long studentId, Long radId) {
        return radRepository.findById(radId)
                .filter(r -> r.getStudent().getId().equals(studentId))
                .orElseThrow(() -> new ResourceNotFoundException("Rad ne postoji"));
    }

    @Transactional(readOnly = true)
    public Path arhivaZadatka(Long studentId, Long radId) {
        Rad rad = mojRad(studentId, radId);
        proveriDaJeUToku(rad.getZadatak().getGrupa().getTermin());
        return skladisteService.putanja(rad.getZadatak().getPutanjaArhive());
    }

    // konacno=false je automatsko cuvanje: fajl se cuva, a rad ostaje U_IZRADI
    public Rad predaj(Long studentId, Long radId, MultipartFile fajl, boolean konacno) {
        Rad rad = mojRad(studentId, radId);
        Instant sada = Instant.now();
        if (sada.isAfter(rad.getZadatak().getGrupa().getTermin().getKraj()))
            throw new ForbiddenException("Rok za predaju je istekao");
        rad.setPutanjaResenja(skladisteService.sacuvajZip(fajl, "predaje/" + rad.getId() + ".zip"));
        if (konacno) {
            rad.setStatus(StatusRada.PREDATO);
            rad.setVremePredaje(sada);
        }
        return radRepository.save(rad);
    }

    // poslednje sacuvano resenje: za nastavak rada na drugom racunaru, a posle termina za pregled komentara
    @Transactional(readOnly = true)
    public Path sacuvanoResenje(Long studentId, Long radId) {
        Rad rad = mojRad(studentId, radId);
        if (rad.getPutanjaResenja() == null) throw new ResourceNotFoundException("Nema sacuvanog resenja");
        return skladisteService.putanja(rad.getPutanjaResenja());
    }

    @Transactional(readOnly = true)
    public List<Komentar> komentari(Long studentId, Long radId) {
        return komentarRepository.findByRadIdOrderByPutanjaFajlaAscLinijaAsc(mojRad(studentId, radId).getId());
    }

    public boolean imaSacuvanoResenje(Rad rad) {
        return rad.getPutanjaResenja() != null && Files.exists(skladisteService.putanja(rad.getPutanjaResenja()));
    }

    private boolean naSpisku(Long studentId, Termin t) {
        return prijavaRepository.existsByStudentIdAndPredmetIdAndIspitniRokId(studentId, t.getPredmet().getId(), t.getIspitniRok().getId());
    }

    private static void proveriDaJeUToku(Termin t) {
        Instant sada = Instant.now();
        if (sada.isBefore(t.getPocetak())) throw new ForbiddenException("Polaganje jos nije pocelo");
        if (sada.isAfter(t.getKraj())) throw new ForbiddenException("Polaganje je zavrseno");
    }
}
