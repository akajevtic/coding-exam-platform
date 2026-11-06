package org.raflab.lms.controllers.response;

import java.math.BigDecimal;
import java.util.List;

public record FeedbackResponse(Long attemptId, BigDecimal score, List<KomentarResponse> comments) {
}
