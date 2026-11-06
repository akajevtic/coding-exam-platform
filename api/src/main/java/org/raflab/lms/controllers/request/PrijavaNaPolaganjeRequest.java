package org.raflab.lms.controllers.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// student unosi termin, grupu i svoj indeks; server proverava spisak i vreme
public record PrijavaNaPolaganjeRequest(@NotNull Long termId, @NotBlank String group, @NotBlank String studentNumber) {
}
