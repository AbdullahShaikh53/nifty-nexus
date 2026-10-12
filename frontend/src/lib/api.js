import axios from 'axios';

/** Axios instance. Same-origin via Vite proxy so the HttpOnly JWT cookie flows automatically. */
export const api = axios.create({ baseURL: '/api', withCredentials: true, timeout: 15000 });

export function errorMessage(err) {
  if (!err) return 'Unknown error';
  if (err.response?.data?.message) return err.response.data.message;
  // Vite's dev proxy answers 500 with an empty body when nothing is listening on :8080
  if (err.response?.status >= 500 && !err.response?.data?.status) return 'Backend server is not running or still starting (port 8080). Check the backend terminal.';
  if (err.code === 'ERR_NETWORK') return 'Cannot reach the Nifty Nexus server. Is the backend running on port 8080?';
  return err.message || 'Request failed';
}

let onUnauthorized = null;
export const setUnauthorizedHandler = (fn) => { onUnauthorized = fn; };
api.interceptors.response.use((r) => r, (err) => {
  if (err.response?.status === 401 && onUnauthorized && !err.config?.url?.startsWith('/auth/')) onUnauthorized();
  return Promise.reject(err);
});
