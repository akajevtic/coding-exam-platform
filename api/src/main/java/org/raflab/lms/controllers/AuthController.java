package org.raflab.lms.controllers;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.raflab.lms.controllers.request.LoginRequest;
import org.raflab.lms.controllers.request.PromenaLozinkeRequest;
import org.raflab.lms.controllers.response.LoginResponse;
import org.raflab.lms.services.AuthService;
import org.raflab.lms.services.AuthService.Prijava;
import org.raflab.lms.services.PristupService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping(path = "/auth")
public class AuthController {

	final AuthService authService;
	final PristupService pristupService;

	@PostMapping(path = "/login")
	public LoginResponse login(@RequestBody @Valid LoginRequest request) {
		Prijava p = authService.login(request.getUsername(), request.getPassword());
		return new LoginResponse(p.token(), p.korisnikId(), p.uloga(), p.istice());
	}

	// za prijavljenog korisnika; nalozi sa spiska prijavljenih dobijaju pocetnu lozinku koju ovde menjaju
	@PostMapping(path = "/change-password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void promeniLozinku(@RequestBody @Valid PromenaLozinkeRequest request) {
		authService.promeniLozinku(pristupService.ulogovaniEmail(), request.oldPassword(), request.newPassword());
	}
}
