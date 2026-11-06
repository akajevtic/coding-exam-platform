// Ciste funkcije glavnog procesa, odvojene da bi mogle da se testiraju bez Electron-a (npm test).
const path = require('node:path');

// putanja unutar radnog foldera; odbija "../" iz ZIP-a ili renderera
function unutar(dir, rel) {
  const koren = path.resolve(dir);
  const p = path.resolve(koren, rel);
  if (p !== koren && !p.startsWith(koren + path.sep)) throw new Error('Nedozvoljena putanja: ' + rel);
  return p;
}

// CSV: indeks,ime,prezime,email (zarez ili tacka-zarez); prvi red se preskace ako je zaglavlje
function parsirajSpisak(tekst) {
  const redovi = tekst.split(/\r?\n/).map((r) => r.trim()).filter(Boolean);
  if (redovi.length && /indeks|email|ime/i.test(redovi[0])) redovi.shift();
  return redovi.map((r, i) => {
    const k = r.split(/[;,]/).map((x) => x.trim());
    if (k.length < 4) throw new Error(`Red ${i + 1} nema 4 kolone (indeks, ime, prezime, email): ${r}`);
    return { studentNumber: k[0], firstName: k[1], lastName: k[2], email: k[3] };
  });
}

module.exports = { unutar, parsirajSpisak };
