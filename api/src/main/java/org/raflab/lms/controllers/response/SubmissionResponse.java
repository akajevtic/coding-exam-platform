package org.raflab.lms.controllers.response;

import org.raflab.lms.model.StatusRada;

import java.math.BigDecimal;
import java.time.Instant;

// rad studenta iz ugla nastavnika
public record SubmissionResponse(Long id, Long examId, Long termId, String subject, String examPeriod, String group, String room,
                                 String studentNumber, String firstName, String lastName, StatusRada status,
                                 Instant downloadedAt, Instant submittedAt, Long durationSeconds, BigDecimal score, boolean hasFile) {
}
