package org.raflab.lms.repositories;

import java.util.Optional;

import org.raflab.lms.model.Korisnik;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KorisnikRepository extends JpaRepository<Korisnik, Long> {

	Optional<Korisnik> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);
}
