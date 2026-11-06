package org.raflab.lms.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"naziv", "skolskaGodina"}))
public class IspitniRok {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String naziv;           // "Januarski", "Kolokvijumska nedelja 1"...

	@Column(nullable = false)
	private String skolskaGodina;   // "2026/2027"
}
