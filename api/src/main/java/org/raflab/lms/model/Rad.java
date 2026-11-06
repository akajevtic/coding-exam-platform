package org.raflab.lms.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/*
 * Rad studenta na zadatku: nastaje pri preuzimanju, dopunjuje se pri predaji i ocenjivanju.
 * Jedan rad po studentu i zadatku; nova predaja zamenjuje fajl prethodne.
 */
@Entity
@Data
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "zadatak_id"}))
public class Rad {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "student_id")
	private Student student;

	@ManyToOne(optional = false)
	@JoinColumn(name = "zadatak_id")
	private Zadatak zadatak;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private StatusRada status;

	@Column(nullable = false)
	private Instant vremePreuzimanja;

	private Instant vremePredaje;     // null dok je U_IZRADI

	private String putanjaResenja;    // ZIP predaje na serverskom disku

	@Column(precision = 5, scale = 2)
	private BigDecimal poeni;         // null dok nastavnik ne oceni
}
