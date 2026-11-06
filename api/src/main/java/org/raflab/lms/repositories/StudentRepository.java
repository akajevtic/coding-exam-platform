package org.raflab.lms.repositories;

import java.util.Optional;

import org.raflab.lms.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {

	Optional<Student> findByIndeksIgnoreCase(String indeks);
}
