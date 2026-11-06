package org.raflab.lms.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.raflab.lms.controllers.request.GrupaRequest;
import org.raflab.lms.controllers.request.SpisakRequest;
import org.raflab.lms.controllers.request.TerminRequest;
import org.raflab.lms.controllers.response.ExamResponse;
import org.raflab.lms.controllers.response.SpisakResponse;
import org.raflab.lms.controllers.response.TerminResponse;
import org.raflab.lms.model.Zadatak;
import org.raflab.lms.services.PolaganjeService;
import org.raflab.lms.services.PrijavaService;
import org.raflab.lms.utils.Converters;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/*
 * Nastavnicki tok pripreme: termin -> grupa sa zadatkom (polaganje) -> ZIP zadatka, i spisak prijavljenih.
 * Uloga NASTAVNIK se proverava u SecurityConfig za /teacher/**.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/teacher")
public class NastavnikPolaganjeController {

	final PolaganjeService polaganjeService;
	final PrijavaService prijavaService;

	@PostMapping("/terms")
	@ResponseStatus(HttpStatus.CREATED)
	public TerminResponse noviTermin(@RequestBody @Valid TerminRequest request) {
		return Converters.toTerminResponse(polaganjeService.kreirajTermin(request), List.of());
	}

	// termini sa grupama i zadacima, najnoviji prvi
	@GetMapping("/terms")
	public List<TerminResponse> termini() {
		Map<Long, List<Zadatak>> zadaci = polaganjeService.zadaciPoTerminu();
		return polaganjeService.sviTermini().stream()
				.map(t -> Converters.toTerminResponse(t, zadaci.getOrDefault(t.getId(), List.of())))
				.toList();
	}

	@PostMapping("/terms/{termId}/groups")
	@ResponseStatus(HttpStatus.CREATED)
	public ExamResponse novaGrupa(@PathVariable Long termId, @RequestBody @Valid GrupaRequest request) {
		return Converters.toExamResponse(polaganjeService.kreirajGrupu(termId, request));
	}

	@PutMapping(path = "/terms/{termId}/roster")
	public SpisakResponse spisak(@PathVariable Long termId, @RequestBody @Valid SpisakRequest request) {
		return prijavaService.postaviSpisak(termId, request.students());
	}

	@PutMapping(path = "/exams/{examId}/task", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void zadatak(@PathVariable Long examId, @RequestParam("file") MultipartFile file) {
		polaganjeService.postaviArhivu(examId, file);
	}
}
