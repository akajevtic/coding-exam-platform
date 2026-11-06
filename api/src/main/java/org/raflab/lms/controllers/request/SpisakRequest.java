package org.raflab.lms.controllers.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SpisakRequest(@NotEmpty List<@Valid StudentIdentity> students) {
}
