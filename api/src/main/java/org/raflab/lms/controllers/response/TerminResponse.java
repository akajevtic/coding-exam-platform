package org.raflab.lms.controllers.response;

import java.time.Instant;
import java.util.List;

public record TerminResponse(Long id, PredmetResponse subject, IspitniRokResponse examPeriod,
                             Instant startsAt, Instant endsAt, String room, List<ExamResponse> exams) {
}
