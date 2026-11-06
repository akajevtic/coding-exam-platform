'use strict';
/* global monaco, lms */
// Renderer: ekrani za studenta i nastavnika. Mreza i disk idu kroz window.lms (preload.js).

const $ = (sel, el = document) => el.querySelector(sel);
const app = $('#app');
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const vreme = (iso) => (iso ? new Date(iso).toLocaleString('sr-RS') : '—');
const trajanje = (s) => (s == null ? '—' : `${Math.floor(s / 60)} min ${s % 60} s`);

let config = null;
let ciscenje = [];   // tajmeri i editori koje treba ugasiti pri promeni ekrana

function poruka(tekst, ok = false) {
  const el = $('#poruka');
  el.textContent = tekst;
  el.className = ok ? 'ok' : '';
  el.hidden = false;
  clearTimeout(poruka.t);
  poruka.t = setTimeout(() => { el.hidden = true; }, ok ? 4000 : 10000);
}

async function poziv(obecanje) {
  const r = await obecanje;
  if (!r.ok) {
    poruka(r.error);
    throw new Error(r.error);
  }
  return r.data;
}
const api = (metod, putanja, telo) => poziv(lms.api(metod, putanja, telo));

function ekran(html) {
  ciscenje.forEach((f) => f());
  ciscenje = [];
  app.innerHTML = html;
}

// dugme koje se gasi dok traje akcija, da se zahtev ne posalje dva puta
function naKlik(el, fn) {
  el.addEventListener('click', async (e) => {
    e.preventDefault();
    el.disabled = true;
    try { await fn(e); } catch { /* poruka je vec prikazana */ } finally { el.disabled = false; }
  });
}

function naSlanje(forma, fn) {
  forma.addEventListener('submit', async (e) => {
    e.preventDefault();
    const dugme = forma.querySelector('button[type=submit], button:not([type])');
    if (dugme) dugme.disabled = true;
    try { await fn(Object.fromEntries(new FormData(forma))); } catch { /* poruka je vec prikazana */ } finally { if (dugme) dugme.disabled = false; }
  });
}

// ---------- prijava ----------

function prikaziPrijavu() {
  $('#korisnik').textContent = '';
  $('#odjava').hidden = true;
  ekran(`
    <div class="strana" style="max-width:420px">
      <form class="kartica" id="prijava">
        <h2>Prijava</h2>
        <label>Server <input name="server" value="${esc(config.server)}" required></label><br>
        <label>Email <input name="email" type="email" value="${esc(config.email || '')}" placeholder="${esc(config.osKorisnik)}@raf.rs" required autofocus></label><br>
        <label>Lozinka <input name="lozinka" type="password" required></label><br>
        <button class="glavno">Prijavi se</button>
        <p class="sivo">Nalog operativnog sistema: <strong>${esc(config.osKorisnik)}</strong></p>
      </form>
    </div>`);
  naSlanje($('#prijava'), async (f) => {
    const d = await poziv(lms.login(f.server, f.email, f.lozinka));
    config.server = f.server;
    $('#korisnik').textContent = `${f.email} (${d.role === 'STUDENT' ? 'student' : 'nastavnik'})`;
    $('#odjava').hidden = false;
    if (d.role === 'STUDENT') studentPocetna();
    else nastavnikPocetna('polaganja');
  });
}

$('#odjava').addEventListener('click', async () => {
  await lms.logout();
  prikaziPrijavu();
});

function promenaLozinkeHtml() {
  return `
    <form class="kartica" id="lozinka">
      <h3>Promena lozinke</h3>
      <div class="red">
        <label>Trenutna <input name="oldPassword" type="password" required></label>
        <label>Nova (bar 8 znakova) <input name="newPassword" type="password" minlength="8" required></label>
        <button>Promeni</button>
      </div>
    </form>`;
}

function povezLozinku() {
  naSlanje($('#lozinka'), async (f) => {
    await api('POST', '/auth/change-password', f);
    $('#lozinka').reset();
    poruka('Lozinka je promenjena', true);
  });
}

// ---------- zajednicki radni prostor: stablo fajlova + Monaco + bocni panel ----------

