package org.raflab.lms.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/*
 * Jedno polaganje predmeta u ispitnom roku: vreme i ucionica.
 * Vremenski prozor [pocetak, kraj] se proverava na serverskom satu.
 */
@Entity
@Data
public class Termin {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	private Predmet predmet;

	@ManyToOne(optional = false)
	private IspitniRok ispitniRok;

	@Column(nullable = false)
	private Instant pocetak;

	@Column(nullable = false)
	private Instant kraj;

	private String ucionica;   // kriterijum za preuzimanje resenja
}
