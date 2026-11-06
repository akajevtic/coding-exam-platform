package org.raflab.lms.controllers;

import org.raflab.lms.exceptions.ResourceNotFoundException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

// odgovor sa ZIP fajlom sa diska, za rute koje preuzimaju zadatak ili resenje
class Fajlovi {

	static ResponseEntity<Resource> zip(Path putanja, String ime) {
		if (!Files.isRegularFile(putanja)) throw new ResourceNotFoundException("Fajl ne postoji na serveru");
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("application/zip"))
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(ime, StandardCharsets.UTF_8).build().toString())
				.body(new FileSystemResource(putanja));
	}
}
