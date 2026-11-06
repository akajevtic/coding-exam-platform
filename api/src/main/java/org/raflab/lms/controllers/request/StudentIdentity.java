package org.raflab.lms.controllers.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record StudentIdentity(@NotBlank String studentNumber, @NotBlank String firstName,
                              @NotBlank String lastName, @NotBlank @Email String email) {
}
