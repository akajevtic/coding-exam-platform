package org.raflab.lms.services;

import lombok.RequiredArgsConstructor;
import org.raflab.lms.controllers.request.GrupaRequest;
import org.raflab.lms.controllers.request.TerminRequest;
import org.raflab.lms.exceptions.BadRequestException;
import org.raflab.lms.exceptions.ResourceNotFoundException;
import org.raflab.lms.model.*;
import org.raflab.lms.repositories.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * Nastavnik priprema polaganje: predmet i rok (sifarnici), termin, grupa sa zadatkom i ZIP zadatka.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PolaganjeService {

    final PredmetRepository predmetRepository;
    final IspitniRokRepository ispitniRokRepository;
    final TerminRepository terminRepository;
    final GrupaRepository grupaRepository;
    final ZadatakRepository zadatakRepository;
    final SkladisteService skladisteService;

    public Predmet kreirajPredmet(String sifra, String naziv) {
        Predmet p = new Predmet();
        p.setSifra(sifra.trim());
        p.setNaziv(naziv.trim());
        return predmetRepository.save(p);
    }

    @Transactional(readOnly = true)
    public List<Predmet> sviPredmeti() {
        return predmetRepository.findAll(Sort.by("sifra"));
    }

    public IspitniRok kreirajRok(String naziv, String skolskaGodina) {
        IspitniRok r = new IspitniRok();
        r.setNaziv(naziv.trim());
        r.setSkolskaGodina(skolskaGodina.trim());
        return ispitniRokRepository.save(r);
    }

    @Transactional(readOnly = true)
    public List<IspitniRok> sviRokovi() {
        return ispitniRokRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    public Termin kreirajTermin(TerminRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) throw new BadRequestException("Kraj termina mora biti posle pocetka");
        Termin t = new Termin();
        t.setPredmet(predmetRepository.findById(request.subjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Predmet ne postoji")));
        t.setIspitniRok(ispitniRokRepository.findById(request.examPeriodId())
                .orElseThrow(() -> new ResourceNotFoundException("Ispitni rok ne postoji")));
        t.setPocetak(request.startsAt());
        t.setKraj(request.endsAt());
        t.setUcionica(request.room());
        return terminRepository.save(t);
    }

    @Transactional(readOnly = true)
    public List<Termin> sviTermini() {
        return terminRepository.findAll(Sort.by(Sort.Direction.DESC, "pocetak"));
    }

    // shortcut: ucitava sve zadatke odjednom, dodati filter po roku kad termina bude mnogo
    @Transactional(readOnly = true)
    public Map<Long, List<Zadatak>> zadaciPoTerminu() {
        return zadatakRepository.findAll(Sort.by("grupa.naziv")).stream()
                .collect(Collectors.groupingBy(z -> z.getGrupa().getTermin().getId()));
    }

    // grupa i njen zadatak nastaju zajedno; to je "polaganje" iz API ugovora
    public Zadatak kreirajGrupu(Long terminId, GrupaRequest request) {
        Grupa g = new Grupa();
        g.setTermin(termin(terminId));
        g.setNaziv(request.group().trim());
        grupaRepository.save(g);
        Zadatak z = new Zadatak();
        z.setGrupa(g);
        z.setNaziv(request.title().trim());
        z.setOpis(request.description());
        return zadatakRepository.save(z);
    }

    public void postaviArhivu(Long zadatakId, MultipartFile fajl) {
        Zadatak z = zadatakRepository.findById(zadatakId)
                .orElseThrow(() -> new ResourceNotFoundException("Polaganje ne postoji"));
        z.setPutanjaArhive(skladisteService.sacuvajZip(fajl, "zadaci/" + z.getId() + ".zip"));
        // samo ime, bez putanje koju je klijent mozda poslao
        String ime = fajl.getOriginalFilename();
        z.setNazivArhive(ime == null || ime.isBlank() ? "zadatak.zip" : Path.of(ime.replace('\\', '/')).getFileName().toString());
    }

    @Transactional(readOnly = true)
    public Termin termin(Long terminId) {
        return terminRepository.findById(terminId).orElseThrow(() -> new ResourceNotFoundException("Termin ne postoji"));
    }
}
