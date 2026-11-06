// Glavni proces: prozor, pozivi REST API-ja i rad sa fajlovima.
// Renderer ne pristupa disku ni mrezi direktno, nego kroz window.lms (preload.js).
const { app, BrowserWindow, ipcMain, dialog } = require('electron');
const path = require('node:path');
const fs = require('node:fs');
const os = require('node:os');
const AdmZip = require('adm-zip');
const { unutar, parsirajSpisak } = require('./lib');

let server = process.env.RAF_LMS_API || 'http://localhost:8080/api/v1';
let token = null;
let prozor = null;

function poruka(status, data) {
  if (data && typeof data === 'object' && !Buffer.isBuffer(data)) {
    if (data.errors) return (data.detail || 'Neispravni podaci') + ': ' + Object.entries(data.errors).map(([k, v]) => k + ' ' + v).join(', ');
    if (data.detail) return data.detail;
  }
  if (status === 401) return 'Prijava nije ispravna ili je istekla';
  if (status === 403) return 'Nemate pravo na ovu akciju';
  return 'Greska servera (' + status + ')';
}

async function api(method, putanja, body) {
  const headers = {};
  if (token) headers.Authorization = 'Bearer ' + token;
  let payload;
  if (body instanceof FormData) payload = body;
  else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }
  let res;
  try {
    res = await fetch(server + putanja, { method, headers, body: payload });
  } catch {
    throw new Error('Server nije dostupan: ' + server);
  }
  const tip = res.headers.get('content-type') || '';
  let data = null;
  if (tip.includes('json')) data = await res.json();
  else if (res.status !== 204) data = Buffer.from(await res.arrayBuffer());
  if (!res.ok) throw new Error(poruka(res.status, data));
  return data;
}

function zipForm(buffer) {
  const form = new FormData();
  form.append('file', new Blob([buffer], { type: 'application/zip' }), 'resenje.zip');
  return form;
}

function raspakuj(buffer, dir) {
  fs.rmSync(dir, { recursive: true, force: true });
  fs.mkdirSync(dir, { recursive: true });
  for (const e of new AdmZip(buffer).getEntries()) {
    if (e.isDirectory) continue;
    const cilj = unutar(dir, e.entryName);
    fs.mkdirSync(path.dirname(cilj), { recursive: true });
    fs.writeFileSync(cilj, e.getData());
  }
  return dir;
}

function spakuj(dir) {
  const zip = new AdmZip();
  zip.addLocalFolder(dir);
  return zip.toBuffer();
}

function stablo(dir, rel = '') {
  const fajlovi = [];
  for (const e of fs.readdirSync(path.join(dir, rel), { withFileTypes: true })) {
    const r = rel ? rel + '/' + e.name : e.name;
    if (e.isDirectory()) fajlovi.push(...stablo(dir, r));
    else fajlovi.push(r);
  }
  return fajlovi.sort();
}

function imaFajlova(dir) {
  return fs.existsSync(dir) && stablo(dir).length > 0;
}

const folder = (...delovi) => path.join(app.getPath('userData'), ...delovi);

// svaki handler vraca {ok, data} ili {ok:false, error}, da renderer dobije citljivu poruku
function handle(ime, fn) {
  ipcMain.handle(ime, async (_e, ...args) => {
    try {
      return { ok: true, data: await fn(...args) };
    } catch (e) {
      return { ok: false, error: e.message };
    }
  });
}

handle('config', () => ({
  server,
  // dodatna funkcionalnost: podaci iz okruzenja OS-a za automatsko popunjavanje prijave
  osKorisnik: os.userInfo().username,
  email: process.env.RAF_LMS_EMAIL || null,
}));

handle('login', async (srv, email, lozinka) => {
  server = srv.trim().replace(/\/$/, '');
  token = null;
  const d = await api('POST', '/auth/login', { username: email, password: lozinka });
  token = d.accessToken;
  return d;
});

handle('logout', () => { token = null; });

handle('api', (method, putanja, body) => api(method, putanja, body));

handle('fs:stablo', (dir) => stablo(dir));

