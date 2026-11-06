# Podela rada za paralelni razvoj

## Član A — backend

- Spring Boot aplikacija, MySQL konfiguracija i JPA model.
- Prijava, role `STUDENT` i `NASTAVNIK`, autorizacija endpointa.
- Upravljanje polaganjima, listama prijavljenih i zadacima.
- Provera uslova i vremena za preuzimanje/predaju.
- Skladištenje metapodataka i ZIP fajlova, komentari, poeni i monitoring podaci.
- API i servisni testovi.

## Član B — klijenti

- Electron + Monaco editor: prijava, studentski tok, nastavnički tok za zadatke i pregled rešenja.
- React monitoring: statusi, brojači, vreme izrade, filteri i mobilni raspored.
- Mock podaci/mock API dok backend ne implementira rute.
- API klijent i obrada stanja učitavanja, grešaka i odbijene autorizacije.

## Zajednički rad pre paralelizacije

1. Pregledati i potvrditi `project-scope.md`.
2. Potvrditi otvorena pravila polaganja navedena u tom dokumentu.
3. Dogovoriti request/response payload-e i greške u `api-contract.yaml`.
4. Napraviti prvi GitHub issue/PR za svaki MVP tok i dodeliti vlasnika.

Posle toga backend i klijenti mogu da se razvijaju istovremeno. Član B koristi mock podatke zasnovane na API ugovoru; Član A ne menja ugovor bez najave i dogovora.

## GitHub tok

- `main` je stabilna grana; promene ulaze preko Pull Request-a.
- Grane imaju kratke nazive po delu posla, npr. `backend/auth`, `backend/exam-api`, `client/student-flow`, `client/monitoring`.
- Držati komponente u odvojenim folderima da se smanji konflikt.
- PR-ovi su mali i usmereni na jednu funkcionalnost; pre spajanja drugi član pregleda promenu.
- Ako se API ugovor promeni, prvo spojiti tu promenu i ažurirati mock klijent, zatim implementaciju.
- Obe osobe povremeno povlače `main` u svoje grane i integrišu aplikacije kroz vertikalne MVP tokove.

## Redosled integracije

1. Prijava i osnovni prikaz polaganja.
2. Nastavnik priprema zadatak i roster.
3. Student prolazi proveru, preuzima zadatak i predaje rad.
4. Nastavnik pregleda predaju i dodeljuje komentar/poene.
5. Student vidi samo sopstveni rezultat; monitoring prikazuje ažurirane statuse.
6. Tek zatim raditi dodatne funkcionalnosti.
