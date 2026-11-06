package org.raflab.lms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class RafLmsApp {

	public static void main(String[] args) {
		// .env je u api/; kad se aplikacija pokrene iz korena repozitorijuma (npr. IntelliJ), trazi se tamo
		if (!Files.exists(Path.of(".env")) && Files.exists(Path.of("api", ".env")) && System.getProperty("springdotenv.directory") == null)
			System.setProperty("springdotenv.directory", "api");
		SpringApplication.run(RafLmsApp.class, args);
	}

}
