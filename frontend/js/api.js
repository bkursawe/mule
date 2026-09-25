// Client for the game API of the backend.

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

async function request(method, path, body) {
  const response = await fetch(path, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined,
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new ApiError(response.status, data.message ?? response.statusText);
  return data;
}

export const api = {
  startGame: (humanColor, strength) => request('POST', '/api/games', { humanColor, strength }),
  getGame: (id) => request('GET', `/api/games/${encodeURIComponent(id)}`),
  playMove: (id, move) => request('POST', `/api/games/${encodeURIComponent(id)}/moves`, move),
  computerMove: (id) => request('POST', `/api/games/${encodeURIComponent(id)}/computer-move`),
};
