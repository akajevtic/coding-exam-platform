# Opseg RAF LMS projekta

## Cilj

RAF LMS podržava postavljanje zadataka, realizaciju polaganja, predaju rešenja, nadzor i ocenjivanje. Serverski deo izlaže REST API; editor i monitoring koriste API i ne pristupaju bazi direktno.

## Komponente i bodovi iz trenutne specifikacije

| Deo | Predložena tehnologija | Maksimum |
|---|---|---:|
| REST API | Spring Boot | 22 |
| Editor za studente i nastavnike | Electron + Monaco Editor | 18 |
| Veb monitoring za nastavnike | React | 5 |
| Dodatne funkcionalnosti | izbor prema specifikaciji | do 10 |

Trenutno navedene dodatne funkcionalnosti nose ukupno 9 poena: tajmer (2), automatsko slanje/čuvanje i nastavak rada (2), podaci studenta iz okruženja (2), isticanje `TODO` delova (3). Specifikacija je radna verzija i spisak može biti dopunjen.

## Uloge i osnovni tok

### Nastavnik

- Priprema polaganje kroz predmet, ispitni rok, termin i grupu.
- Dodaje projektni zadatak i spisak prijavljenih studenata.
- Prati preuzimanja i predaje.
- Filtrira rešenja, unosi komentare uz fajl i liniju koda i dodeljuje poene.

### Student

- Unosi podatke za polaganje i grupu/termin.
- Sistem proverava da li je na spisku prijavljenih.
- Preuzima zadatak samo tokom dozvoljenog termina.
- Predaje rešenje pre isteka termina.
- Pregleda samo svoje komentare i poene.

## MVP

Prvo isporučiti kompletan obavezni tok: nastavnik kreira polaganje i zadatak → student prolazi proveru i preuzima zadatak → student predaje rešenje → nastavnik pregleda i ocenjuje → student vidi rezultat. Monitoring mora prikazati status preuzimanja/predaje i prosečno vreme izrade.

## Početni model podataka

Predmet, ispitni rok, termin, grupa, zadatak, student, prijava/spisak, korisnički nalog i uloga, pokušaj polaganja, predaja, komentar i poeni. Pokušaj polaganja čuva identitet studenta i polaganja, status, vreme preuzimanja i vreme predaje. Komentar je vezan za predaju, putanju fajla i liniju.

## Pravila koja moraju biti sprovedena na serveru

- Student mora biti na spisku za polaganje.
- Preuzimanje zadatka i predaja proveravaju vremenski prozor na serverskom satu.
- Endpoints su zaštićeni po ulozi; student dobija samo svoje predaje i ocene.
- Klientskе provere služe za prikaz, ali nisu zamena za serversku autorizaciju.

## Predloženo čuvanje projekata i rešenja

Baza čuva metapodatke i vezu do fajla. ZIP zadataka i predaja čuvaju se na serverskom disku izvan javnog direktorijuma, a preuzimaju se samo kroz autorizovani API. Ovo je početni izbor implementacije, jer specifikacija ne propisuje konkretan storage sistem.

## Otvorene stavke za zajednički dogovor

Pre zaključavanja API ugovora dogovoriti: da li student može imati više pokušaja/predaja, da li predaja zamenjuje prethodnu, kako se tačno identifikuje student, tačnu granicu početka/kraja termina i da li se komentari čuvaju uz verziju predatog fajla. Za početni MVP preporučuje se jedna aktivna predaja po studentu i polaganju, ali to potvrditi prema dopunama predmeta.
