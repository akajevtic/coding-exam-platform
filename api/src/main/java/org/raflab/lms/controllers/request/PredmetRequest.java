package org.raflab.lms.controllers.request;

import jakarta.validation.constraints.NotBlank;

public record PredmetRequest(@NotBlank String code, @NotBlank String name) {
}