handle('fs:citaj', (dir, rel) => {
  const buf = fs.readFileSync(unutar(dir, rel));
  if (buf.subarray(0, 8000).includes(0)) return { binarni: true, tekst: '' };
  return { binarni: false, tekst: buf.toString('utf8') };
});

handle('fs:pisi', (dir, rel, tekst) => fs.writeFileSync(unutar(dir, rel), tekst, 'utf8'));

// lokalna kopija ima prednost (nastavak posle pada aplikacije), zatim sacuvano resenje sa servera
// (nastavak na drugom racunaru), a tek onda originalni zadatak
handle('student:otvori', async (rad) => {
  const dir = folder('radovi', String(rad.id));
  if (imaFajlova(dir)) return { dir, izvor: 'lokalna kopija' };
  if (rad.hasSavedWork) {
    raspakuj(await api('GET', `/student/attempts/${rad.id}/submission`), dir);
    return { dir, izvor: 'poslednje sacuvano resenje sa servera' };
  }
  raspakuj(await api('GET', `/student/attempts/${rad.id}/task`), dir);
  return { dir, izvor: 'zadatak sa servera' };
});

handle('student:predaj', (radId, dir, konacno) =>
  api('POST', `/student/attempts/${radId}/submission?final=${konacno}`, zipForm(spakuj(dir))));

// predato resenje za pregled komentara; posle termina server i dalje daje studentu njegov rad
handle('student:otvoriPredato', async (radId) => {
  const dir = folder('povratna', String(radId));
  raspakuj(await api('GET', `/student/attempts/${radId}/submission`), dir);
  return dir;
});

handle('nastavnik:postaviZadatak', async (examId, vrsta) => {
  const izbor = await dialog.showOpenDialog(prozor, vrsta === 'folder'
    ? { title: 'Folder projekta', properties: ['openDirectory'] }
    : { title: 'ZIP projekta', properties: ['openFile'], filters: [{ name: 'ZIP', extensions: ['zip'] }] });
  if (izbor.canceled) return null;
  const p = izbor.filePaths[0];
  const buf = vrsta === 'folder' ? spakuj(p) : fs.readFileSync(p);
  const form = new FormData();
  form.append('file', new Blob([buf], { type: 'application/zip' }), vrsta === 'folder' ? path.basename(p) + '.zip' : path.basename(p));
  await api('PUT', `/teacher/exams/${examId}/task`, form);
  return p;
});

handle('nastavnik:ucitajSpisak', async (termId) => {
  const izbor = await dialog.showOpenDialog(prozor, {
    title: 'Spisak prijavljenih (CSV: indeks, ime, prezime, email)',
    properties: ['openFile'], filters: [{ name: 'CSV', extensions: ['csv', 'txt'] }],
  });
  if (izbor.canceled) return null;
  const students = parsirajSpisak(fs.readFileSync(izbor.filePaths[0], 'utf8'));
  return api('PUT', `/teacher/terms/${termId}/roster`, { students });
});

handle('nastavnik:preuzmiSve', async (upit) => {
  const buf = await api('GET', '/teacher/submissions/download' + upit);
  const izbor = await dialog.showSaveDialog(prozor, { title: 'Sacuvaj radove', defaultPath: 'radovi.zip' });
  if (izbor.canceled) return null;
  fs.writeFileSync(izbor.filePath, buf);
  return izbor.filePath;
});

handle('nastavnik:otvoriRad', async (radId) => {
  const dir = folder('pregled', String(radId));
  raspakuj(await api('GET', `/teacher/submissions/${radId}/file`), dir);
  return dir;
});

app.whenReady().then(() => {
  prozor = new BrowserWindow({
    width: 1400,
    height: 900,
    title: 'RAF LMS',
    webPreferences: { preload: path.join(__dirname, 'preload.js'), contextIsolation: true, sandbox: true },
  });
  prozor.removeMenu();
  prozor.loadFile('index.html');
  if (process.env.RAF_LMS_DEVTOOLS) prozor.webContents.openDevTools();
});

app.on('window-all-closed', () => app.quit());
