package org.raflab.lms.controllers.response;

// registered: koliko je studenata na spisku, newStudents: koliko ih je tek upisano u bazu
public record SpisakResponse(int registered, int newStudents) {
}
