package org.raflab.lms.controllers.response;

// polaganje = zadatak jedne grupe u terminu; id je id zadatka
public record ExamResponse(Long id, Long termId, Long groupId, String group, String title,
                           String description, boolean hasTask) {
}
