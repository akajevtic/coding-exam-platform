package org.raflab.lms.controllers.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

// komentari zamenjuju prethodne komentare na radu
public record OcenaRequest(@NotNull @DecimalMin("0") BigDecimal score, @NotNull List<@Valid KomentarRequest> comments) {
}
