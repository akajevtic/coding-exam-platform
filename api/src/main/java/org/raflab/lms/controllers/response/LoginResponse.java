package org.raflab.lms.controllers.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.raflab.lms.model.Uloga;

import java.time.Instant;

@Data
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;   // salje se kao "Authorization: Bearer <token>"
    private Long userId;
    private Uloga role;
    private Instant expiresAt;
}
