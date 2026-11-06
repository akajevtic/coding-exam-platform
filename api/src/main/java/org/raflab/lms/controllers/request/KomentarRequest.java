package org.raflab.lms.controllers.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record KomentarRequest(@NotBlank String filePath, @Min(1) int line, @NotBlank String text) {
}
