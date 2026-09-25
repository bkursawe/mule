// Controls the game: talks to the backend, turns clicks into moves and updates the page.

import { api, ApiError } from './api.js';
import { BoardView, fieldName } from './board.js';

const GAME_KEY = 'mule.game';
const SETTINGS_KEY = 'mule.settings';
const COLOR_NAMES = { WHITE: 'Weiß', BLACK: 'Schwarz' };
const COMPUTER_DELAY = 450;

const dom = {
  board: document.getElementById('board'),
  status: document.getElementById('status'),
  detail: document.getElementById('detail'),
  human: document.getElementById('player-human'),
  computer: document.getElementById('player-computer'),
  moves: document.getElementById('moves'),
  noMoves: document.getElementById('no-moves'),
  newGame: document.getElementById('new-game'),
  form: document.getElementById('new-game-form'),
};

let game = null; // the game as the backend sent it
let busy = false; // a request is running
let selected = null; // field of the stone the human wants to move
let pending = null; // a move that closes a mule and still needs the stone to capture
let message = null; // texts that replace the status, e.g. after an error
let motion = null; // the move to animate with the next render

const view = new BoardView(dom.board, activate);

// ---------------------------------------------------------------- game flow

async function startGame(humanColor, strength) {
  await run(async () => {
    game = await api.startGame(humanColor, strength);
    selected = null;
    pending = null;
    motion = null;
    dom.newGame.open = false;
  });
  if (isComputerTurn()) await computerTurn();
}

async function playHumanMove(move, alreadyShown = false) {
  selected = null;
  pending = null;
  await run(async () => {
    game = await api.playMove(game.id, move);
    motion = { move, animateMove: !alreadyShown, glowMules: !alreadyShown };
  });
  if (isComputerTurn()) {
    await pause(COMPUTER_DELAY);
    await computerTurn();
  }
}

async function computerTurn() {
  await run(async () => {
    game = await api.computerMove(game.id);
    motion = { move: game.moves.at(-1), animateMove: true, glowMules: true };
  });
}

/** Runs a request: blocks the board meanwhile and shows errors as status. */
async function run(action) {
  busy = true;
  message = null;
  render();
  try {
    await action();
    save(GAME_KEY, game?.id);
  } catch (error) {
    message = errorTexts(error);
  } finally {
    busy = false;
    render();
  }
}

