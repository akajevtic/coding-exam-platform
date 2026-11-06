// Jedini most izmedju renderera i glavnog procesa.
const { contextBridge, ipcRenderer } = require('electron');

const poziv = (ime) => (...args) => ipcRenderer.invoke(ime, ...args);

contextBridge.exposeInMainWorld('lms', {
  config: poziv('config'),
  login: poziv('login'),
  logout: poziv('logout'),
  api: poziv('api'),
  stablo: poziv('fs:stablo'),
  citaj: poziv('fs:citaj'),
  pisi: poziv('fs:pisi'),
  otvoriRad: poziv('student:otvori'),
  predaj: poziv('student:predaj'),
  otvoriPredato: poziv('student:otvoriPredato'),
  postaviZadatak: poziv('nastavnik:postaviZadatak'),
  ucitajSpisak: poziv('nastavnik:ucitajSpisak'),
  preuzmiSve: poziv('nastavnik:preuzmiSve'),
  otvoriRadZaPregled: poziv('nastavnik:otvoriRad'),
});
