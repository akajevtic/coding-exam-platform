import { useEffect, useState } from 'react';
import { api, Neprijavljen, sacuvaniToken, zapamtiToken } from './api.js';

const OSVEZAVANJE_MS = 5000;

const vreme = (iso) => (iso ? new Date(iso).toLocaleTimeString('sr-RS', { hour: '2-digit', minute: '2-digit' }) : '—');
const trajanje = (s) => (s == null ? '—' : `${Math.floor(s / 60)} min ${Math.round(s % 60)} s`);
const STATUS = { NIJE_ZAPOCETO: 'nije zapoceo', U_IZRADI: 'u izradi', PREDATO: 'predat rad' };

export default function App() {
  const [token, setToken] = useState(sacuvaniToken);
  const odjava = () => { zapamtiToken(null); setToken(null); };
  if (!token) return <Prijava onPrijava={(t) => { zapamtiToken(t); setToken(t); }} />;
  return <Monitoring token={token} onOdjava={odjava} />;
}

function Prijava({ onPrijava }) {
  const [greska, setGreska] = useState('');
  const [salje, setSalje] = useState(false);

  async function posalji(e) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    setSalje(true);
    setGreska('');
    try {
      const d = await api('/auth/login', { method: 'POST', body: { username: f.get('email'), password: f.get('lozinka') } });
      if (d.role !== 'NASTAVNIK') throw new Error('Monitoring je samo za nastavnike');
      onPrijava(d.accessToken);
    } catch (err) {
      setGreska(err.message);
    } finally {
      setSalje(false);
    }
  }

  return (
    <main className="prijava">
      <form className="kartica" onSubmit={posalji}>
        <h1>RAF LMS monitoring</h1>
        <label>Email<input name="email" type="email" autoComplete="username" required autoFocus /></label>
        <label>Lozinka<input name="lozinka" type="password" autoComplete="current-password" required /></label>
        {greska && <p className="greska" role="alert">{greska}</p>}
        <button disabled={salje}>{salje ? 'Prijava...' : 'Prijavi se'}</button>
      </form>
    </main>
  );
}

function Monitoring({ token, onOdjava }) {
  const [termini, setTermini] = useState(null);
  const [terminId, setTerminId] = useState('');
  const [podaci, setPodaci] = useState(null);
  const [greska, setGreska] = useState('');
  const [osvezeno, setOsvezeno] = useState(null);

  // termini: najnoviji prvi; podrazumevano termin koji je u toku, inace prvi sa liste
  useEffect(() => {
    api('/teacher/terms', { token })
      .then((t) => {
        setTermini(t);
        const sada = Date.now();
        const uToku = t.find((x) => Date.parse(x.startsAt) <= sada && sada <= Date.parse(x.endsAt));
        setTerminId(String((uToku || t[0])?.id ?? ''));
      })
      .catch((e) => (e instanceof Neprijavljen ? onOdjava() : setGreska(e.message)));
  }, [token]);

  // osvezavanje bez ponovnog ucitavanja stranice: novi zahtev na svakih 5 s dok je termin izabran
  useEffect(() => {
    if (!terminId) return undefined;
    let aktivno = true;
    const ucitaj = () => api(`/teacher/terms/${terminId}/monitoring`, { token })
      .then((d) => { if (aktivno) { setPodaci(d); setGreska(''); setOsvezeno(new Date()); } })
      .catch((e) => { if (!aktivno) return; if (e instanceof Neprijavljen) onOdjava(); else setGreska(e.message); });
    setPodaci(null);
    ucitaj();
    const t = setInterval(ucitaj, OSVEZAVANJE_MS);
    return () => { aktivno = false; clearInterval(t); };
  }, [terminId, token]);

  return (
    <div className="strana">
      <header>
        <h1>Monitoring</h1>
        <button className="sporedno" onClick={onOdjava}>Odjava</button>
      </header>

      {termini && (termini.length ? (
        <label className="izbor">Termin
          <select value={terminId} onChange={(e) => setTerminId(e.target.value)}>
            {termini.map((t) => (
              <option key={t.id} value={t.id}>
                {t.subject.name} · {t.examPeriod.name} · {new Date(t.startsAt).toLocaleString('sr-RS')}{t.room ? ` · ${t.room}` : ''}
              </option>
            ))}
          </select>
        </label>
      ) : <p className="sivo">Nema termina.</p>)}

      {greska && <p className="greska" role="alert">{greska}</p>}

      {podaci && (
        <>
          <section className="brojevi" aria-label="Pregled">
            <Broj naziv="Na spisku" vrednost={podaci.registeredCount} />
            <Broj naziv="Preuzeto" vrednost={podaci.downloadedCount} />
            <Broj naziv="Predato" vrednost={podaci.submittedCount} />
            <Broj naziv="Prosecno vreme izrade" vrednost={trajanje(podaci.averageDurationSeconds)} />
          </section>
          <p className="sivo osvezeno" aria-live="polite">Osvezava se na {OSVEZAVANJE_MS / 1000} s · poslednje: {osvezeno?.toLocaleTimeString('sr-RS')}</p>

          <section className="studenti">
            <div className="red zaglavlje"><span>Student</span><span>Grupa</span><span>Status</span><span>Preuzeo</span><span>Predao</span><span>Vreme izrade</span></div>
            {podaci.students.map((s) => (
              <div className="red" key={s.studentNumber}>
                <span><strong>{s.studentNumber}</strong> {s.firstName} {s.lastName}</span>
                <span data-naziv="Grupa">{s.group || '—'}</span>
                <span data-naziv="Status"><span className={`oznaka ${s.status}`}>{STATUS[s.status] || s.status}</span></span>
                <span data-naziv="Preuzeo">{vreme(s.downloadedAt)}</span>
                <span data-naziv="Predao">{vreme(s.submittedAt)}</span>
                <span data-naziv="Vreme izrade">{trajanje(s.durationSeconds)}</span>
              </div>
            ))}
            {!podaci.students.length && <p className="sivo">Nema studenata na spisku za ovaj termin.</p>}
          </section>
        </>
      )}
    </div>
  );
}

function Broj({ naziv, vrednost }) {
  return (
    <div className="broj">
      <span className="vrednost">{vrednost}</span>
      <span className="naziv">{naziv}</span>
    </div>
  );
}
