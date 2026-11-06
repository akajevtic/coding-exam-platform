package org.raflab.lms.model;

import jakarta.persistence.*;
import lombok.Data;

/*
 * Spisak prijavljenih: student je prijavio predmet u ispitnom roku.
 * Student sme da preuzme zadatak termina samo ako postoji prijava za predmet i rok tog termina.
 */
@Entity
@Data
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "predmet_id", "ispitni_rok_id"}))
public class Prijava {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "student_id")
	private Student student;

	@ManyToOne(optional = false)
	@JoinColumn(name = "predmet_id")
	private Predmet predmet;

	@ManyToOne(optional = false)
	@JoinColumn(name = "ispitni_rok_id")
	private IspitniRok ispitniRok;
}
