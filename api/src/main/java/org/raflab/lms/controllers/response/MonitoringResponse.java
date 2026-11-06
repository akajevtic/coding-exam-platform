package org.raflab.lms.controllers.response;

import java.time.Instant;
import java.util.List;

public record MonitoringResponse(Long termId, Instant serverTime, int registeredCount, int downloadedCount, int submittedCount,
                                 Double averageDurationSeconds, List<Student> students) {

    // status NIJE_ZAPOCETO: na spisku je, a jos nije preuzeo zadatak
    public record Student(String studentNumber, String firstName, String lastName, String group, String status,
                          Instant downloadedAt, Instant submittedAt, Long durationSeconds) {
    }
}
