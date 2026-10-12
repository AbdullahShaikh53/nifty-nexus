// Display helpers for the logged-in user's handle and trader ID.
export const handleOf = (u) => (u?.email ? '@' + u.email.split('@')[0] : '');
export const traderId = (u) => (u?.id ? `NN-${String(u.id).padStart(4, '0')}` : '—');
