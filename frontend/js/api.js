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
  const data = await response.json().catch(() => null);
  if (!response.ok) throw new ApiError(response.status, data?.message ?? response.statusText);
  // Every route answers with a game. Anything else, like the page of a proxy, must not replace the game shown.
  if (!isGame(data)) throw new ApiError(response.status, 'The server did not send a game');
  return data;
}

function isGame(data) {
  return typeof data?.id === 'string' &&
    Array.isArray(data.board) && data.board.length === 24 &&
    Array.isArray(data.legalMoves) && Array.isArray(data.moves) &&
    Boolean(data.white && data.black) &&
    typeof data.result?.status === 'string';
}

export const api = {
  startGame: (humanColor, strength) => request('POST', '/api/games', { humanColor, strength }),
  getGame: (id) => request('GET', `/api/games/${encodeURIComponent(id)}`),
  playMove: (id, move) => request('POST', `/api/games/${encodeURIComponent(id)}/moves`, move),
  computerMove: (id) => request('POST', `/api/games/${encodeURIComponent(id)}/computer-move`),
};
