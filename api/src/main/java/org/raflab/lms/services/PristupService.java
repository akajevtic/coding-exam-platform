package org.raflab.lms.services;

import org.raflab.lms.exceptions.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/*
 * Podaci o prijavljenom korisniku iz JWT tokena.
 */
@Service
public class PristupService {

    // Student id iz tokena; ruta /student/** je vec ogranicena na ulogu STUDENT
    public Long ulogovaniStudentId() {
        Object id = ulogovaniJwt().getClaim(AuthService.CLAIM_STUDENT_ID);
        if (id instanceof Number n) return n.longValue();
        throw new UnauthorizedException("Nalog nije vezan za studenta");
    }

    // email naloga (subject tokena)
    public String ulogovaniEmail() {
        return ulogovaniJwt().getSubject();
    }

    private Jwt ulogovaniJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) return jwt;
        throw new UnauthorizedException("Korisnik nije prijavljen");
    }
}
