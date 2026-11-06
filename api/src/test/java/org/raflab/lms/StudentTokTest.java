package org.raflab.lms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.raflab.lms.model.*;
import org.raflab.lms.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

/*
 * Ceo tok nad H2: nastavnik postavi termin, ZIP i spisak -> student se prijavi, preuzme, sacuva i preda ->
 * nastavnik pretrazi, preuzme i oceni -> student vidi ocenu; plus pravila spiska, vremena i vlasnistva.
 */
@SpringBootTest
@AutoConfigureMockMvc
class StudentTokTest {

	@Autowired MockMvc mvc;
	@Autowired PredmetRepository predmetRepository;
	@Autowired IspitniRokRepository ispitniRokRepository;
	@Autowired TerminRepository terminRepository;
	@Autowired GrupaRepository grupaRepository;
	@Autowired ZadatakRepository zadatakRepository;
	@Autowired RadRepository radRepository;
	@Autowired StudentRepository studentRepository;

	String nastavnik;
	Termin termin;
	String indeks;     // novi student sa spiska, dobija nalog sa pocetnom lozinkom
	String email;
	byte[] zadatakZip;

	@BeforeEach
	void pripremi() throws Exception {
		nastavnik = token("nastavnik@raf.rs", "lozinka123");
		termin = termin(Instant.now().minus(Duration.ofHours(1)), Instant.now().plus(Duration.ofHours(1)));
		long examId = grupa(termin, "Grupa 1");
		zadatakZip = zip("src/Main.java", "class Main { // TODO }");
		mvc.perform(kao(nastavnik, multipart(HttpMethod.PUT, "/teacher/exams/" + examId + "/task")
				.file(new MockMultipartFile("file", "kol.zip", "application/zip", zadatakZip))))
				.andExpect(status().isNoContent());

		long n = System.nanoTime() % 100000;
		indeks = "RN " + n + "/2025";
		email = "s" + n + "@raf.rs";
		mvc.perform(kao(nastavnik, put("/teacher/terms/" + termin.getId() + "/roster")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"students\":[{\"studentNumber\":\"" + indeks + "\",\"firstName\":\"Iva\",\"lastName\":\"Ivic\",\"email\":\"" + email + "\"}]}"))
				.andExpect(status().isOk());
	}

	@Test
	void punTokOdPrijaveDoOcene() throws Exception {
		String student = token(email, "student123");

		mvc.perform(kao(student, get("/student/me"))).andExpect(jsonPath("$.studentNumber").value(indeks));
		mvc.perform(kao(student, get("/student/terms")))
				.andExpect(jsonPath("$[?(@.id == " + termin.getId() + ")].exams[0].group").value("Grupa 1"));

		String prijava = "{\"termId\":" + termin.getId() + ",\"group\":\"grupa 1\",\"studentNumber\":\"" + indeks.toLowerCase() + "\"}";
		String rad = mvc.perform(kao(student, post("/student/attempts")).contentType(MediaType.APPLICATION_JSON).content(prijava))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("U_IZRADI"))
				.andReturn().getResponse().getContentAsString();
		long radId = id(rad);
		// ponovna prijava (drugi racunar) vraca isti rad
		mvc.perform(kao(student, post("/student/attempts")).contentType(MediaType.APPLICATION_JSON).content(prijava))
				.andExpect(jsonPath("$.id").value(radId));

		mvc.perform(kao(student, get("/student/attempts/" + radId + "/task")))
				.andExpect(status().isOk()).andExpect(content().bytes(zadatakZip));

		byte[] nacrt = zip("src/Main.java", "class Main { int x; }");
		mvc.perform(kao(student, multipart("/student/attempts/" + radId + "/submission").param("final", "false")
				.file(new MockMultipartFile("file", "r.zip", "application/zip", nacrt))))
				.andExpect(jsonPath("$.status").value("U_IZRADI"))
				.andExpect(jsonPath("$.hasSavedWork").value(true));
		mvc.perform(kao(student, get("/student/attempts/" + radId + "/submission"))).andExpect(content().bytes(nacrt));

