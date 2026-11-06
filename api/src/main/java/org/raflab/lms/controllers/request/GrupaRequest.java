package org.raflab.lms.controllers.request;

import jakarta.validation.constraints.NotBlank;

// pravi grupu u terminu i njen zadatak (polaganje); ZIP se salje posebno
public record GrupaRequest(@NotBlank String group, @NotBlank String title, String description) {
}
