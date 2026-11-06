package org.raflab.lms.controllers.response;

import org.raflab.lms.model.StatusRada;

import java.time.Instant;

// rad studenta; serverTime sluzi klijentu da uskladi tajmer sa serverskim satom
public record AttemptResponse(Long id, Long examId, Long termId, String subject, String group, String title,
                              StatusRada status, Instant downloadedAt, Instant submittedAt,
                              Instant startsAt, Instant endsAt, Instant serverTime, boolean hasSavedWork) {
}