function jezik(rel) {
  const ext = '.' + rel.split('.').pop().toLowerCase();
  const l = monaco.languages.getLanguages().find((x) => (x.extensions || []).includes(ext));
  return l ? l.id : 'plaintext';
}

/*
 * Pravi ekran sa projektom iz foldera `dir`. Vraca kontroler sa editorom i pomocnim funkcijama.
 * opcije: naslov, desnoHtml (dugmad u traci), panelHtml, samoCitanje, lokalnoCuvanje, dekoracije(rel) -> lista
 */
async function radniProstor(dir, opcije) {
  const fajlovi = await poziv(lms.stablo(dir));
  ekran(`
    <div class="radni">
      <div class="traka"><span class="naslov">${esc(opcije.naslov)}</span><span class="sivo" id="status"></span>
        <div class="desno">${opcije.desnoHtml || ''}</div></div>
      <div class="stablo" id="stablo">${fajlovi.map((f) => `<div data-f="${esc(f)}" title="${esc(f)}">${esc(f)}</div>`).join('')}</div>
      <div id="monaco"></div>
      <div class="panel">${opcije.panelHtml || ''}<h3>Delovi oznaceni sa TODO</h3><div id="todo" class="sivo">Trazim...</div></div>
    </div>`);

  const editor = monaco.editor.create($('#monaco'), {
    automaticLayout: true, readOnly: !!opcije.samoCitanje, glyphMargin: true, minimap: { enabled: false },
  });
  const modeli = new Map();   // rel -> { model, binarni, todo: [], dek: [] }
  const izmenjeni = new Set();
  let trenutni = null;
  let promenaOdSlanja = false;
  const todo = new Map();     // rel -> [{linija, tekst}]
  const kontroler = { zakljucan: false };

  ciscenje.push(() => { editor.dispose(); modeli.forEach((m) => m.model.dispose()); });

  function nadjiTodo(model) {
    return model.findMatches('TODO', false, false, true, null, false)
      .map((m) => m.range.startLineNumber)
      .filter((l, i, a) => a.indexOf(l) === i)
      .map((l) => ({ linija: l, tekst: model.getLineContent(l).trim() }));
  }

  function prikaziTodo() {
    const stavke = [...todo.entries()].flatMap(([rel, lista]) => lista.map((t) => ({ rel, ...t })));
    $('#todo').innerHTML = stavke.length
      ? stavke.map((t) => `<div class="stavka" data-f="${esc(t.rel)}" data-l="${t.linija}"><small>${esc(t.rel)}:${t.linija}</small>${esc(t.tekst)}</div>`).join('')
      : 'Nema oznacenih delova.';
  }

  // TODO isticanje + dodatne dekoracije (komentari nastavnika) za jedan model
  function osveziDekoracije(rel) {
    const m = modeli.get(rel);
    if (!m || m.binarni) return;
    const lista = nadjiTodo(m.model);
    todo.set(rel, lista);
    const dek = lista.map((t) => ({
      range: new monaco.Range(t.linija, 1, t.linija, 1),
      options: {
        isWholeLine: true, className: 'todo-linija', glyphMarginClassName: 'todo-glyph',
        overviewRuler: { color: '#e0a100', position: monaco.editor.OverviewRulerLane.Right },
        hoverMessage: { value: 'Deo na kome treba dati odgovor (TODO)' },
      },
    }));
    if (opcije.dekoracije) dek.push(...opcije.dekoracije(rel));
    m.dek = m.model.deltaDecorations(m.dek, dek);
    prikaziTodo();
  }

  async function ucitaj(rel) {
    if (modeli.has(rel)) return modeli.get(rel);
    const f = await poziv(lms.citaj(dir, rel));
    const model = monaco.editor.createModel(f.binarni ? '(binarni fajl, ne prikazuje se)' : f.tekst, f.binarni ? 'plaintext' : jezik(rel));
    const m = { model, binarni: f.binarni, dek: [] };
    modeli.set(rel, m);
    if (!f.binarni) {
      let tajmer = null;
      model.onDidChangeContent(() => {
        izmenjeni.add(rel);
        promenaOdSlanja = true;
        oznaciStablo();
        clearTimeout(tajmer);
        tajmer = setTimeout(() => osveziDekoracije(rel), 300);
        // lokalna kopija na disku, da se rad ne izgubi ako aplikacija padne
        if (opcije.lokalnoCuvanje) { clearTimeout(m.cuvanje); m.cuvanje = setTimeout(() => sacuvaj(rel), 1000); }
      });
    }
    osveziDekoracije(rel);
    return m;
  }

  function oznaciStablo() {
    document.querySelectorAll('#stablo div').forEach((d) => {
      d.classList.toggle('izabran', d.dataset.f === trenutni);
      d.classList.toggle('izmenjen', izmenjeni.has(d.dataset.f));
    });
  }

  async function otvori(rel, linija) {
    const m = await ucitaj(rel);
    trenutni = rel;
    editor.setModel(m.model);
    editor.updateOptions({ readOnly: !!opcije.samoCitanje || m.binarni || kontroler.zakljucan });
    if (linija) { editor.revealLineInCenter(linija); editor.setPosition({ lineNumber: linija, column: 1 }); }
    editor.focus();
    oznaciStablo();
  }

  async function sacuvaj(rel) {
    const m = modeli.get(rel);
    if (!m || m.binarni || !izmenjeni.has(rel)) return;
    await poziv(lms.pisi(dir, rel, m.model.getValue()));
    izmenjeni.delete(rel);
    oznaciStablo();
  }

  const sacuvajSve = async () => { for (const rel of [...izmenjeni]) await sacuvaj(rel); };

  $('#stablo').addEventListener('click', (e) => { if (e.target.dataset.f) otvori(e.target.dataset.f); });
  $('.radni').addEventListener('click', (e) => {
    const s = e.target.closest('.stavka[data-f]');
    if (s) otvori(s.dataset.f, Number(s.dataset.l));
  });

  // TODO pregled za ceo projekat odmah po otvaranju
  for (const rel of fajlovi) await ucitaj(rel).catch(() => {});
  if (fajlovi.length) await otvori(fajlovi.find((f) => todo.get(f)?.length) || fajlovi[0]);
  else $('#todo').textContent = 'Projekat je prazan.';

  return Object.assign(kontroler, {
    editor, otvori, sacuvajSve, osveziDekoracije,
    trenutni: () => trenutni,
    imaPromena: () => promenaOdSlanja,
    poslato: () => { promenaOdSlanja = false; },
    status: (t) => { $('#status').textContent = t; },
    zakljucaj: () => { kontroler.zakljucan = true; editor.updateOptions({ readOnly: true }); },
    osveziSve: () => modeli.forEach((_m, rel) => osveziDekoracije(rel)),
  });
}

