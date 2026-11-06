// REST API klijent; adresa se menja sa VITE_API_URL (npr. u .env.local)
export const API = (import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1').replace(/\/$/, '');

const TOKEN = 'raf-lms-token';

export function sacuvaniToken() {
  try { return sessionStorage.getItem(TOKEN); } catch { return null; }
}

export function zapamtiToken(t) {
  try { t ? sessionStorage.setItem(TOKEN, t) : sessionStorage.removeItem(TOKEN); } catch { /* bez cuvanja u privatnom rezimu */ }
}

export class Neprijavljen extends Error {}

export async function api(putanja, { token, method = 'GET', body } = {}) {
  const headers = { Accept: 'application/json' };
  if (token) headers.Authorization = 'Bearer ' + token;
  if (body) headers['Content-Type'] = 'application/json';
  let res;
  try {
    res = await fetch(API + putanja, { method, headers, body: body && JSON.stringify(body) });
  } catch {
    throw new Error('Server nije dostupan');
  }
  const data = res.status === 204 ? null : await res.json().catch(() => null);
  if (res.status === 401) throw new Neprijavljen(data?.detail || 'Prijava je istekla');
  if (!res.ok) throw new Error(data?.detail || `Greska servera (${res.status})`);
  return data;
}