		byte[] resenje = zip("src/Main.java", "class Main { int x = 1; }");
		mvc.perform(kao(student, multipart("/student/attempts/" + radId + "/submission")
				.file(new MockMultipartFile("file", "r.zip", "application/zip", resenje))))
				.andExpect(jsonPath("$.status").value("PREDATO"))
				.andExpect(jsonPath("$.submittedAt").isNotEmpty());

		// nastavnik: pretraga po kriterijumima, preuzimanje, ocena
		mvc.perform(kao(nastavnik, get("/teacher/submissions").param("termId", "" + termin.getId()).param("studentNumber", indeks)))
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].status").value("PREDATO"))
				.andExpect(jsonPath("$[0].room").value("RAF2"));
		mvc.perform(kao(nastavnik, get("/teacher/submissions").param("termId", "" + termin.getId()).param("status", "U_IZRADI")))
				.andExpect(jsonPath("$.length()").value(0));
		mvc.perform(kao(nastavnik, get("/teacher/submissions/" + radId + "/file"))).andExpect(content().bytes(resenje));

		// StreamingResponseBody se u MockMvc izvrsava asinhrono
		MvcResult preuzimanje = mvc.perform(kao(nastavnik, get("/teacher/submissions/download").param("termId", "" + termin.getId())))
				.andExpect(request().asyncStarted()).andReturn();
		byte[] sve = mvc.perform(asyncDispatch(preuzimanje)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
		assertThat(imenaUZipu(sve)).containsExactly(termin.getPredmet().getSifra() + "/Grupa_1/" + indeks.replace(' ', '_').replace('/', '-') + ".zip");

		mvc.perform(kao(nastavnik, put("/teacher/submissions/" + radId + "/grade")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"score\":8.5,\"comments\":[{\"filePath\":\"src/Main.java\",\"line\":1,\"text\":\"Dobro\"}]}"))
				.andExpect(status().isOk());

		mvc.perform(kao(student, get("/student/attempts/" + radId + "/feedback")))
				.andExpect(jsonPath("$.score").value(8.5))
				.andExpect(jsonPath("$.comments[0].line").value(1))
				.andExpect(jsonPath("$.comments[0].text").value("Dobro"));

		mvc.perform(kao(nastavnik, get("/teacher/terms/" + termin.getId() + "/monitoring")))
				.andExpect(jsonPath("$.registeredCount").value(1))
				.andExpect(jsonPath("$.downloadedCount").value(1))
				.andExpect(jsonPath("$.submittedCount").value(1))
				.andExpect(jsonPath("$.averageDurationSeconds").isNumber())
				.andExpect(jsonPath("$.students[0].status").value("PREDATO"));
	}

	@Test
	void monitoringPrikazujeKoJosNijePoceo() throws Exception {
		mvc.perform(kao(nastavnik, get("/teacher/terms/" + termin.getId() + "/monitoring")))
				.andExpect(jsonPath("$.downloadedCount").value(0))
				.andExpect(jsonPath("$.students[0].status").value("NIJE_ZAPOCETO"));
	}

	@Test
	void studentKojiNijeNaSpiskuDobija403() throws Exception {
		mvc.perform(kao(token("petar@raf.rs", "lozinka123"), post("/student/attempts")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"termId\":" + termin.getId() + ",\"group\":\"Grupa 1\",\"studentNumber\":\"RN 3/2023\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.detail").value("Niste na spisku prijavljenih za ovo polaganje"));
	}

	@Test
	void tudjiIndeksDobija403() throws Exception {
		mvc.perform(kao(token(email, "student123"), post("/student/attempts")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"termId\":" + termin.getId() + ",\"group\":\"Grupa 1\",\"studentNumber\":\"RN 1/2023\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void zavrsenTerminNeDozvoljavaPreuzimanjeNiPredaju() throws Exception {
		Termin zavrsen = termin(Instant.now().minus(Duration.ofHours(3)), Instant.now().minus(Duration.ofHours(1)));
		long examId = grupa(zavrsen, "A");
		Student ana = studentRepository.findByIndeksIgnoreCase("RN 1/2023").orElseThrow();
		// termin je za novi predmet i rok, pa anu prvo stavljamo na spisak
		mvc.perform(kao(nastavnik, put("/teacher/terms/" + zavrsen.getId() + "/roster")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"students\":[{\"studentNumber\":\"RN 1/2023\",\"firstName\":\"Ana\",\"lastName\":\"Anic\",\"email\":\"ana@raf.rs\"}]}"))
				.andExpect(status().isOk());
		String anaToken = token("ana@raf.rs", "lozinka123");

		mvc.perform(kao(anaToken, post("/student/attempts")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"termId\":" + zavrsen.getId() + ",\"group\":\"A\",\"studentNumber\":\"RN 1/2023\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.detail").value("Polaganje je zavrseno"));

		// rad iz vremena dok je termin trajao: predaja posle kraja se odbija
		Rad rad = new Rad();
		rad.setStudent(ana);
		rad.setZadatak(zadatakRepository.findById(examId).orElseThrow());
		rad.setStatus(StatusRada.U_IZRADI);
		rad.setVremePreuzimanja(zavrsen.getPocetak());
		radRepository.save(rad);
		mvc.perform(kao(anaToken, multipart("/student/attempts/" + rad.getId() + "/submission")
				.file(new MockMultipartFile("file", "r.zip", "application/zip", zip("a", "b")))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.detail").value("Rok za predaju je istekao"));

		// drugi student ne vidi tudji rad
		mvc.perform(kao(token("marko@raf.rs", "lozinka123"), get("/student/attempts/" + rad.getId())))
				.andExpect(status().isNotFound());
	}

	@Test
	void studentMenjaPocetnuLozinku() throws Exception {
		mvc.perform(kao(token(email, "student123"), post("/auth/change-password")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"oldPassword\":\"student123\",\"newPassword\":\"novaLozinka1\"}"))
				.andExpect(status().isNoContent());
		mvc.perform(login(email, "student123")).andExpect(status().isUnauthorized());
		mvc.perform(login(email, "novaLozinka1")).andExpect(status().isOk());
	}

	private Termin termin(Instant pocetak, Instant kraj) {
		Predmet p = new Predmet();
		p.setSifra("T" + System.nanoTime());
		p.setNaziv("Test");
		predmetRepository.save(p);
		IspitniRok r = new IspitniRok();
		r.setNaziv("R" + System.nanoTime());
		r.setSkolskaGodina("2026/2027");
		ispitniRokRepository.save(r);
		Termin t = new Termin();
		t.setPredmet(p);
		t.setIspitniRok(r);
		t.setPocetak(pocetak);
		t.setKraj(kraj);
		t.setUcionica("RAF2");
		return terminRepository.save(t);
	}

	private long grupa(Termin t, String naziv) throws Exception {
		return id(mvc.perform(kao(nastavnik, post("/teacher/terms/" + t.getId() + "/groups")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"group\":\"" + naziv + "\",\"title\":\"Zadatak\"}")).andReturn().getResponse().getContentAsString());
	}

	private String token(String email, String lozinka) throws Exception {
		String odgovor = mvc.perform(login(email, lozinka)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(odgovor, "$.accessToken");
	}

	private static AbstractMockHttpServletRequestBuilder<?> login(String email, String lozinka) {
		return post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"" + email + "\",\"password\":\"" + lozinka + "\"}");
	}

	private static <T extends AbstractMockHttpServletRequestBuilder<?>> T kao(String token, T zahtev) {
		zahtev.header("Authorization", "Bearer " + token);
		return zahtev;
	}

	private static long id(String json) {
		return ((Number) JsonPath.read(json, "$.id")).longValue();
	}

	private static byte[] zip(String ime, String sadrzaj) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(out)) {
			zos.putNextEntry(new ZipEntry(ime));
			zos.write(sadrzaj.getBytes());
		}
		return out.toByteArray();
	}

	private static List<String> imenaUZipu(byte[] zip) throws Exception {
		List<String> imena = new ArrayList<>();
		try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
			for (ZipEntry e; (e = in.getNextEntry()) != null; ) imena.add(e.getName());
		}
		return imena;
	}
}
