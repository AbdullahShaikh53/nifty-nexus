import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api, setUnauthorizedHandler } from '../lib/api';

const AuthCtx = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    try { const { data } = await api.get('/auth/me'); setUser(data); } catch { setUser(null); } finally { setLoading(false); }
  }, []);

  useEffect(() => { refresh(); setUnauthorizedHandler(() => setUser(null)); }, [refresh]);

  const login = async (email, password) => { const { data } = await api.post('/auth/login', { email, password }); setUser(data); return data; };
  const register = async (name, email, password) => { const { data } = await api.post('/auth/register', { name, email, password }); setUser(data); return data; };
  const logout = async () => { try { await api.post('/auth/logout'); } finally { setUser(null); } };

  return <AuthCtx.Provider value={{ user, loading, login, register, logout, refresh, isAdmin: user?.role === 'ADMIN' }}>{children}</AuthCtx.Provider>;
}
export const useAuth = () => useContext(AuthCtx);
