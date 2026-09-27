// Controls the game: talks to the backend, turns clicks into moves and updates the page.

import { api, ApiError } from './api.js';
import { BoardView, fieldName } from './board.js';

const GAME_KEY = 'mule.game';
const SETTINGS_KEY = 'mule.settings';
const RATINGS_KEY = 'mule.ratings';
const COLOR_NAMES = { WHITE: 'Weiß', BLACK: 'Schwarz' };
const COMPUTER_DELAY = 450;

/** The stops of the slider, weakest first, with the names of the backend. */
const LEVELS = [
  { strength: 'BEGINNER', name: 'Anfänger', note: 'Schließt seine Mühlen, achtet aber nicht auf deine.' },
  { strength: 'EASY', name: 'Leicht', note: 'Denkt nur an seinen eigenen Zug und lässt dir oft eine Mühle offen.' },
  { strength: 'MEDIUM', name: 'Mittel', note: 'Sieht deinen nächsten Zug voraus und blockt offene Mühlen.' },
  { strength: 'HARD', name: 'Stark', note: 'Denkt eine Sekunde lang viele Züge voraus.' },
  { strength: 'MASTER', name: 'Meister', note: 'Denkt drei Sekunden lang und verzeiht kaum einen Fehler.' },
];
const DEFAULT_SETTINGS = { humanColor: 'WHITE', strength: 'EASY' };

// Ratings of the backend, best last; neutral moves get no extra words
const RATINGS = ['BAD', 'NEUTRAL', 'GOOD'];
const RATING_WORDS = { GOOD: 'sehr gut', BAD: 'schlecht' };

const dom = {
  board: document.getElementById('board'),
  status: document.getElementById('status'),
  detail: document.getElementById('detail'),
  human: document.getElementById('player-human'),
  computer: document.getElementById('player-computer'),
  moves: document.getElementById('moves'),
  noMoves: document.getElementById('no-moves'),
  playAgain: document.getElementById('play-again'),
  rateMoves: document.getElementById('rate-moves'),
  ratingNote: document.getElementById('rating-note'),
  ratingLegend: document.getElementById('rating-legend'),
  newGameOpen: document.getElementById('new-game-open'),
  newGame: document.getElementById('new-game'),
  form: document.getElementById('new-game-form'),
  cancel: document.getElementById('new-game-cancel'),
  level: document.getElementById('level'),
  levelName: document.getElementById('level-name'),
  levelNote: document.getElementById('level-note'),
  levelScale: document.querySelector('.level-scale'),
};

let game = null; // the game as the backend sent it
let busy = false; // a request is running
let selected = null; // field of the stone the human wants to move
let pending = null; // a move that closes a mule and still needs the stone to capture
let message = null; // texts that replace the status, e.g. after an error
let motion = null; // the move to animate with the next render

let ratingsOn = false; // the human wants the moves rated
let ratings = null; // the ratings of the position shown: { position, byMove }
let ratingsLoading = null; // the position whose ratings are on the way
let ratingsFailed = null; // the position whose ratings could not be loaded

const view = new BoardView(dom.board, activate);

// ---------------------------------------------------------------- game flow

