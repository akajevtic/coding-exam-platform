package org.raflab.lms.controllers.response;

// podaci prijavljenog studenta za automatsko popunjavanje forme prijave na polaganje
public record StudentResponse(Long id, String studentNumber, String firstName, String lastName, String email) {
}
