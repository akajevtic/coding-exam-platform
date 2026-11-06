# RAF LMS

Platforma za pripremu i realizaciju kolokvijuma i ispita iz programiranja.
Ovaj repozitorijum je početni kostur za rad u paru. Za sada sadrži opis MVP-a,
podelu rada i nacrt API ugovora; funkcionalnosti aplikacije tek treba implementirati.

## Struktura

- `api/` — Spring Boot REST API (Java, MySQL, Spring Data JPA)
- `editor/` — desktop aplikacija Electron + Monaco Editor
- `monitoring/` — veb aplikacija React za nastavnički nadzor
- `docs/` — specifikacija, podela rada i nacrt API-ja

## Dogovor za rad

- `main` čuva integrisano stanje.
- Svaki član radi u svojoj grani i predlaže promene kroz Pull Request.
- API ugovor je zajednička granica: klijent može koristiti mock podatke dok se API razvija.
- Pogledati [podelu rada](docs/work-split.md), [MVP specifikaciju](docs/project-scope.md) i [API nacrt](docs/api-contract.yaml).

## Početni tok

1. Nastavnik napravi polaganje, učita zadatak i spisak prijavljenih studenata.
2. Student se prijavi za polaganje; API proverava dozvolu i vremenski prozor.
3. Student preuzme zadatak i preda rešenje kroz editor.
4. Nastavnik prati status, pregleda rešenje i dodeli poene/komentare.
5. Student vidi povratne informacije za svoje rešenje.

## Pokretanje

Potrebno: JDK 21, MySQL 8, Node.js 20+.

### Najbrze: IntelliJ

1. Otvoriti ovaj folder u IntelliJ-u (Maven projekat se uvozi sam, JDK 21).
2. Kopirati `api/.env.example` u `api/.env` i upisati lozinku za lokalni MySQL.
3. Pokrenuti run konfiguraciju **RAF LMS (API + editor + monitoring)**.

Uz API se sami pokrecu monitoring (otvara se u pregledacu na http://localhost:5173) i editor (Electron prozor).
Prvo pokretanje traje duze jer radi `npm install`. Gasenjem API-ja gase se i klijenti.
Iskljucuje se sa `POKRENI_KLIJENTE=false`.

### Pojedinacno

1. **API** (`api/`, port 8080, rute pod `/api/v1`). Kredencijali za MySQL idu u `api/.env`:
   ```
   DB_USERNAME=root
   DB_PASSWORD=<lozinka za MySQL>
   ```
   Baza `raflms` se pravi sama. Pokretanje: `mvn spring-boot:run` u `api/`, testovi (H2, bez MySQL-a): `mvn test`.
   Seeder pri prvom pokretanju pravi naloge `nastavnik@raf.rs`, `ana@raf.rs`, `marko@raf.rs`, `petar@raf.rs`
   (lozinka `lozinka123`) i termin koji traje 3 sata od pokretanja. Petar namerno nije na spisku.
2. **Editor** (`editor/`): `npm install`, pa `npm start`. Adresa API-ja se menja na ekranu za prijavu ili kroz
   `RAF_LMS_API`; `RAF_LMS_EMAIL` popunjava email na prijavi. Testovi pomocnih funkcija: `npm test`.
3. **Monitoring** (`monitoring/`): `npm install`, pa `npm run dev` (http://localhost:5173). Adresa API-ja: `VITE_API_URL`.

Spisak prijavljenih se ucitava iz CSV-a sa kolonama `indeks, ime, prezime, email`. Novi studenti dobijaju nalog
sa pocetnom lozinkom iz `STUDENT_POCETNA_LOZINKA` (podrazumevano `student123`) i menjaju je u editoru.

## Pokriveno iz specifikacije

- API: model i perzistencija, JWT prijava i uloge, zadaci i spisak, provera spiska i termina, predaja,
  podaci za monitoring, pregled po kriterijumima, komentari po fajlu i liniji, poeni.
- Editor: prijava na polaganje, preuzimanje i rad u editoru, predaja, komentari i poeni (student);
  zadaci, spisak, preuzimanje resenja po kriterijumima, pregled i ocenjivanje (nastavnik).
- Monitoring: brojevi preuzetih/predatih radova, prosecno vreme izrade, status svakog studenta,
  osvezavanje na 5 s, prikaz za telefon.
- Dodatno: tajmer po serverskom satu, automatsko slanje u intervalu i nastavak rada na drugom racunaru,
  podaci studenta iz naloga i OS korisnika na prijavi, isticanje `TODO` delova.

Detalji specifikacije mogu se menjati tokom semestra. Nove zahteve dodavati u backlog pre implementacije.
