package org.raflab.lms.controllers.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// nazivi polja prate docs/api-contract.yaml; username je email naloga
@Data
public class LoginRequest {

    @NotBlank
    private String username;
    @NotBlank
    private String password;
}
