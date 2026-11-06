package org.raflab.lms.controllers.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PromenaLozinkeRequest(@NotBlank String oldPassword, @NotBlank @Size(min = 8) String newPassword) {
}