// dekoracije za komentare nastavnika (vidi ih i student i nastavnik)
function komentarDekoracije(komentari, rel) {
  // opseg do kraja linije (Monaco ga skrati), da se tekst komentara prikaze iza koda
  return komentari.filter((k) => k.filePath === rel).map((k) => ({
    range: new monaco.Range(k.line, 1, k.line, 100000),
    options: {
      isWholeLine: true, className: 'komentar-linija', glyphMarginClassName: 'komentar-glyph',
      hoverMessage: { value: 'Komentar nastavnika: ' + k.text },
      after: { content: '   // ' + k.text, inlineClassName: 'komentar-inline' },
      overviewRuler: { color: '#2457c5', position: monaco.editor.OverviewRulerLane.Left },
    },
  }));
}

function komentariHtml(komentari, saBrisanjem) {
  if (!komentari.length) return '<p class="sivo">Nema komentara.</p>';
  return komentari.map((k, i) => `
    <div class="stavka" data-f="${esc(k.filePath)}" data-l="${k.line}">
      <small>${esc(k.filePath)}:${k.line}</small>${esc(k.text)}
      ${saBrisanjem ? `<button data-obrisi="${i}" style="float:right">Obrisi</button>` : ''}
    </div>`).join('');
}

// ---------- student ----------

