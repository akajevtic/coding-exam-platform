package org.raflab.lms.model;

import jakarta.persistence.*;
import lombok.Data;

/*
 * Projekat koji grupa dobija. ZIP je na serverskom disku, u bazi je samo putanja.
 * Predmet, rok i termin se citaju preko grupe.
 */
@Entity
@Data
public class Zadatak {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;

	@OneToOne(optional = false)
	@JoinColumn(unique = true)   // jedna grupa ima jedan zadatak
	private Grupa grupa;

	@Column(nullable = false)
	private String naziv;

	@Column(length = 2000)
	private String opis;

	private String putanjaArhive;   // null dok nastavnik ne ucita ZIP
	private String nazivArhive;     // originalno ime fajla, za preuzimanje
}