async function startGame(humanColor, strength) {
  await run(async () => {
    game = await api.startGame(humanColor, strength);
    selected = null;
    pending = null;
    motion = null;
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

// ---------------------------------------------------------------- ratings

/** Identifies the position shown, so late ratings of an earlier position are dropped. */
function positionKey() {
  return `${game.id}:${game.moves.length}`;
}

/** Asks the backend once per position how good the moves of the human are. The board stays usable meanwhile. */
async function requestRatings() {
  if (!ratingsOn || !isHumanTurn()) return;
  const position = positionKey();
  if ([ratings?.position, ratingsLoading, ratingsFailed].includes(position)) return;
  ratingsLoading = position;
  renderRatingNote();
  try {
    const answer = await api.rateMoves(game.id);
    if (`${game.id}:${answer.moveNumber}` === position) {
      ratings = { position, byMove: new Map(answer.moves.map(({ move, rating }) => [moveKey(move), rating])) };
    }
  } catch {
    // Without ratings the game simply goes on
    ratingsFailed = position;
  } finally {
    if (ratingsLoading === position) ratingsLoading = null;
    render();
  }
}

/** The ratings of the moves in the position shown, or null. */
function shownRatings() {
  return ratingsOn && game && ratings?.position === positionKey() ? ratings.byMove : null;
}

/** The best rating of some moves: a field is as good as the best move that ends or starts there. */
function bestRating(byMove, moves) {
  const found = moves.map((move) => RATINGS.indexOf(byMove.get(moveKey(move)))).filter((index) => index >= 0);
  return found.length > 0 ? RATINGS[Math.max(...found)] : null;
}

function moveKey(move) {
  return [move.type, move.from ?? '', move.to, move.capture ?? ''].join(' ');
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
  // In the dialog Esc closes the dialog and leaves the board as it is
  if (event.key !== 'Escape' || busy || dom.newGame.open) return;
  if (pending || selected !== null) {
    pending = null;
    selected = null;
    render();
  }
});

dom.rateMoves.addEventListener('change', () => {
  ratingsOn = dom.rateMoves.checked;
  save(RATINGS_KEY, String(ratingsOn));
  render();
});

// ---------------------------------------------------------------- new game dialog

function openNewGame() {
  showSettings(loadSettings());
  if (!dom.newGame.open) dom.newGame.showModal();
}

dom.newGameOpen.addEventListener('click', openNewGame);
dom.playAgain.addEventListener('click', openNewGame);
dom.cancel.addEventListener('click', () => dom.newGame.close());
dom.level.addEventListener('input', renderLevel);

// A click on the backdrop closes the dialog. The form fills the dialog, so only the backdrop hits the dialog itself;
// a drag that starts on the slider and ends outside does not count.
let pressedOnBackdrop = false;
dom.newGame.addEventListener('pointerdown', (event) => {
  pressedOnBackdrop = event.target === dom.newGame;
});
dom.newGame.addEventListener('click', (event) => {
  if (pressedOnBackdrop && event.target === dom.newGame) dom.newGame.close();
});

dom.form.addEventListener('submit', (event) => {
  event.preventDefault();
  const data = new FormData(dom.form);
  const settings = { humanColor: data.get('humanColor'), strength: LEVELS[Number(data.get('level')) - 1].strength };
  save(SETTINGS_KEY, JSON.stringify(settings));
  dom.newGame.close();
  startGame(settings.humanColor, settings.strength);
});

function showSettings({ humanColor, strength }) {
  dom.form.elements.humanColor.value = humanColor;
  dom.level.value = String(levelIndex(strength) + 1);
  renderLevel();
}

function renderLevel() {
  const index = Number(dom.level.value) - 1;
  const { name, note } = LEVELS[index];
  dom.levelName.textContent = name;
  dom.levelNote.textContent = note;
  dom.level.setAttribute('aria-valuetext', name);
  [...dom.levelScale.children].forEach((stop, stopIndex) => stop.classList.toggle('current', stopIndex === index));
}

function levelIndex(strength) {
  const index = LEVELS.findIndex((level) => level.strength === strength);
  return index >= 0 ? index : levelIndex(DEFAULT_SETTINGS.strength);
}

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
  renderRatingNote();
  if (!game) return;

  renderBoard();
  renderPlayer(dom.human, game.humanColor, 'Du');
  renderPlayer(dom.computer, computerColor(), 'Computer', LEVELS.find((level) => level.strength === game.strength));
  renderMoves();
  dom.playAgain.hidden = isOngoing() || busy;
  requestRatings();
}

function renderBoard() {
  const board = shownBoard();
  const marks = new Map();
  const actionable = new Set();
  const notes = new Map();
  const mark = (field, name) => marks.set(field, [...(marks.get(field) ?? []), name]);
  const rated = shownRatings();
  const rate = (field, moves) => {
    const rating = rated && bestRating(rated, moves);
    if (!rating) return;
    mark(field, `rated-${rating.toLowerCase()}`);
    if (RATING_WORDS[rating]) notes.set(field, RATING_WORDS[rating]);
  };

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
        rate(move.capture, [move]);
      }
    } else if (humanPlayer().phase === 'SETTING') {
      for (const [field, options] of groupBy(moves, (m) => m.to)) {
        actionable.add(field);
        // Free points only get a dot when there is a rating to show
        if (rated) mark(field, 'target');
        rate(field, options);
      }
    } else {
      for (const [field, options] of groupBy(moves, (m) => m.from)) {
        actionable.add(field);
        if (selected === null) {
          mark(field, 'movable');
          rate(field, options);
        }
      }
      if (selected !== null) {
        mark(selected, 'selected');
        for (const [field, options] of groupBy(moves.filter((m) => m.from === selected), (m) => m.to)) {
          mark(field, 'target');
          actionable.add(field);
          rate(field, options);
        }
      }
    }
  }

  const labels = board.map((color, field) => {
    const stone = color === null ? 'frei' : `${color === 'WHITE' ? 'weißer' : 'schwarzer'} Stein`;
    const note = notes.get(field);
    return note ? `${fieldName(field)}, ${stone}, ${note}` : `${fieldName(field)}, ${stone}`;
  });

  view.render({ board, marks, labels, actionable, motion });
  motion = null;
}