async function studentPocetna() {
  const [ja, termini, radovi] = await Promise.all([api('GET', '/student/me'), api('GET', '/student/terms'), api('GET', '/student/attempts')]);
  ekran(`
    <div class="strana">
      <form class="kartica" id="polaganje">
        <h2>Prijava na polaganje</h2>
        ${termini.length ? `
        <div class="red">
          <label>Termin <select name="termId" id="termin">${termini.map((t) => `<option value="${t.id}">${esc(t.subject.name)} · ${esc(t.examPeriod.name)} · ${vreme(t.startsAt)} – ${vreme(t.endsAt)}${t.room ? ' · ' + esc(t.room) : ''}</option>`).join('')}</select></label>
          <label>Grupa <select name="group" id="grupa"></select></label>
          <label>Indeks <input name="studentNumber" value="${esc(ja.studentNumber)}" required></label>
          <label>Ime i prezime <input value="${esc(ja.firstName + ' ' + ja.lastName)}" disabled></label>
          <button class="glavno">Prijavi se i preuzmi zadatak</button>
        </div>
        <p class="sivo">Podaci su popunjeni iz vaseg naloga. Zadatak moze da se preuzme samo tokom termina.</p>`
        : '<p class="sivo">Nema termina za koje ste na spisku prijavljenih.</p>'}
      </form>
      <div class="kartica">
        <h2>Moji radovi</h2>
        ${radovi.length ? `<table><tr><th>Predmet</th><th>Grupa</th><th>Zadatak</th><th>Status</th><th>Preuzeto</th><th>Predato</th><th></th></tr>
          ${radovi.map((r) => `<tr><td>${esc(r.subject)}</td><td>${esc(r.group)}</td><td>${esc(r.title)}</td>
            <td><span class="oznaka ${r.status}">${r.status === 'PREDATO' ? 'predato' : 'u izradi'}</span></td>
            <td>${vreme(r.downloadedAt)}</td><td>${vreme(r.submittedAt)}</td>
            <td>${Date.parse(r.endsAt) > Date.parse(r.serverTime) ? `<button data-nastavi="${r.id}">Nastavi rad</button>` : ''}
                <button data-povratna="${r.id}">Ocena i komentari</button></td></tr>`).join('')}</table>`
        : '<p class="sivo">Jos nemate radova.</p>'}
      </div>
      ${promenaLozinkeHtml()}
    </div>`);
  povezLozinku();

  const grupe = () => {
    const t = termini.find((x) => String(x.id) === $('#termin').value);
    $('#grupa').innerHTML = (t?.exams || []).map((e) => `<option>${esc(e.group)}</option>`).join('');
  };
  if (termini.length) {
    grupe();
    $('#termin').addEventListener('change', grupe);
    naSlanje($('#polaganje'), async (f) => {
      const rad = await api('POST', '/student/attempts', { termId: Number(f.termId), group: f.group, studentNumber: f.studentNumber });
      await studentRad(rad);
    });
  }
  app.querySelectorAll('[data-nastavi]').forEach((b) => naKlik(b, async () => studentRad(await api('GET', `/student/attempts/${b.dataset.nastavi}`))));
  app.querySelectorAll('[data-povratna]').forEach((b) => naKlik(b, async () => studentPovratna(radovi.find((r) => String(r.id) === b.dataset.povratna))));
}

