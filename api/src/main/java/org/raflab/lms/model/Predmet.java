package org.raflab.lms.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Predmet {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String sifra;

	@Column(nullable = false)
	private String naziv;
}
