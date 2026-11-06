package org.raflab.lms.controllers.response;

public record KomentarResponse(Long id, String filePath, int line, String text) {
}