function pause(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

// ---------------------------------------------------------------- input

function activate(field) {
  if (!isHumanTurn()) return;
  const moves = game.legalMoves;

  if (pending) {
    const move = moves.find((m) => sameMove(m, pending) && m.capture === field);
    if (move) playHumanMove(move, true);
    return;
  }
  if (humanPlayer().phase === 'SETTING') {
    choose(moves.filter((m) => m.to === field));
    return;
  }
  if (selected !== null) {
    const options = moves.filter((m) => m.from === selected && m.to === field);
    if (options.length > 0) {
      choose(options);
      return;
    }
  }
  if (moves.some((m) => m.from === field)) {
    selected = selected === field ? null : field;
    render();
  }
}

// A move that closes a mule comes once per stone that may be captured
function choose(options) {
  if (options.length === 0) return;
  const [move] = options;
  if (move.capture == null) {
    playHumanMove(move);
    return;
  }
  pending = { type: move.type, color: move.color, from: move.from, to: move.to };
  selected = null;
  motion = { move: pending, animateMove: true, glowMules: true };
  render();
}

function sameMove(a, b) {
  return a.type === b.type && a.from === b.from && a.to === b.to;
}

document.addEventListener('keydown', (event) => {
  if (event.key !== 'Escape' || busy) return;
  if (pending || selected !== null) {
    pending = null;
    selected = null;
    render();
  }
});

dom.form.addEventListener('submit', (event) => {
  event.preventDefault();
  const settings = Object.fromEntries(new FormData(dom.form));
  save(SETTINGS_KEY, JSON.stringify(settings));
  startGame(settings.humanColor, settings.strength);
});

// ---------------------------------------------------------------- state helpers

function isOngoing() {
  return game?.result.status === 'ONGOING';
}

function isHumanTurn() {
  return !busy && isOngoing() && game.activeColor === game.humanColor;
}

function isComputerTurn() {
  return isOngoing() && game.activeColor !== game.humanColor;
}

function computerColor() {
  return game.humanColor === 'WHITE' ? 'BLACK' : 'WHITE';
}

function player(color) {
  return color === 'WHITE' ? game.white : game.black;
}

function humanPlayer() {
  return player(game.humanColor);
}

/** The board as the human sees it, with a mule-closing move that still waits for its capture. */
function shownBoard() {
  const board = [...game.board];
  if (pending) {
    if (pending.from != null) board[pending.from] = null;
    board[pending.to] = pending.color;
  }
  return board;
}

// ---------------------------------------------------------------- rendering

function render() {
  document.body.classList.toggle('thinking', busy && isComputerTurn());
  const { status, detail } = texts();
  dom.status.textContent = status;
  dom.detail.textContent = detail;
  dom.status.classList.toggle('result', Boolean(game) && !isOngoing() && !message);
  if (!game) return;

  renderBoard();
  renderPlayer(dom.human, game.humanColor, 'Du');
  renderPlayer(dom.computer, computerColor(), 'Computer');
  renderMoves();
  if (!isOngoing() && !busy) dom.newGame.open = true;
}

function renderBoard() {
  const board = shownBoard();
  const marks = new Map();
  const actionable = new Set();
  const mark = (field, name) => marks.set(field, [...(marks.get(field) ?? []), name]);

  const last = game.moves.at(-1);
  if (last && !pending) {
    if (last.from != null) mark(last.from, 'last-from');
    mark(last.to, 'last-to');
  }

  if (isHumanTurn()) {
    const moves = game.legalMoves;
    if (pending) {
      mark(pending.to, 'selected');
      for (const move of moves.filter((m) => sameMove(m, pending))) {
        mark(move.capture, 'capturable');
        actionable.add(move.capture);
      }
    } else if (humanPlayer().phase === 'SETTING') {
      moves.forEach((m) => actionable.add(m.to));
    } else {
      for (const move of moves) {
        actionable.add(move.from);
        if (selected === null) mark(move.from, 'movable');
      }
      if (selected !== null) {
        mark(selected, 'selected');
        for (const move of moves.filter((m) => m.from === selected)) {
          mark(move.to, 'target');
          actionable.add(move.to);
        }
      }
    }
  }

  const labels = board.map((color, field) => {
    const stone = color === null ? 'frei' : `${color === 'WHITE' ? 'weißer' : 'schwarzer'} Stein`;
    return `${fieldName(field)}, ${stone}`;
  });

  view.render({ board, marks, labels, actionable, motion });
  motion = null;
}

function renderPlayer(item, color, name) {
  const own = player(color);
  const opponent = player(color === 'WHITE' ? 'BLACK' : 'WHITE');
  const note = [phaseNote(own), own.stonesLost > 0 ? `${own.stonesLost} verloren` : null]
    .filter(Boolean)
    .join(', ');

  item.classList.toggle('active', isOngoing() && game.activeColor === color);
  item.setAttribute('aria-label', `${name}, ${COLOR_NAMES[color]}: ${note}`);
  item.innerHTML = `
    <span class="token ${color.toLowerCase()}" aria-hidden="true"></span>
    <span class="player-text" aria-hidden="true">
      <span class="player-name">${name}</span>
      <span class="player-note">${note}</span>
    </span>
    <span class="pebbles" aria-hidden="true">
      ${pebbles(own.stonesInHand, color, 'hand')}
      ${pebbles(opponent.stonesLost, opponent.color, 'captured')}
    </span>`;
}

function phaseNote(own) {
  switch (own.phase) {
    case 'SETTING': return own.stonesInHand === 1 ? '1 Stein zu setzen' : `${own.stonesInHand} Steine zu setzen`;
    case 'MOVING': return `${own.stonesOnBoard} Steine auf dem Brett`;
    case 'JUMPING': return '3 Steine, darf springen';
    default: return 'nur noch 2 Steine';
  }
}

function pebbles(count, color, kind) {
  const pebble = `<span class="pebble ${color.toLowerCase()}"></span>`;
  return `<span class="${kind}">${pebble.repeat(count)}</span>`;
}

function renderMoves() {
  const rounds = [];
  game.moves.forEach((move, index) => {
    if (index % 2 === 0) rounds.push([]);
    rounds.at(-1).push(moveName(move));
  });
  dom.moves.innerHTML = rounds
    .map((round) => `<li>${round.map((name) => `<span class="move">${name}</span>`).join('')}</li>`)
    .join('');
  dom.noMoves.hidden = rounds.length > 0;
  dom.moves.lastElementChild?.lastElementChild?.classList.add('latest');
  dom.moves.scrollTop = dom.moves.scrollHeight;
  dom.moves.classList.toggle('overflowing', dom.moves.scrollHeight > dom.moves.clientHeight);
}

function moveName(move) {
  const target = move.from == null ? fieldName(move.to) : `${fieldName(move.from)}–${fieldName(move.to)}`;
  return move.capture == null ? target : `${target}×${fieldName(move.capture)}`;
}

// ---------------------------------------------------------------- texts

function texts() {
  if (message) return message;
  if (!game) return { status: 'Das Spiel wird geladen.', detail: '' };
  if (!isOngoing()) return resultTexts(game.result);
  if (game.activeColor !== game.humanColor) return { status: 'Der Computer überlegt …', detail: '' };
  if (pending) {
    return {
      status: 'Mühle! Nimm einen Stein des Computers.',
      detail: 'Wähle einen markierten Stein. Mit Esc nimmst du deinen Zug zurück.',
    };
  }
  const detail = lastComputerMoveText();
  switch (humanPlayer().phase) {
    case 'SETTING':
      return { status: 'Du bist am Zug. Setze einen Stein auf einen freien Punkt.', detail };
    case 'JUMPING':
      return selected === null
        ? { status: 'Du hast nur noch drei Steine und darfst springen. Wähle einen Stein.', detail }
        : { status: 'Wohin soll der Stein springen? Jeder freie Punkt geht.', detail };
    default:
      return selected === null
        ? { status: 'Du bist am Zug. Wähle einen Stein, den du ziehen willst.', detail }
        : { status: 'Wohin soll der Stein? Wähle einen markierten Punkt.', detail };
  }
}

function lastComputerMoveText() {
  const move = game.moves.at(-1);
  if (!move || move.color === game.humanColor) {
    return 'Drei Steine in einer Linie sind eine Mühle. Dann darfst du einen Stein des Computers wegnehmen.';
  }
  const where = move.type === 'SET'
    ? `Der Computer hat auf ${fieldName(move.to)} gesetzt.`
    : `Der Computer ${move.type === 'JUMP' ? 'ist' : 'hat'} von ${fieldName(move.from)} nach ${fieldName(move.to)} ` +
      `${move.type === 'JUMP' ? 'gesprungen' : 'gezogen'}.`;
  return move.capture == null
    ? where
    : `${where} Er hat eine Mühle geschlossen und deinen Stein auf ${fieldName(move.capture)} genommen.`;
}

function resultTexts({ status, winner, reason }) {
  const humanWon = winner === game.humanColor;
  const headline = status === 'REMIS'
    ? 'Unentschieden.'
    : humanWon ? 'Du hast gewonnen.' : 'Der Computer hat gewonnen.';
  const why = {
    TWO_STONES: humanWon ? 'Der Computer hat nur noch zwei Steine.' : 'Du hast nur noch zwei Steine.',
    BLOCKED: humanWon ? 'Der Computer kann keinen Stein mehr ziehen.' : 'Du kannst keinen Stein mehr ziehen.',
    REPETITION: 'Dieselbe Stellung ist dreimal vorgekommen.',
    NO_CAPTURE: 'Ihr habt beide 20 Züge lang keine Mühle geschlossen.',
  }[reason];
  return { status: headline, detail: why };
}

function errorTexts(error) {
  if (!(error instanceof ApiError)) {
    return { status: 'Der Server antwortet nicht.', detail: 'Prüfe, ob er läuft, und lade die Seite neu.' };
  }
  if (error.status === 404) {
    dom.newGame.open = true;
    return { status: 'Dieses Spiel gibt es nicht mehr.', detail: 'Beginne unten ein neues Spiel.' };
  }
  return { status: 'Der Zug wurde nicht angenommen.', detail: 'Lade die Seite neu, um den aktuellen Stand zu sehen.' };
}

// ---------------------------------------------------------------- storage and start

function save(key, value) {
  try {
    if (value) localStorage.setItem(key, value);
  } catch {
    // Without storage the game simply starts anew after a reload
  }
}

function load(key) {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

async function init() {
  let settings = { humanColor: 'WHITE', strength: 'MEDIUM' };
  try {
    settings = { ...settings, ...JSON.parse(load(SETTINGS_KEY) ?? '{}') };
  } catch {
    // Keep the defaults
  }
  for (const [name, value] of Object.entries(settings)) {
    const input = dom.form.querySelector(`input[name="${name}"][value="${value}"]`);
    if (input) input.checked = true;
  }

  const savedId = load(GAME_KEY);
  if (savedId) {
    try {
      game = await api.getGame(savedId);
    } catch (error) {
      if (!(error instanceof ApiError && error.status === 404)) {
        message = errorTexts(error);
        render();
        return;
      }
    }
  }
  if (game) {
    render();
    if (isComputerTurn()) await computerTurn();
  } else {
    await startGame(settings.humanColor, settings.strength);
  }
}

init();
