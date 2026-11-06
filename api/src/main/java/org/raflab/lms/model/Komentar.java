package org.raflab.lms.model;

import jakarta.persistence.*;
import lombok.Data;

// komentar nastavnika uz liniju fajla iz predatog rada
@Entity
@Data
public class Komentar {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	private Rad rad;

	@Column(nullable = false)
	private String putanjaFajla;   // relativno u projektu, npr. "src/Main.java"

	private int linija;            // od 1

	@Column(nullable = false, length = 2000)
	private String tekst;
}
