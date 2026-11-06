package org.raflab.lms.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.raflab.lms.controllers.request.PrijavaNaPolaganjeRequest;
import org.raflab.lms.controllers.response.AttemptResponse;
import org.raflab.lms.controllers.response.FeedbackResponse;
import org.raflab.lms.controllers.response.StudentResponse;
import org.raflab.lms.controllers.response.TerminResponse;
import org.raflab.lms.model.Rad;
import org.raflab.lms.model.Zadatak;
import org.raflab.lms.services.PolaganjeService;
import org.raflab.lms.services.PristupService;
import org.raflab.lms.services.StudentRadService;
import org.raflab.lms.utils.Converters;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/*
 * Studentski tok. Uloga STUDENT se proverava u SecurityConfig za /student/**,
 * a vlasnistvo rada u StudentRadService (student vidi samo svoje radove).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/student")
public class StudentController {

	final StudentRadService studentRadService;
	final PolaganjeService polaganjeService;
	final PristupService pristupService;

	@GetMapping("/me")
	public StudentResponse ja() {
		return Converters.toStudentResponse(studentRadService.student(pristupService.ulogovaniStudentId()));
	}

	// termini u kojima student sme da polaze (prijavljen je, termin nije zavrsen), sa grupama
	@GetMapping("/terms")
	public List<TerminResponse> termini() {
		Map<Long, List<Zadatak>> zadaci = polaganjeService.zadaciPoTerminu();
		return studentRadService.dostupniTermini(pristupService.ulogovaniStudentId()).stream()
				.map(t -> Converters.toTerminResponse(t, zadaci.getOrDefault(t.getId(), List.of())))
				.toList();
	}

	@PostMapping("/attempts")
	@ResponseStatus(HttpStatus.CREATED)
	public AttemptResponse prijava(@RequestBody @Valid PrijavaNaPolaganjeRequest request) {
		return odgovor(studentRadService.prijaviSe(pristupService.ulogovaniStudentId(), request));
	}

	@GetMapping("/attempts")
	public List<AttemptResponse> mojiRadovi() {
		return studentRadService.mojiRadovi(pristupService.ulogovaniStudentId()).stream().map(this::odgovor).toList();
	}

	@GetMapping("/attempts/{attemptId}")
	public AttemptResponse rad(@PathVariable Long attemptId) {
		return odgovor(studentRadService.mojRad(pristupService.ulogovaniStudentId(), attemptId));
	}

	@GetMapping("/attempts/{attemptId}/task")
	public ResponseEntity<Resource> zadatak(@PathVariable Long attemptId) {
		return Fajlovi.zip(studentRadService.arhivaZadatka(pristupService.ulogovaniStudentId(), attemptId), "zadatak.zip");
	}

	// final=false: automatsko cuvanje tokom rada, rad ostaje U_IZRADI
	@PostMapping(path = "/attempts/{attemptId}/submission", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public AttemptResponse predaja(@PathVariable Long attemptId, @RequestParam("file") MultipartFile file,
								   @RequestParam(name = "final", defaultValue = "true") boolean konacno) {
		return odgovor(studentRadService.predaj(pristupService.ulogovaniStudentId(), attemptId, file, konacno));
	}

	// poslednje sacuvano resenje: nastavak rada na drugom racunaru, a posle termina pregled komentara
	@GetMapping("/attempts/{attemptId}/submission")
	public ResponseEntity<Resource> sacuvano(@PathVariable Long attemptId) {
		return Fajlovi.zip(studentRadService.sacuvanoResenje(pristupService.ulogovaniStudentId(), attemptId), "resenje.zip");
	}

	@GetMapping("/attempts/{attemptId}/feedback")
	public FeedbackResponse povratna(@PathVariable Long attemptId) {
		Long studentId = pristupService.ulogovaniStudentId();
		return Converters.toFeedbackResponse(studentRadService.mojRad(studentId, attemptId), studentRadService.komentari(studentId, attemptId));
	}

	private AttemptResponse odgovor(Rad r) {
		return Converters.toAttemptResponse(r, studentRadService.imaSacuvanoResenje(r));
	}
}
