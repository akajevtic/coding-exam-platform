package org.raflab.lms.controllers.request;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

// vreme stize kao ISO-8601 sa zonom, npr. 2027-01-20T09:00:00+01:00
public record TerminRequest(@NotNull Long subjectId, @NotNull Long examPeriodId,
                            @NotNull Instant startsAt, @NotNull Instant endsAt, String room) {
}
