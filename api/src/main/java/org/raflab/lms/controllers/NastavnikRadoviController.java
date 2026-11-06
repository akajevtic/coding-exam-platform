package org.raflab.lms.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.raflab.lms.controllers.request.OcenaRequest;
import org.raflab.lms.controllers.response.FeedbackResponse;
import org.raflab.lms.controllers.response.MonitoringResponse;
import org.raflab.lms.controllers.response.SubmissionResponse;
import org.raflab.lms.model.StatusRada;
import org.raflab.lms.services.OcenjivanjeService;
import org.raflab.lms.services.OcenjivanjeService.FajlRada;
import org.raflab.lms.services.OcenjivanjeService.Kriterijumi;
import org.raflab.lms.utils.Converters;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/*
 * Nastavnik: radovi studenata po kriterijumima selekcije, preuzimanje, ocenjivanje i monitoring.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/teacher")
public class NastavnikRadoviController {

	final OcenjivanjeService ocenjivanjeService;

	// svi kriterijumi su opcioni: predmet, rok, termin, grupa, ucionica, student (indeks), status
	@GetMapping("/submissions")
	public List<SubmissionResponse> radovi(@RequestParam(required = false) Long subjectId, @RequestParam(required = false) Long examPeriodId,
										   @RequestParam(required = false) Long termId, @RequestParam(required = false) Long groupId,
										   @RequestParam(required = false) String room, @RequestParam(required = false) String studentNumber,
										   @RequestParam(required = false) StatusRada status) {
		return ocenjivanjeService.pretraga(new Kriterijumi(subjectId, examPeriodId, termId, groupId, room, studentNumber, status))
				.stream().map(Converters::toSubmissionResponse).toList();
	}

	// isti kriterijumi kao /submissions; vraca jedan ZIP sa ZIP-om svakog rada (predmet/grupa/indeks.zip)
	@GetMapping("/submissions/download")
	public ResponseEntity<StreamingResponseBody> preuzmiSve(@RequestParam(required = false) Long subjectId, @RequestParam(required = false) Long examPeriodId,
															@RequestParam(required = false) Long termId, @RequestParam(required = false) Long groupId,
															@RequestParam(required = false) String room, @RequestParam(required = false) String studentNumber,
															@RequestParam(required = false) StatusRada status) {
		List<FajlRada> fajlovi = ocenjivanjeService.fajlovi(new Kriterijumi(subjectId, examPeriodId, termId, groupId, room, studentNumber, status));
		StreamingResponseBody telo = out -> {
			try (ZipOutputStream zip = new ZipOutputStream(out)) {
				for (FajlRada f : fajlovi) {
					if (!Files.isRegularFile(f.putanja())) continue;
					zip.putNextEntry(new ZipEntry(f.ime()));
					Files.copy(f.putanja(), zip);
					zip.closeEntry();
				}
			}
		};
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("application/zip"))
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("radovi.zip").build().toString())
				.body(telo);
	}

	@GetMapping("/submissions/{submissionId}")
	public SubmissionResponse rad(@PathVariable Long submissionId) {
		return Converters.toSubmissionResponse(ocenjivanjeService.rad(submissionId));
	}

	@GetMapping("/submissions/{submissionId}/file")
	public ResponseEntity<Resource> fajl(@PathVariable Long submissionId) {
		return Fajlovi.zip(ocenjivanjeService.fajl(submissionId), "resenje-" + submissionId + ".zip");
	}

	@GetMapping("/submissions/{submissionId}/grade")
	public FeedbackResponse ocena(@PathVariable Long submissionId) {
		return Converters.toFeedbackResponse(ocenjivanjeService.rad(submissionId), ocenjivanjeService.komentari(submissionId));
	}

	@PutMapping("/submissions/{submissionId}/grade")
	public FeedbackResponse oceni(@PathVariable Long submissionId, @RequestBody @Valid OcenaRequest request) {
		return Converters.toFeedbackResponse(ocenjivanjeService.oceni(submissionId, request), ocenjivanjeService.komentari(submissionId));
	}

	// broj preuzetih i predatih radova, prosecno vreme izrade i status svakog studenta u terminu
	@GetMapping("/terms/{termId}/monitoring")
	public MonitoringResponse monitoring(@PathVariable Long termId) {
		return Converters.toMonitoringResponse(ocenjivanjeService.monitoring(termId));
	}
}