async function studentRad(rad) {
  const { dir, izvor } = await poziv(lms.otvoriRad(rad));
  const p = await radniProstor(dir, {
    naslov: `${rad.subject} · ${rad.group} · ${rad.title}`,
    lokalnoCuvanje: true,
    desnoHtml: `<span id="tajmer"></span><button id="sacuvaj">Sacuvaj (Ctrl+S)</button>
      <button class="glavno" id="predaj">Predaj resenje</button><button id="nazad">Nazad</button>`,
    panelHtml: `<h3>Automatsko slanje</h3>
      <label>Interval <select id="interval"><option value="30">30 s</option><option value="60" selected>1 min</option><option value="120">2 min</option></select></label>
      <p class="sivo" id="autosave">Jos nije poslato.</p>`,
  });
  p.status(`Otvoreno: ${izvor}`);

  const posalji = async (konacno) => {
    await p.sacuvajSve();
    const r = await poziv(lms.predaj(rad.id, dir, konacno));
    p.poslato();
    return r;
  };
  const sacuvaj = async () => {
    await p.sacuvajSve();
    p.status('Sacuvano lokalno u ' + new Date().toLocaleTimeString('sr-RS'));
  };
  p.editor.addCommand(monaco.KeyMod.CtrlCmd | monaco.KeyCode.KeyS, sacuvaj);
  naKlik($('#sacuvaj'), sacuvaj);
  naKlik($('#nazad'), async () => { await p.sacuvajSve(); studentPocetna(); });
  naKlik($('#predaj'), async () => {
    if (!confirm('Predati resenje? Do kraja termina mozete predati ponovo, nova predaja zamenjuje prethodnu.')) return;
    const r = await posalji(true);
    p.status('Predato u ' + vreme(r.submittedAt));
    poruka('Resenje je uspesno predato.', true);
  });

  // automatsko slanje: salje se samo ako je bilo promena od poslednjeg slanja; rad ostaje U_IZRADI
  let autoTajmer = null;
  const autosave = async () => {
    if (!p.imaPromena() || p.zakljucan) return;
    try {
      await posalji(false);
      $('#autosave').textContent = 'Poslednje automatsko slanje: ' + new Date().toLocaleTimeString('sr-RS');
    } catch { /* poruka je prikazana, pokusava se ponovo u sledecem intervalu */ }
  };
  const pokreniAuto = () => { clearInterval(autoTajmer); autoTajmer = setInterval(autosave, Number($('#interval').value) * 1000); };
  $('#interval').addEventListener('change', pokreniAuto);
  pokreniAuto();
  ciscenje.push(() => clearInterval(autoTajmer));

  // tajmer po serverskom satu: razlika izmedju serverskog i lokalnog sata se racuna jednom
  const razlika = Date.parse(rad.serverTime) - Date.now();
  const kraj = Date.parse(rad.endsAt);
  let poslednjeSlanje = false;
  const otkucaj = () => {
    const ostalo = kraj - (Date.now() + razlika);
    const el = $('#tajmer');
    if (!el) return;
    if (ostalo <= 0) {
      el.textContent = 'Vreme je isteklo';
      el.className = 'malo';
      p.zakljucaj();
      $('#predaj').disabled = true;
      clearInterval(sat);
      return;
    }
    const s = Math.floor(ostalo / 1000);
    el.textContent = `${String(Math.floor(s / 3600)).padStart(2, '0')}:${String(Math.floor(s / 60) % 60).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;
    el.className = ostalo < 5 * 60 * 1000 ? 'malo' : '';
    // pred sam kraj salje se poslednja verzija, da nastavnik dobije i rad koji nije rucno predat
    if (ostalo < 20000 && !poslednjeSlanje) { poslednjeSlanje = true; autosave(); }
  };
  const sat = setInterval(otkucaj, 1000);
  otkucaj();
  ciscenje.push(() => clearInterval(sat));
}

async function studentPovratna(rad) {
  const fb = await api('GET', `/student/attempts/${rad.id}/feedback`);
  const dir = await poziv(lms.otvoriPredato(rad.id));
  const p = await radniProstor(dir, {
    naslov: `Ocena: ${rad.subject} · ${rad.group} · ${rad.title}`,
    samoCitanje: true,
    desnoHtml: '<button id="nazad">Nazad</button>',
    panelHtml: `<h3>Poeni</h3><p style="font-size:20px;font-weight:700">${fb.score == null ? '<span class="sivo">Jos nije ocenjeno</span>' : esc(fb.score)}</p>
      <h3>Komentari nastavnika</h3>${komentariHtml(fb.comments, false)}`,
    dekoracije: (rel) => komentarDekoracije(fb.comments, rel),
  });
  p.status('Predato resenje (samo za citanje)');
  naKlik($('#nazad'), studentPocetna);
}

// ---------- nastavnik ----------

function tabovi(aktivan) {
  return `<div class="tabovi">
    <button data-tab="polaganja" class="${aktivan === 'polaganja' ? 'aktivan' : ''}">Polaganja</button>
    <button data-tab="radovi" class="${aktivan === 'radovi' ? 'aktivan' : ''}">Radovi studenata</button></div>`;
}

function povezTabove() {
  app.querySelectorAll('[data-tab]').forEach((b) => b.addEventListener('click', () => nastavnikPocetna(b.dataset.tab)));
}

const opcije = (lista, tekst, prazno) => (prazno ? `<option value="">${prazno}</option>` : '') + lista.map((x) => `<option value="${x.id}">${esc(tekst(x))}</option>`).join('');
const opisTermina = (t) => `${t.subject.name} · ${t.examPeriod.name} · ${vreme(t.startsAt)}${t.room ? ' · ' + t.room : ''}`;

async function nastavnikPocetna(tab) {
  const [predmeti, rokovi, termini] = await Promise.all([api('GET', '/subjects'), api('GET', '/exam-periods'), api('GET', '/teacher/terms')]);
  if (tab === 'radovi') return nastavnikRadovi(predmeti, rokovi, termini);

  ekran(`
    <div class="strana">
      ${tabovi('polaganja')}
      <div class="kartica">
        <h2>Predmeti i ispitni rokovi</h2>
        <form class="red" id="noviPredmet"><label>Sifra <input name="code" required></label><label>Naziv predmeta <input name="name" required></label><button>Dodaj predmet</button></form><br>
        <form class="red" id="noviRok"><label>Naziv roka <input name="name" placeholder="Januarski" required></label><label>Skolska godina <input name="schoolYear" placeholder="2026/2027" required></label><button>Dodaj rok</button></form>
      </div>
      <form class="kartica" id="noviTermin">
        <h2>Novi termin</h2>
        <div class="red">
          <label>Predmet <select name="subjectId" required>${opcije(predmeti, (p) => `${p.code} – ${p.name}`)}</select></label>
          <label>Ispitni rok <select name="examPeriodId" required>${opcije(rokovi, (r) => `${r.name} ${r.schoolYear}`)}</select></label>
          <label>Pocetak <input name="startsAt" type="datetime-local" required></label>
          <label>Kraj <input name="endsAt" type="datetime-local" required></label>
          <label>Ucionica <input name="room" placeholder="RAF1"></label>
          <button class="glavno">Napravi termin</button>
        </div>
      </form>
      ${termini.map((t) => `
        <div class="kartica">
          <h3>${esc(opisTermina(t))} – ${vreme(t.endsAt)}</h3>
          <table><tr><th>Grupa</th><th>Zadatak</th><th>Projekat</th><th></th></tr>
            ${t.exams.map((e) => `<tr><td>${esc(e.group)}</td><td>${esc(e.title)}<br><span class="sivo">${esc(e.description)}</span></td>
              <td>${e.hasTask ? '<span class="oznaka PREDATO">ZIP ucitan</span>' : '<span class="oznaka">nije postavljen</span>'}</td>
              <td><button data-zip="${e.id}">Postavi ZIP</button> <button data-folder="${e.id}">Postavi folder</button></td></tr>`).join('')}
          </table><br>
          <form class="red" data-grupa="${t.id}">
            <label>Grupa <input name="group" placeholder="Grupa 1" required></label>
            <label>Naziv zadatka <input name="title" required></label>
            <label>Opis <input name="description"></label>
            <button>Dodaj grupu</button>
          </form><br>
          <button data-spisak="${t.id}">Ucitaj spisak prijavljenih (CSV)</button>
          <span class="sivo">Kolone: indeks, ime, prezime, email. Vazi za predmet i rok termina; novi studenti dobijaju nalog.</span>
        </div>`).join('') || '<p class="sivo">Jos nema termina.</p>'}
    </div>`);
  povezTabove();
  const osvezi = () => nastavnikPocetna('polaganja');

  naSlanje($('#noviPredmet'), async (f) => { await api('POST', '/teacher/subjects', f); poruka('Predmet je dodat', true); osvezi(); });
  naSlanje($('#noviRok'), async (f) => { await api('POST', '/teacher/exam-periods', f); poruka('Rok je dodat', true); osvezi(); });
  naSlanje($('#noviTermin'), async (f) => {
    await api('POST', '/teacher/terms', {
      subjectId: Number(f.subjectId), examPeriodId: Number(f.examPeriodId),
      startsAt: new Date(f.startsAt).toISOString(), endsAt: new Date(f.endsAt).toISOString(), room: f.room || null,
    });
    poruka('Termin je napravljen', true);
    osvezi();
  });
  app.querySelectorAll('[data-grupa]').forEach((forma) => naSlanje(forma, async (f) => {
    await api('POST', `/teacher/terms/${forma.dataset.grupa}/groups`, f);
    poruka('Grupa je dodata', true);
    osvezi();
  }));
  app.querySelectorAll('[data-zip],[data-folder]').forEach((b) => naKlik(b, async () => {
    const vrsta = b.dataset.zip ? 'zip' : 'folder';
    const p = await poziv(lms.postaviZadatak(b.dataset.zip || b.dataset.folder, vrsta));
    if (p) { poruka('Zadatak je postavljen: ' + p, true); osvezi(); }
  }));
  app.querySelectorAll('[data-spisak]').forEach((b) => naKlik(b, async () => {
    const r = await poziv(lms.ucitajSpisak(b.dataset.spisak));
    if (r) poruka(`Spisak je sacuvan: ${r.registered} studenata, od toga ${r.newStudents} novih naloga.`, true);
  }));
}

async function nastavnikRadovi(predmeti, rokovi, termini, filter = {}) {
  ekran(`
    <div class="strana" style="max-width:1300px">
      ${tabovi('radovi')}
      <form class="kartica" id="filter">
        <h2>Kriterijumi selekcije</h2>
        <div class="red">
          <label>Predmet <select name="subjectId">${opcije(predmeti, (p) => p.name, 'svi')}</select></label>
          <label>Rok <select name="examPeriodId">${opcije(rokovi, (r) => `${r.name} ${r.schoolYear}`, 'svi')}</select></label>
          <label>Termin <select name="termId" id="fTermin">${opcije(termini, opisTermina, 'svi')}</select></label>
          <label>Grupa <select name="groupId" id="fGrupa"><option value="">sve</option></select></label>
          <label>Ucionica <input name="room" size="8"></label>
          <label>Student (indeks) <input name="studentNumber" size="12"></label>
          <label>Status <select name="status"><option value="">svi</option><option value="U_IZRADI">u izradi</option><option value="PREDATO">predato</option></select></label>
          <button class="glavno">Prikazi</button>
          <button type="button" id="preuzmi">Preuzmi izabrane (ZIP)</button>
        </div>
      </form>
      <div class="kartica" id="rezultat"><p class="sivo">Izaberite kriterijume i kliknite Prikazi.</p></div>
    </div>`);
  povezTabove();
  const forma = $('#filter');
  Object.entries(filter).forEach(([k, v]) => { if (forma.elements[k]) forma.elements[k].value = v; });

  const grupe = () => {
    const t = termini.find((x) => String(x.id) === $('#fTermin').value);
    $('#fGrupa').innerHTML = '<option value="">sve</option>' + (t ? t.exams.map((e) => `<option value="${e.groupId}">${esc(e.group)}</option>`).join('') : '');
    if (filter.groupId) $('#fGrupa').value = filter.groupId;
  };
  $('#fTermin').addEventListener('change', () => { filter.groupId = ''; grupe(); });
  grupe();

  const upit = () => {
    const f = Object.fromEntries(new FormData(forma));
    Object.assign(filter, f);
    const q = new URLSearchParams(Object.entries(f).filter(([, v]) => v !== ''));
    return q.toString() ? '?' + q : '';
  };
  const prikazi = async () => {
    const radovi = await api('GET', '/teacher/submissions' + upit());
    $('#rezultat').innerHTML = radovi.length ? `<p class="sivo">Pronadjeno radova: ${radovi.length}</p>
      <table><tr><th>Indeks</th><th>Student</th><th>Predmet</th><th>Grupa</th><th>Ucionica</th><th>Status</th><th>Preuzeto</th><th>Predato</th><th>Vreme izrade</th><th>Poeni</th><th></th></tr>
      ${radovi.map((r) => `<tr><td>${esc(r.studentNumber)}</td><td>${esc(r.firstName)} ${esc(r.lastName)}</td><td>${esc(r.subject)}</td>
        <td>${esc(r.group)}</td><td>${esc(r.room)}</td><td><span class="oznaka ${r.status}">${r.status === 'PREDATO' ? 'predato' : 'u izradi'}</span></td>
        <td>${vreme(r.downloadedAt)}</td><td>${vreme(r.submittedAt)}</td><td>${trajanje(r.durationSeconds)}</td><td>${esc(r.score ?? '—')}</td>
        <td>${r.hasFile ? `<button data-pregled="${r.id}">Pregledaj</button>` : '<span class="sivo">nema fajla</span>'}</td></tr>`).join('')}</table>`
      : '<p class="sivo">Nema radova za izabrane kriterijume.</p>';
    $('#rezultat').querySelectorAll('[data-pregled]').forEach((b) => naKlik(b, () =>
      pregledRada(radovi.find((r) => String(r.id) === b.dataset.pregled), () => nastavnikRadovi(predmeti, rokovi, termini, filter))));
  };
  naSlanje(forma, prikazi);
  naKlik($('#preuzmi'), async () => {
    const p = await poziv(lms.preuzmiSve(upit()));
    if (p) poruka('Radovi su sacuvani u ' + p, true);
  });
  if (Object.keys(filter).length) prikazi();
}

async function pregledRada(rad, nazad) {
  const dir = await poziv(lms.otvoriRadZaPregled(rad.id));
  const ocena = await api('GET', `/teacher/submissions/${rad.id}/grade`);
  const komentari = ocena.comments.map(({ filePath, line, text }) => ({ filePath, line, text }));

  const p = await radniProstor(dir, {
    naslov: `Pregled: ${rad.studentNumber} ${rad.firstName} ${rad.lastName} · ${rad.subject} · ${rad.group}`,
    samoCitanje: true,
    desnoHtml: '<button class="glavno" id="sacuvajOcenu">Sacuvaj ocenu</button><button id="nazad">Nazad</button>',
    panelHtml: `<h3>Poeni</h3><input id="poeni" type="number" min="0" step="0.5" value="${esc(ocena.score ?? '')}" style="width:100%">
      <h3>Novi komentar</h3><p class="sivo" id="mesto">Kliknite na liniju koda.</p>
      <textarea id="tekst" placeholder="Komentar za izabranu liniju"></textarea><button id="dodaj">Dodaj komentar</button>
      <h3>Komentari</h3><div id="komentari"></div>`,
    dekoracije: (rel) => komentarDekoracije(komentari, rel),
  });
  p.status(`Predato: ${vreme(rad.submittedAt)} · vreme izrade: ${trajanje(rad.durationSeconds)}`);

  const mesto = () => ({ filePath: p.trenutni(), line: p.editor.getPosition()?.lineNumber || 1 });
  const prikazi = () => {
    $('#komentari').innerHTML = komentariHtml(komentari, true);
    $('#komentari').querySelectorAll('[data-obrisi]').forEach((b) => b.addEventListener('click', (e) => {
      e.stopPropagation();
      komentari.splice(Number(b.dataset.obrisi), 1);
      prikazi();
      p.osveziSve();
    }));
  };
  p.editor.onDidChangeCursorPosition(() => { const m = mesto(); $('#mesto').textContent = `${m.filePath}, linija ${m.line}`; });
  naKlik($('#dodaj'), async () => {
    const tekst = $('#tekst').value.trim();
    if (!tekst || !p.trenutni()) return poruka('Izaberite liniju i upisite komentar');
    komentari.push({ ...mesto(), text: tekst });
    $('#tekst').value = '';
    prikazi();
    p.osveziDekoracije(p.trenutni());
  });
  naKlik($('#sacuvajOcenu'), async () => {
    const poeni = $('#poeni').value;
    if (poeni === '') return poruka('Upisite poene');
    await api('PUT', `/teacher/submissions/${rad.id}/grade`, { score: Number(poeni), comments: komentari });
    poruka('Ocena i komentari su sacuvani', true);
  });
  naKlik($('#nazad'), nazad);
  prikazi();
}

// ---------- pokretanje ----------

window.MonacoEnvironment = { getWorkerUrl: () => 'node_modules/monaco-editor/min/vs/editor/editor.worker.js' };
require.config({ paths: { vs: 'node_modules/monaco-editor/min/vs' } });
require(['vs/editor/editor.main'], async () => {
  config = await poziv(lms.config());
  prikaziPrijavu();
});