function renderPlayer(item, color, name, level) {
  const own = player(color);
  const opponent = player(color === 'WHITE' ? 'BLACK' : 'WHITE');
  const note = [phaseNote(own), own.stonesLost > 0 ? `${own.stonesLost} verloren` : null]
    .filter(Boolean)
    .join(', ');
  const who = level ? `${name} (${level.name})` : name;

  item.classList.toggle('active', isOngoing() && game.activeColor === color);
  item.setAttribute('aria-label', `${who}, ${COLOR_NAMES[color]}: ${note}`);
  item.innerHTML = `
    <span class="token ${color.toLowerCase()}" aria-hidden="true"></span>
    <span class="player-text" aria-hidden="true">
      <span class="player-name">${name}</span>
      ${level ? `<span class="player-level">${level.name}</span>` : ''}
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

function renderRatingNote() {
  dom.rateMoves.checked = ratingsOn;
  dom.ratingLegend.hidden = !ratingsOn;
  const position = game ? positionKey() : null;
  if (!ratingsOn) {
    dom.ratingNote.textContent = 'Zeigt vor deinem Zug, welche Züge gut und welche schlecht sind.';
  } else if (ratingsLoading) {
    dom.ratingNote.textContent = 'Deine Züge werden bewertet …';
  } else if (position && ratingsFailed === position) {
    dom.ratingNote.textContent = 'Die Bewertung ist gerade nicht verfügbar.';
  } else {
    dom.ratingNote.textContent = 'Die Punkte auf dem Brett zeigen, wie gut dein Zug dort wäre.';
  }
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
    openNewGame();
    return { status: 'Dieses Spiel gibt es nicht mehr.', detail: 'Beginne ein neues Spiel.' };
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

/** The settings of the last game, checked, so that settings of an older version still start a game. */
function loadSettings() {
  let saved = {};
  try {
    saved = JSON.parse(load(SETTINGS_KEY) ?? '{}') ?? {};
  } catch {
    // Keep the defaults
  }
  return {
    humanColor: Object.hasOwn(COLOR_NAMES, saved.humanColor) ? saved.humanColor : DEFAULT_SETTINGS.humanColor,
    strength: LEVELS[levelIndex(saved.strength)].strength,
  };
}

function groupBy(items, keyOf) {
  const groups = new Map();
  for (const item of items) groups.set(keyOf(item), [...(groups.get(keyOf(item)) ?? []), item]);
  return groups;
}

async function init() {
  const settings = loadSettings();
  dom.levelScale.innerHTML = LEVELS.map((level, index) => `<li style="--stop: ${index}">${level.name}</li>`).join('');
  showSettings(settings);
  ratingsOn = load(RATINGS_KEY) === 'true';

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
