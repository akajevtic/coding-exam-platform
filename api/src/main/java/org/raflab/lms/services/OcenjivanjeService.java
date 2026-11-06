package org.raflab.lms.services;

import lombok.RequiredArgsConstructor;
import org.raflab.lms.controllers.request.KomentarRequest;
import org.raflab.lms.controllers.request.OcenaRequest;
import org.raflab.lms.exceptions.ConflictException;
import org.raflab.lms.exceptions.ResourceNotFoundException;
import org.raflab.lms.model.*;
import org.raflab.lms.repositories.KomentarRepository;
import org.raflab.lms.repositories.PrijavaRepository;
import org.raflab.lms.repositories.RadRepository;
import org.raflab.lms.repositories.TerminRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * Nastavnik: pretraga i preuzimanje radova, komentari po fajlu i liniji, poeni, podaci za monitoring.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OcenjivanjeService {

    final RadRepository radRepository;
    final KomentarRepository komentarRepository;
    final TerminRepository terminRepository;
    final PrijavaRepository prijavaRepository;
    final SkladisteService skladisteService;

    public record Kriterijumi(Long predmetId, Long rokId, Long terminId, Long grupaId, String ucionica, String indeks, StatusRada status) {
    }

    // ZIP rada i ime pod kojim ide u zbirnu arhivu
    public record FajlRada(String ime, Path putanja) {
    }

    @Transactional(readOnly = true)
    public List<Rad> pretraga(Kriterijumi k) {
        return radRepository.pretraga(k.predmetId(), k.rokId(), k.terminId(), k.grupaId(),
                prazanUNull(k.ucionica()), k.indeks() == null || k.indeks().isBlank() ? null : PrijavaService.normalizujIndeks(k.indeks()),
                k.status());
    }

    // samo radovi koji imaju predat ili automatski sacuvan fajl
    @Transactional(readOnly = true)
    public List<FajlRada> fajlovi(Kriterijumi k) {
        return pretraga(k).stream()
                .filter(r -> r.getPutanjaResenja() != null)
                .map(r -> new FajlRada(imeUArhivi(r), skladisteService.putanja(r.getPutanjaResenja())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Rad rad(Long radId) {
        return radRepository.findById(radId).orElseThrow(() -> new ResourceNotFoundException("Rad ne postoji"));
    }

    @Transactional(readOnly = true)
    public Path fajl(Long radId) {
        Rad r = rad(radId);
        if (r.getPutanjaResenja() == null) throw new ResourceNotFoundException("Student nije predao resenje");
        return skladisteService.putanja(r.getPutanjaResenja());
    }

    @Transactional(readOnly = true)
    public List<Komentar> komentari(Long radId) {
        return komentarRepository.findByRadIdOrderByPutanjaFajlaAscLinijaAsc(rad(radId).getId());
    }

    public Rad oceni(Long radId, OcenaRequest request) {
        Rad r = rad(radId);
        if (r.getPutanjaResenja() == null) throw new ConflictException("Rad bez predatog resenja ne moze da se oceni");
        r.setPoeni(request.score());
        komentarRepository.deleteByRadId(r.getId());
        komentarRepository.flush();
        for (KomentarRequest kr : request.comments()) {
            Komentar k = new Komentar();
            k.setRad(r);
            k.setPutanjaFajla(kr.filePath().replace('\\', '/'));
            k.setLinija(kr.line());
            k.setTekst(kr.text());
            komentarRepository.save(k);
        }
        return radRepository.save(r);
    }

    public record Monitoring(Termin termin, List<Rad> radovi, List<Student> nisuPoceli, int naSpisku) {
    }

    // svi radovi termina + studenti sa spiska koji jos nisu preuzeli zadatak
    @Transactional(readOnly = true)
    public Monitoring monitoring(Long terminId) {
        Termin t = terminRepository.findById(terminId).orElseThrow(() -> new ResourceNotFoundException("Termin ne postoji"));
        List<Rad> radovi = radRepository.findByZadatakGrupaTerminId(terminId);
        Set<Long> poceli = radovi.stream().map(r -> r.getStudent().getId()).collect(Collectors.toSet());
        List<Prijava> spisak = prijavaRepository.findByPredmetIdAndIspitniRokId(t.getPredmet().getId(), t.getIspitniRok().getId());
        List<Student> nisuPoceli = new ArrayList<>();
        for (Prijava p : spisak) if (!poceli.contains(p.getStudent().getId())) nisuPoceli.add(p.getStudent());
        return new Monitoring(t, radovi, nisuPoceli, spisak.size());
    }

    private static String imeUArhivi(Rad r) {
        Grupa g = r.getZadatak().getGrupa();
        return deo(g.getTermin().getPredmet().getSifra()) + "/" + deo(g.getNaziv()) + "/" + deo(r.getStudent().getIndeks()) + ".zip";
    }

    // "RN 1/2023" -> "RN_1-2023", da kosa crta ne napravi novi folder u arhivi
    private static String deo(String s) {
        return s.replace('/', '-').replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String prazanUNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
