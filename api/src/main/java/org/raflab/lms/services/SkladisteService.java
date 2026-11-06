package org.raflab.lms.services;

import org.raflab.lms.exceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

/*
 * ZIP fajlovi zadataka i predaja na serverskom disku (app.storage.dir), van javnog direktorijuma.
 * U bazi se cuva samo relativna putanja; ime fajla nikad ne dolazi od klijenta.
 */
@Service
public class SkladisteService {

    private static final byte[] ZIP_ZAGLAVLJE = {0x50, 0x4B, 0x03, 0x04};   // "PK\3\4"

    private final Path koren;

    public SkladisteService(@Value("${app.storage.dir}") String dir) {
        this.koren = Path.of(dir).toAbsolutePath().normalize();
    }

    // cuva ZIP pod relativnom putanjom i vraca tu putanju za upis u bazu
    public String sacuvajZip(MultipartFile fajl, String relativno) {
        if (fajl.isEmpty()) throw new BadRequestException("Fajl je prazan");
        try {
            try (InputStream in = fajl.getInputStream()) {
                if (!Arrays.equals(in.readNBytes(4), ZIP_ZAGLAVLJE)) throw new BadRequestException("Fajl nije ZIP arhiva");
            }
            Path cilj = koren.resolve(relativno);
            Files.createDirectories(cilj.getParent());
            // prvo privremeni fajl pa premestanje, da prekinut upload ne pokvari postojeci ZIP
            Path tmp = Files.createTempFile(cilj.getParent(), "upload", ".tmp");
            try (InputStream in = fajl.getInputStream()) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(tmp, cilj, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return relativno;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Path putanja(String relativno) {
        return koren.resolve(relativno);
    }
}
