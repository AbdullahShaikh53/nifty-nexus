import { useCallback, useEffect, useState } from 'react';
import { api, errorMessage } from './api';

/** GET helper with loading / error / reload, optional polling. */
export function useApi(url, { poll, deps = [] } = {}) {
  const [data, setData] = useState(null); const [error, setError] = useState(null); const [loading, setLoading] = useState(true);
  const load = useCallback(async (silent) => {
    if (!url) return;
    if (!silent) setLoading(true);
    try { const r = await api.get(url); setData(r.data); setError(null); } catch (e) { setError(errorMessage(e)); } finally { setLoading(false); }
  }, [url]);
  useEffect(() => { load(); if (!poll) return; const t = setInterval(() => load(true), poll); return () => clearInterval(t); }, [load, poll, ...deps]); // eslint-disable-line
  return { data, error, loading, reload: load, setData };
}
