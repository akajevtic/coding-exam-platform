package org.raflab.lms.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"termin_id", "naziv"}))
public class Grupa {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "termin_id")
	private Termin termin;

	@Column(nullable = false)
	private String naziv;   // "Grupa 1", "A"...
}
