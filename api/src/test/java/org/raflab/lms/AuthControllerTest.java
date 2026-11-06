package org.raflab.lms;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

// cela aplikacija nad H2: Seeder, prijava i pravila po ulozi
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

	@Autowired
	MockMvc mvc;

	@Test
	void seedovaniNastavnikSePrijavljuje() throws Exception {
		mvc.perform(login("nastavnik@raf.rs", "lozinka123"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("NASTAVNIK"))
				.andExpect(jsonPath("$.accessToken").isNotEmpty());
	}

	@Test
	void pogresnaLozinkaVraca401() throws Exception {
		mvc.perform(login("ana@raf.rs", "pogresna")).andExpect(status().isUnauthorized());
	}

	@Test
	void bezTokenaVraca401() throws Exception {
		mvc.perform(get("/teacher/exams")).andExpect(status().isUnauthorized());
	}

	@Test
	void studentNemaPristupNastavnickimRutama() throws Exception {
		String odgovor = mvc.perform(login("ana@raf.rs", "lozinka123")).andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(odgovor, "$.accessToken");
		mvc.perform(get("/teacher/exams").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());
	}

	private static RequestBuilder login(String email, String lozinka) {
		return post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"" + email + "\",\"password\":\"" + lozinka + "\"}");
	}
}
