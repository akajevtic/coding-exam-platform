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

Detalji specifikacije mogu se menjati tokom semestra. Nove zahteve dodavati u backlog pre implementacije.
