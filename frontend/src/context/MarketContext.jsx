import { createContext, useContext, useEffect, useRef, useState, useSyncExternalStore } from 'react';
import { Client } from '@stomp/stompjs';
import { api } from '../lib/api';

/**
 * One STOMP connection shared by the whole app. Quotes are kept in an external store so that
 * a tick only re-renders components that subscribe to that symbol (useQuote), not the entire tree.
 */
const store = { quotes: new Map(), listeners: new Map(), all: new Set() };
function emit(symbol) {
  store.listeners.get(symbol)?.forEach((l) => l());
  store.all.forEach((l) => l());
}
function setQuote(q) { store.quotes.set(q.symbol, q); emit(q.symbol); }

export function useQuote(symbol) {
  return useSyncExternalStore(
    (cb) => { if (!store.listeners.has(symbol)) store.listeners.set(symbol, new Set()); store.listeners.get(symbol).add(cb); return () => store.listeners.get(symbol)?.delete(cb); },
    () => store.quotes.get(symbol),
  );
}
let version = 0;
export function useAllQuotesVersion() {
  return useSyncExternalStore((cb) => { const f = () => { version++; cb(); }; store.all.add(f); return () => store.all.delete(f); }, () => version);
}
export const getQuote = (s) => store.quotes.get(s);
export const allQuotes = () => Array.from(store.quotes.values());

const MarketCtx = createContext(null);

export function MarketProvider({ children }) {
  const [status, setStatus] = useState(null);
  const [connected, setConnected] = useState(false);
  const [userEvents, setUserEvents] = useState([]);
  const clientRef = useRef(null);
  const userSubRef = useRef(null);

  useEffect(() => {
    // initial snapshot over REST so the UI has prices before the socket connects
    api.get('/market/quotes').then(({ data }) => data.forEach(setQuote)).catch(() => {});
    api.get('/market/status').then(({ data }) => setStatus(data)).catch(() => {});
    const proto = window.location.protocol === 'https:' ? 'wss' : 'ws';
    const client = new Client({
      brokerURL: `${proto}://${window.location.host}/ws`,
      reconnectDelay: 3000,
      heartbeatIncoming: 10000, heartbeatOutgoing: 10000,
      onConnect: () => {
        setConnected(true);
        client.subscribe('/topic/quotes', (m) => { try { JSON.parse(m.body).forEach((q) => setQuote({ ...q, change: q.ltp - q.prevClose, changePct: q.prevClose ? ((q.ltp - q.prevClose) / q.prevClose) * 100 : 0 })); } catch { /* ignore */ } });
        client.subscribe('/topic/feed-status', (m) => { try { setStatus(JSON.parse(m.body)); } catch { /* ignore */ } });
      },
      onWebSocketClose: () => setConnected(false),
      onStompError: () => setConnected(false),
    });
    client.activate();
    clientRef.current = client;
    return () => { client.deactivate(); };
  }, []);

  /** Subscribe to the logged-in user's private topic (fills, alerts). */
  const watchUser = (userId, onEvent) => {
    const c = clientRef.current;
    if (!c || !userId) return () => {};
    let sub = null;
    const attach = () => {
      if (!c.connected) return;
      sub = c.subscribe(`/topic/user.${userId}`, (m) => { try { const ev = JSON.parse(m.body); setUserEvents((e) => [{ ...ev, at: new Date().toISOString() }, ...e].slice(0, 30)); onEvent?.(ev); } catch { /* ignore */ } });
      userSubRef.current = sub;
    };
    attach();
    const t = setInterval(() => { if (!sub && c.connected) attach(); }, 1000);
    return () => { clearInterval(t); sub?.unsubscribe(); };
  };

  return <MarketCtx.Provider value={{ status, connected, watchUser, userEvents, setUserEvents }}>{children}</MarketCtx.Provider>;
}
export const useMarket = () => useContext(MarketCtx);
