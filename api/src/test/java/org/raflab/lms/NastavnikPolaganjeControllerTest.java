package org.raflab.lms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.raflab.lms.model.Termin;
import org.raflab.lms.repositories.PrijavaRepository;
import org.raflab.lms.repositories.StudentRepository;
import org.raflab.lms.repositories.TerminRepository;
import org.raflab.lms.services.SkladisteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;

// nastavnicki tok nad H2: termin, grupa, ZIP zadatka i spisak prijavljenih
@SpringBootTest
@AutoConfigureMockMvc
class NastavnikPolaganjeControllerTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	TerminRepository terminRepository;

	@Autowired
	StudentRepository studentRepository;

	@Autowired
	PrijavaRepository prijavaRepository;

	@Autowired
	SkladisteService skladisteService;

	String token;

	@BeforeEach
	void prijava() throws Exception {
		String odgovor = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"nastavnik@raf.rs\",\"password\":\"lozinka123\"}"))
				.andReturn().getResponse().getContentAsString();
		token = JsonPath.read(odgovor, "$.accessToken");
	}

	@Test
	void terminGrupaIZipZadatka() throws Exception {
		long termin = noviTermin("2027-01-20T09:00:00+01:00", "2027-01-20T12:00:00+01:00");

		String exam = mvc.perform(kao(post("/teacher/terms/" + termin + "/groups"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"group\":\"Grupa 1\",\"title\":\"Kolokvijum 1\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.hasTask").value(false))
				.andReturn().getResponse().getContentAsString();
		long examId = ((Number) JsonPath.read(exam, "$.id")).longValue();

		byte[] zip = zip("src/Main.java", "class Main {}");
		mvc.perform(kao(multipart(HttpMethod.PUT, "/teacher/exams/" + examId + "/task")
				.file(new MockMultipartFile("file", "C:\\Users\\nastavnik\\kol1.zip", "application/zip", zip))))
				.andExpect(status().isNoContent());

		assertThat(Files.readAllBytes(skladisteService.putanja("zadaci/" + examId + ".zip"))).isEqualTo(zip);
		mvc.perform(kao(get("/teacher/terms")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + termin + ")].startsAt").value("2027-01-20T08:00:00Z"))
				.andExpect(jsonPath("$[?(@.id == " + termin + ")].exams[0].hasTask").value(true));
	}

	@Test
	void istaGrupaDvaPutaUTerminuVraca409() throws Exception {
		long termin = noviTermin("2027-01-21T09:00:00Z", "2027-01-21T12:00:00Z");
		String grupa = "{\"group\":\"A\",\"title\":\"Zadatak\"}";
		mvc.perform(kao(post("/teacher/terms/" + termin + "/groups")).contentType(MediaType.APPLICATION_JSON).content(grupa))
				.andExpect(status().isCreated());
		mvc.perform(kao(post("/teacher/terms/" + termin + "/groups")).contentType(MediaType.APPLICATION_JSON).content(grupa))
				.andExpect(status().isConflict());
	}

	@Test
	void fajlKojiNijeZipVraca400() throws Exception {
		mvc.perform(kao(multipart(HttpMethod.PUT, "/teacher/exams/1/task")
				.file(new MockMultipartFile("file", "zadatak.zip", "application/zip", "nije zip".getBytes()))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void krajPrePocetkaVraca400() throws Exception {
		mvc.perform(kao(post("/teacher/terms")).contentType(MediaType.APPLICATION_JSON)
				.content(termin("2027-01-20T12:00:00Z", "2027-01-20T09:00:00Z")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void nepostojeciPredmetVraca404() throws Exception {
		mvc.perform(kao(post("/teacher/terms")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"subjectId\":999,\"examPeriodId\":1,\"startsAt\":\"2027-01-20T09:00:00Z\",\"endsAt\":\"2027-01-20T12:00:00Z\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void noviSpisakZamenjujePrethodni() throws Exception {
		long terminId = noviTermin("2027-02-01T09:00:00Z", "2027-02-01T12:00:00Z");
		Termin t = terminRepository.findById(terminId).orElseThrow();

		// postojeci student iz Seeder-a (indeks drugacije napisan) + novi student + duplikat
		mvc.perform(kao(put("/teacher/terms/" + terminId + "/roster")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"students\":["
						+ "{\"studentNumber\":\"rn  1/2023\",\"firstName\":\"Ana\",\"lastName\":\"Anic\",\"email\":\"ana@raf.rs\"},"
						+ "{\"studentNumber\":\"RN 99/2024\",\"firstName\":\"Nova\",\"lastName\":\"Novic\",\"email\":\"Nova@raf.rs\"},"
						+ "{\"studentNumber\":\"RN 99/2024\",\"firstName\":\"Nova\",\"lastName\":\"Novic\",\"email\":\"nova@raf.rs\"}]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.registered").value(2))
				.andExpect(jsonPath("$.newStudents").value(1));

		Long ana = studentRepository.findByIndeksIgnoreCase("RN 1/2023").orElseThrow().getId();
		Long nova = studentRepository.findByIndeksIgnoreCase("RN 99/2024").orElseThrow().getId();
		assertThat(naSpisku(ana, t)).isTrue();
		assertThat(naSpisku(nova, t)).isTrue();

		mvc.perform(kao(put("/teacher/terms/" + terminId + "/roster")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"students\":[{\"studentNumber\":\"RN 99/2024\",\"firstName\":\"Nova\",\"lastName\":\"Novic\",\"email\":\"nova@raf.rs\"}]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.newStudents").value(0));
		assertThat(naSpisku(ana, t)).isFalse();
		assertThat(naSpisku(nova, t)).isTrue();
	}

	@Test
	void prazanSpisakVraca400() throws Exception {
		mvc.perform(kao(put("/teacher/terms/1/roster")).contentType(MediaType.APPLICATION_JSON).content("{\"students\":[]}"))
				.andExpect(status().isBadRequest());
	}

	// novi predmet i rok, da testovi ne dele spisak sa Seeder-om ili drugim testovima
	private long noviTermin(String pocetak, String kraj) throws Exception {
		String sifra = "P" + System.nanoTime();
		long predmet = id(mvc.perform(kao(post("/teacher/subjects")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"" + sifra + "\",\"name\":\"Test\"}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
		long rok = id(mvc.perform(kao(post("/teacher/exam-periods")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"" + sifra + "\",\"schoolYear\":\"2026/2027\"}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
		return id(mvc.perform(kao(post("/teacher/terms")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"subjectId\":" + predmet + ",\"examPeriodId\":" + rok + ",\"startsAt\":\"" + pocetak
						+ "\",\"endsAt\":\"" + kraj + "\",\"room\":\"RAF1\"}"))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
	}

	private boolean naSpisku(Long studentId, Termin t) {
		return prijavaRepository.existsByStudentIdAndPredmetIdAndIspitniRokId(studentId, t.getPredmet().getId(), t.getIspitniRok().getId());
	}

	private static String termin(String pocetak, String kraj) {
		return "{\"subjectId\":1,\"examPeriodId\":1,\"startsAt\":\"" + pocetak + "\",\"endsAt\":\"" + kraj + "\"}";
	}

	private <T extends AbstractMockHttpServletRequestBuilder<?>> T kao(T zahtev) {
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
}
