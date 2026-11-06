const test = require('node:test');
const assert = require('node:assert');
const path = require('node:path');
const { unutar, parsirajSpisak } = require('../lib');

test('unutar dozvoljava putanje u folderu i odbija izlazak iz njega', () => {
  const dir = path.resolve('radovi', '1');
  assert.strictEqual(unutar(dir, 'src/Main.java'), path.join(dir, 'src', 'Main.java'));
  assert.throws(() => unutar(dir, '../2/tudje.java'));
  assert.throws(() => unutar(dir, path.resolve('/etc/passwd')));
});

test('spisak iz CSV-a preskace zaglavlje i prima zarez ili tacka-zarez', () => {
  const s = parsirajSpisak('indeks,ime,prezime,email\nRN 1/2023, Ana, Anic, ana@raf.rs\r\n\nRN 2/2023;Marko;Markovic;marko@raf.rs\n');
  assert.deepStrictEqual(s, [
    { studentNumber: 'RN 1/2023', firstName: 'Ana', lastName: 'Anic', email: 'ana@raf.rs' },
    { studentNumber: 'RN 2/2023', firstName: 'Marko', lastName: 'Markovic', email: 'marko@raf.rs' },
  ]);
});

test('red bez sve cetiri kolone je greska sa brojem reda', () => {
  assert.throws(() => parsirajSpisak('RN 1/2023,Ana'), /Red 1/);
});
