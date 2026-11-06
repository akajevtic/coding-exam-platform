package org.raflab.lms.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.raflab.lms.controllers.request.IspitniRokRequest;
import org.raflab.lms.controllers.request.PredmetRequest;
import org.raflab.lms.controllers.response.IspitniRokResponse;
import org.raflab.lms.controllers.response.PredmetResponse;
import org.raflab.lms.services.PolaganjeService;
import org.raflab.lms.utils.Converters;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// predmeti i ispitni rokovi: citaju svi prijavljeni, unosi nastavnik (/teacher/**)
@RestController
@RequiredArgsConstructor
public class SifarnikController {

	final PolaganjeService polaganjeService;

	@GetMapping("/subjects")
	public List<PredmetResponse> predmeti() {
		return polaganjeService.sviPredmeti().stream().map(Converters::toPredmetResponse).toList();
	}

	@PostMapping("/teacher/subjects")
	@ResponseStatus(HttpStatus.CREATED)
	public PredmetResponse noviPredmet(@RequestBody @Valid PredmetRequest request) {
		return Converters.toPredmetResponse(polaganjeService.kreirajPredmet(request.code(), request.name()));
	}

	@GetMapping("/exam-periods")
	public List<IspitniRokResponse> rokovi() {
		return polaganjeService.sviRokovi().stream().map(Converters::toIspitniRokResponse).toList();
	}

	@PostMapping("/teacher/exam-periods")
	@ResponseStatus(HttpStatus.CREATED)
	public IspitniRokResponse noviRok(@RequestBody @Valid IspitniRokRequest request) {
		return Converters.toIspitniRokResponse(polaganjeService.kreirajRok(request.name(), request.schoolYear()));
	}
}
