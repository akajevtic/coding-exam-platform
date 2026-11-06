package org.raflab.lms.utils;

import org.raflab.lms.controllers.response.AttemptResponse;
import org.raflab.lms.controllers.response.ExamResponse;
import org.raflab.lms.controllers.response.FeedbackResponse;
import org.raflab.lms.controllers.response.KomentarResponse;
import org.raflab.lms.controllers.response.MonitoringResponse;
import org.raflab.lms.controllers.response.StudentResponse;
import org.raflab.lms.controllers.response.SubmissionResponse;
import org.raflab.lms.controllers.response.IspitniRokResponse;
import org.raflab.lms.controllers.response.PredmetResponse;
import org.raflab.lms.controllers.response.TerminResponse;
import org.raflab.lms.model.Grupa;
import org.raflab.lms.model.IspitniRok;
import org.raflab.lms.model.Komentar;
import org.raflab.lms.model.Rad;
import org.raflab.lms.model.StatusRada;
import org.raflab.lms.model.Student;
import org.raflab.lms.model.Predmet;
import org.raflab.lms.model.Termin;
import org.raflab.lms.model.Zadatak;

import org.raflab.lms.services.OcenjivanjeService.Monitoring;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;

public class Converters {

    public static PredmetResponse toPredmetResponse(Predmet p) {
        return new PredmetResponse(p.getId(), p.getSifra(), p.getNaziv());
    }

    public static IspitniRokResponse toIspitniRokResponse(IspitniRok r) {
        return new IspitniRokResponse(r.getId(), r.getNaziv(), r.getSkolskaGodina());
    }

    public static ExamResponse toExamResponse(Zadatak z) {
        return new ExamResponse(z.getId(), z.getGrupa().getTermin().getId(), z.getGrupa().getId(), z.getGrupa().getNaziv(),
                z.getNaziv(), z.getOpis(), z.getPutanjaArhive() != null);
    }

    public static TerminResponse toTerminResponse(Termin t, List<Zadatak> zadaci) {
        return new TerminResponse(t.getId(), toPredmetResponse(t.getPredmet()), toIspitniRokResponse(t.getIspitniRok()),
                t.getPocetak(), t.getKraj(), t.getUcionica(), zadaci.stream().map(Converters::toExamResponse).toList());
    }

    public static StudentResponse toStudentResponse(Student s) {
        return new StudentResponse(s.getId(), s.getIndeks(), s.getIme(), s.getPrezime(), s.getEmail());
    }

    public static AttemptResponse toAttemptResponse(Rad r, boolean imaSacuvanoResenje) {
        Zadatak z = r.getZadatak();
        Termin t = z.getGrupa().getTermin();
        return new AttemptResponse(r.getId(), z.getId(), t.getId(), t.getPredmet().getNaziv(), z.getGrupa().getNaziv(), z.getNaziv(),
                r.getStatus(), r.getVremePreuzimanja(), r.getVremePredaje(), t.getPocetak(), t.getKraj(), Instant.now(), imaSacuvanoResenje);
    }

    public static KomentarResponse toKomentarResponse(Komentar k) {
        return new KomentarResponse(k.getId(), k.getPutanjaFajla(), k.getLinija(), k.getTekst());
    }

    public static FeedbackResponse toFeedbackResponse(Rad r, List<Komentar> komentari) {
        return new FeedbackResponse(r.getId(), r.getPoeni(), komentari.stream().map(Converters::toKomentarResponse).toList());
    }

    public static SubmissionResponse toSubmissionResponse(Rad r) {
        Grupa g = r.getZadatak().getGrupa();
        Termin t = g.getTermin();
        Student s = r.getStudent();
        return new SubmissionResponse(r.getId(), r.getZadatak().getId(), t.getId(), t.getPredmet().getNaziv(), t.getIspitniRok().getNaziv(),
                g.getNaziv(), t.getUcionica(), s.getIndeks(), s.getIme(), s.getPrezime(), r.getStatus(),
                r.getVremePreuzimanja(), r.getVremePredaje(), trajanje(r), r.getPoeni(), r.getPutanjaResenja() != null);
    }

    public static MonitoringResponse toMonitoringResponse(Monitoring m) {
        List<MonitoringResponse.Student> studenti = new ArrayList<>();
        for (Rad r : m.radovi()) {
            Student s = r.getStudent();
            studenti.add(new MonitoringResponse.Student(s.getIndeks(), s.getIme(), s.getPrezime(), r.getZadatak().getGrupa().getNaziv(),
                    r.getStatus().name(), r.getVremePreuzimanja(), r.getVremePredaje(), trajanje(r)));
        }
        for (Student s : m.nisuPoceli())
            studenti.add(new MonitoringResponse.Student(s.getIndeks(), s.getIme(), s.getPrezime(), null, "NIJE_ZAPOCETO", null, null, null));
        studenti.sort(Comparator.comparing(MonitoringResponse.Student::studentNumber));

        int predato = (int) m.radovi().stream().filter(r -> r.getStatus() == StatusRada.PREDATO).count();
        OptionalDouble avg = m.radovi().stream().map(Converters::trajanje).filter(d -> d != null).mapToLong(Long::longValue).average();
        Double prosek = avg.isPresent() ? avg.getAsDouble() : null;
        return new MonitoringResponse(m.termin().getId(), Instant.now(), m.naSpisku(), m.radovi().size(), predato, prosek, studenti);
    }

    // vreme izrade u sekundama, samo za predate radove
    private static Long trajanje(Rad r) {
        if (r.getStatus() != StatusRada.PREDATO || r.getVremePredaje() == null) return null;
        return Duration.between(r.getVremePreuzimanja(), r.getVremePredaje()).toSeconds();
    }
}
