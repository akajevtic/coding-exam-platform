package org.raflab.lms.controllers.request;

import jakarta.validation.constraints.NotBlank;

public record IspitniRokRequest(@NotBlank String name, @NotBlank String schoolYear) {
}
