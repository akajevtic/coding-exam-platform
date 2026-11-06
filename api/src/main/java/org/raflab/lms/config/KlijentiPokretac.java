package org.raflab.lms.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/*
 * Za lokalni rad i predaju projekta: kad se API pokrene (npr. Run u IntelliJ-u), pokrece i
 * monitoring (Vite, otvara se u pregledacu) i editor (Electron prozor). Pri prvom pokretanju radi npm install.
 * Iskljucuje se sa app.dev.pokreni-klijente=false (POKRENI_KLIJENTE=false). Potreban je Node.js.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.dev.pokreni-klijente", havingValue = "true")
public class KlijentiPokretac {

    private static final String NPM = System.getProperty("os.name").toLowerCase().contains("win") ? "npm.cmd" : "npm";

    private final List<Process> procesi = new CopyOnWriteArrayList<>();

    @EventListener(ApplicationReadyEvent.class)
    public void pokreni() {
        Path koren = korenProjekta();
        if (koren == null) {
            log.warn("Folderi editor/ i monitoring/ nisu nadjeni u {}, klijenti se ne pokrecu", Path.of("").toAbsolutePath());
            return;
        }
        // npm install moze da traje nekoliko minuta, pa ne blokira start API-ja
        Thread.ofVirtual().name("klijenti").start(() -> {
            pokreni(koren.resolve("monitoring"), NPM, "run", "dev", "--", "--open");
            pokreni(koren.resolve("editor"), NPM, "start");
        });
    }

    private void pokreni(Path folder, String... komanda) {
        try {
            if (!Files.isDirectory(folder.resolve("node_modules"))) {
                log.info("Prvo pokretanje: npm install u {}", folder);
                int kod = new ProcessBuilder(NPM, "install").directory(folder.toFile()).inheritIO().start().waitFor();
                if (kod != 0) {
                    log.warn("npm install u {} nije uspeo (kod {})", folder, kod);
                    return;
                }
            }
            procesi.add(new ProcessBuilder(komanda).directory(folder.toFile()).inheritIO().start());
            log.info("Pokrenuto: {} u {}", String.join(" ", komanda), folder.getFileName());
        } catch (IOException e) {
            log.warn("Ne mogu da pokrenem {} ({}). Da li je instaliran Node.js?", folder.getFileName(), e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // radni folder je koren repozitorijuma (IntelliJ) ili api/ (mvn spring-boot:run)
    private static Path korenProjekta() {
        Path cwd = Path.of("").toAbsolutePath();
        for (Path p : new Path[]{cwd, cwd.getParent()}) {
            if (p != null && Files.isDirectory(p.resolve("editor")) && Files.isDirectory(p.resolve("monitoring"))) return p;
        }
        return null;
    }

    // npm pokrece node, a node Electron/Vite, pa se gase i potomci
    @PreDestroy
    public void ugasi() {
        for (Process p : procesi) {
            p.descendants().forEach(ProcessHandle::destroy);
            p.destroy();
        }
    }
}
