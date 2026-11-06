package org.raflab.lms.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

/*
 * Nalog za prijavu na sistem. Student je vezan za Student, nastavnik za Nastavnik.
 */
@Entity
@Data
@ToString(exclude = "lozinka")
public class Korisnik {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false)
	private String lozinka;   // hesirana, nikad u cistom tekstu

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Uloga uloga;

	@OneToOne
	private Student student;         // samo za ulogu STUDENT

	@OneToOne
	private Nastavnik nastavnik;     // samo za ulogu NASTAVNIK
}
