// Client for the game API of the backend.

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

async function request(method, path, body, isValid = isGame) {
  const response = await fetch(path, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined,
  });
  const data = await response.json().catch(() => null);
  if (!response.ok) throw new ApiError(response.status, data?.message ?? response.statusText);
  // Anything else than the expected answer, like the page of a proxy, must not replace the game shown
  if (!isValid(data)) throw new ApiError(response.status, 'The server did not send what was asked for');
  return data;
}

function isGame(data) {
  return typeof data?.id === 'string' &&
    Array.isArray(data.board) && data.board.length === 24 &&
    Array.isArray(data.legalMoves) && Array.isArray(data.moves) &&
    Boolean(data.white && data.black) &&
    typeof data.result?.status === 'string';
}

function isRatings(data) {
  return Number.isInteger(data?.moveNumber) && Array.isArray(data.moves) &&
    data.moves.every((rated) => typeof rated?.move?.to === 'number' && typeof rated.rating === 'string');
}

export const api = {
  startGame: (humanColor, strength) => request('POST', '/api/games', { humanColor, strength }),
  getGame: (id) => request('GET', `/api/games/${encodeURIComponent(id)}`),
  playMove: (id, move) => request('POST', `/api/games/${encodeURIComponent(id)}/moves`, move),
  computerMove: (id) => request('POST', `/api/games/${encodeURIComponent(id)}/computer-move`),
  rateMoves: (id) => request('GET', `/api/games/${encodeURIComponent(id)}/ratings`, undefined, isRatings),
};
