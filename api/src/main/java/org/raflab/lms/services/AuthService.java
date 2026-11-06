package org.raflab.lms.services;

import lombok.AllArgsConstructor;
import org.raflab.lms.config.SecurityConfig;
import org.raflab.lms.config.SecurityProperties;
import org.raflab.lms.exceptions.UnauthorizedException;
import org.raflab.lms.model.Korisnik;
import org.raflab.lms.model.Uloga;
import org.raflab.lms.repositories.KorisnikRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/*
 * Prijava korisnika i izdavanje JWT tokena
 */
@AllArgsConstructor
@Service
public class AuthService {

    public static final String CLAIM_KORISNIK_ID = "korisnikId";
    public static final String CLAIM_STUDENT_ID = "studentId";
    public static final String CLAIM_NASTAVNIK_ID = "nastavnikId";

    final KorisnikRepository korisnikRepository;
    final PasswordEncoder passwordEncoder;
    final JwtEncoder jwtEncoder;
    final SecurityProperties securityProperties;

    public record Prijava(String token, Instant istice, Long korisnikId, Uloga uloga) {
    }

    @Transactional
    public void promeniLozinku(String email, String staraLozinka, String novaLozinka) {
        Korisnik korisnik = korisnikRepository.findByEmailIgnoreCase(email)
                .filter(k -> passwordEncoder.matches(staraLozinka, k.getLozinka()))
                .orElseThrow(() -> new UnauthorizedException("Pogresna trenutna lozinka"));
        korisnik.setLozinka(passwordEncoder.encode(novaLozinka));
    }

    @Transactional(readOnly = true)
    public Prijava login(String email, String lozinka) {
        // ista poruka za nepostojeci email i pogresnu lozinku, da se ne otkriva koji nalozi postoje
        Korisnik korisnik = korisnikRepository.findByEmailIgnoreCase(email)
                .filter(k -> passwordEncoder.matches(lozinka, k.getLozinka()))
                .orElseThrow(() -> new UnauthorizedException("Pogresan email ili lozinka"));

        Instant sada = Instant.now();
        Instant istice = sada.plus(securityProperties.jwtTrajanje());
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("raf-lms")
                .subject(korisnik.getEmail())
                .issuedAt(sada)
                .expiresAt(istice)
                .claim(SecurityConfig.CLAIM_ULOGA, korisnik.getUloga().name())
                .claim(CLAIM_KORISNIK_ID, korisnik.getId());
        if (korisnik.getStudent() != null) claims.claim(CLAIM_STUDENT_ID, korisnik.getStudent().getId());
        if (korisnik.getNastavnik() != null) claims.claim(CLAIM_NASTAVNIK_ID, korisnik.getNastavnik().getId());

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new Prijava(token, istice, korisnik.getId(), korisnik.getUloga());
    }
}
