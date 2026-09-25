// The stone slab: incised lines, drilled points and pebbles, drawn as SVG.
// Fields are numbered 0 to 23 row by row from the top, like in the engine.

const SVG = 'http://www.w3.org/2000/svg';
const ORIGIN = 100;
const STEP = 100;

// Column and row of every field on the 7 x 7 grid
const GRID = [
  [0, 0], [3, 0], [6, 0],
  [1, 1], [3, 1], [5, 1],
  [2, 2], [3, 2], [4, 2],
  [0, 3], [1, 3], [2, 3], [4, 3], [5, 3], [6, 3],
  [2, 4], [3, 4], [4, 4],
  [1, 5], [3, 5], [5, 5],
  [0, 6], [3, 6], [6, 6],
];

export const POINTS = GRID.map(([column, row]) => ({ x: ORIGIN + column * STEP, y: ORIGIN + row * STEP }));

export const MULES = [
  [0, 1, 2], [3, 4, 5], [6, 7, 8], [9, 10, 11], [12, 13, 14], [15, 16, 17], [18, 19, 20], [21, 22, 23],
  [0, 9, 21], [3, 10, 18], [6, 11, 15], [1, 4, 7], [16, 19, 22], [8, 12, 17], [5, 13, 20], [2, 14, 23],
];

/** The usual name of a field, a1 at the bottom left to g7 at the top right. */
export function fieldName(field) {
  const [column, row] = GRID[field];
  return 'abcdefg'[column] + (7 - row);
}

/** The mules that the stone on `field` completes on `board`. */
export function mulesThrough(board, field) {
  const color = board[field];
  return MULES.filter((mule) => mule.includes(field) && mule.every((f) => board[f] === color));
}

const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');

function element(name, attributes = {}, parent) {
  const node = document.createElementNS(SVG, name);
  for (const [key, value] of Object.entries(attributes)) node.setAttribute(key, value);
  if (parent) parent.append(node);
  return node;
}

function animate(node, keyframes, options) {
  if (reducedMotion.matches) return null;
  return node.animate(keyframes, { fill: 'backwards', ...options });
}

export class BoardView {
  /** `onActivate(field)` is called when a point is clicked or activated with the keyboard. */
  constructor(svg, onActivate) {
    this.svg = svg;
    this.onActivate = onActivate;
    this.drawSlab();
    this.holeLayer = element('g', { class: 'holes' }, svg);
    this.stoneLayer = element('g', { class: 'stones' }, svg);
    this.spots = POINTS.map((point, field) => this.createSpot(point, field));
    POINTS.forEach(({ x, y }) => element('circle', { class: 'hole', cx: x, cy: y, r: 9 }, this.holeLayer));
  }

  drawSlab() {
    const defs = element('defs', {}, this.svg);
    defs.innerHTML = `
      <filter id="slab-shadow" x="-10%" y="-10%" width="120%" height="125%">
        <feDropShadow dx="0" dy="16" stdDeviation="14" flood-color="#1c2419" flood-opacity="0.55"/>
      </filter>
      <filter id="grain" x="0" y="0" width="100%" height="100%">
        <feTurbulence type="fractalNoise" baseFrequency="0.85" numOctaves="3" seed="7" result="fine"/>
        <feColorMatrix in="fine" type="matrix" result="fine-tint"
          values="0 0 0 0 0.36  0 0 0 0 0.28  0 0 0 0 0.18  0 0 0 1.1 -0.42"/>
        <feTurbulence type="fractalNoise" baseFrequency="0.006 0.035" numOctaves="3" seed="3" result="beds"/>
        <feColorMatrix in="beds" type="matrix" result="beds-tint"
          values="0 0 0 0 0.52  0 0 0 0 0.36  0 0 0 0 0.2  0 0 0 1.6 -0.55"/>
        <feMerge result="texture"><feMergeNode in="beds-tint"/><feMergeNode in="fine-tint"/></feMerge>
        <feComposite in="texture" in2="SourceGraphic" operator="in"/>
      </filter>
      <filter id="stone-shadow" x="-50%" y="-50%" width="200%" height="200%">
        <feGaussianBlur stdDeviation="3.5"/>
      </filter>
      <!-- In user space: the bounding box of a straight mule line has no height or no width -->
      <filter id="glow" filterUnits="userSpaceOnUse" x="0" y="0" width="800" height="800">
        <feGaussianBlur stdDeviation="5" result="blur"/>
        <feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge>
      </filter>
      <radialGradient id="marble" cx="38%" cy="32%" r="75%">
        <stop offset="0" stop-color="#fffdf8"/>
        <stop offset="0.55" stop-color="#ece6da"/>
        <stop offset="1" stop-color="#bdb3a3"/>
      </radialGradient>
      <radialGradient id="basalt" cx="36%" cy="30%" r="78%">
        <stop offset="0" stop-color="#5a5752"/>
        <stop offset="0.5" stop-color="#2d2b28"/>
        <stop offset="1" stop-color="#151412"/>
      </radialGradient>
      <radialGradient id="drill" cx="60%" cy="62%" r="70%">
        <stop offset="0" stop-color="#8a7556"/>
        <stop offset="1" stop-color="#4f3f2b"/>
      </radialGradient>`;

    // A hewn slab: straight enough to read as a board, uneven enough to read as stone
    const outline = 'M 25 32 L 223 22 L 411 18 L 596 23 L 778 29 L 784 217 L 780 408 L 785 589 L 775 779 ' +
      'L 589 784 L 402 780 L 211 785 L 20 776 L 16 589 L 21 400 L 15 215 Z';
    element('path', { class: 'slab-body', d: outline, filter: 'url(#slab-shadow)' }, this.svg);
    element('path', { class: 'slab-grain', d: outline, filter: 'url(#grain)' }, this.svg);
    element('path', { class: 'slab-edge', d: outline }, this.svg);

    const lines = element('g', { class: 'lines' }, this.svg);
    const segments = MULES.flatMap(([a, b, c]) => [[a, b], [b, c]]);
    // Light falls from the top left: the lower edge of every incision catches it
    for (const [className, offset] of [['groove-light', 2.2], ['groove', 0]]) {
      for (const [a, b] of segments) {
        element('line', {
          class: className,
          x1: POINTS[a].x + offset, y1: POINTS[a].y + offset,
          x2: POINTS[b].x + offset, y2: POINTS[b].y + offset,
        }, lines);
      }
    }

    this.muleLines = MULES.map((mule) => element('polyline', {
      class: 'mule-line',
      points: mule.map((f) => `${POINTS[f].x},${POINTS[f].y}`).join(' '),
      filter: 'url(#glow)',
    }, lines));

    const labels = element('g', { class: 'coordinates', 'aria-hidden': 'true' }, this.svg);
    for (let i = 0; i < 7; i++) {
      element('text', { x: ORIGIN + i * STEP, y: 768 }, labels).textContent = 'abcdefg'[i];
      element('text', { x: 44, y: ORIGIN + i * STEP + 7 }, labels).textContent = String(7 - i);
    }
  }

  createSpot({ x, y }, field) {
    const spot = element('g', { class: 'spot', role: 'button', 'data-field': field }, this.svg);
    element('circle', { class: 'spot-area', cx: x, cy: y, r: 46 }, spot);
    element('circle', { class: 'spot-ring', cx: x, cy: y, r: 41 }, spot);
    element('circle', { class: 'spot-dot', cx: x, cy: y, r: 11 }, spot);
    spot.addEventListener('click', () => this.onActivate(field));
    spot.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        this.onActivate(field);
      }
    });
    return spot;
  }

  /**
   * Draws the stones of `board` (24 entries: 'WHITE', 'BLACK' or null).
   * `marks` maps fields to hint classes, `labels` gives the accessible name of every field,
   * `actionable` tells which fields can be activated, `motion` describes the move to animate.
   */
  render({ board, marks = new Map(), labels, actionable = new Set(), motion = null }) {
    this.stoneLayer.replaceChildren();
    board.forEach((color, field) => {
      if (color) this.stoneLayer.append(this.createStone(color, field));
    });

    this.spots.forEach((spot, field) => {
      spot.setAttribute('class', ['spot', ...(marks.get(field) ?? [])].join(' '));
      spot.setAttribute('aria-label', labels[field]);
      const enabled = actionable.has(field);
      spot.setAttribute('tabindex', enabled ? '0' : '-1');
      spot.setAttribute('aria-disabled', String(!enabled));
    });

    if (motion) this.animateMotion(board, motion);
  }

  createStone(color, field) {
    const { x, y } = POINTS[field];
    const stone = element('g', {
      class: `stone ${color === 'WHITE' ? 'white' : 'black'}`,
      'data-field': field,
      transform: `translate(${x} ${y})`,
    });
    const body = element('g', { class: 'stone-body' }, stone);
    // Every pebble is a little different: the angle of its oval depends on its field
    const angle = (field * 47) % 180;
    element('ellipse', { class: 'stone-shadow', cx: 3, cy: 8, rx: 31, ry: 28, filter: 'url(#stone-shadow)' }, body);
    element('ellipse', {
      class: 'stone-face', rx: 32, ry: 29.5, transform: `rotate(${angle})`,
      fill: color === 'WHITE' ? 'url(#marble)' : 'url(#basalt)',
    }, body);
    element('ellipse', { class: 'stone-gloss', cx: -10, cy: -12, rx: 11, ry: 6, transform: 'rotate(-32 -10 -12)' }, body);
    return stone;
  }

  animateMotion(board, { move, animateMove, glowMules }) {
    const stone = this.stoneLayer.querySelector(`[data-field="${move.to}"] .stone-body`);
    let arrival = 0;
    if (stone && animateMove) {
      if (move.type === 'SET') {
        animate(stone, [
          { transform: 'translateY(-16px) scale(1.12)', opacity: 0 },
          { transform: 'none', opacity: 1 },
        ], { duration: 260, easing: 'cubic-bezier(.2,.8,.3,1)' });
        arrival = 200;
      } else {
        const dx = POINTS[move.from].x - POINTS[move.to].x;
        const dy = POINTS[move.from].y - POINTS[move.to].y;
        const jump = move.type === 'JUMP';
        animate(stone, jump
          ? [
            { transform: `translate(${dx}px, ${dy}px)` },
            { transform: `translate(${dx / 2}px, ${dy / 2}px) scale(1.18)`, offset: 0.5 },
            { transform: 'none' },
          ]
          : [{ transform: `translate(${dx}px, ${dy}px)` }, { transform: 'none' }],
        { duration: jump ? 460 : 320, easing: 'cubic-bezier(.3,.7,.3,1)' });
        arrival = jump ? 420 : 280;
      }
    }

    if (glowMules) {
      for (const mule of mulesThrough(board, move.to)) {
        const line = this.muleLines[MULES.indexOf(mule)];
        animate(line, [
          { opacity: 0 }, { opacity: 1, offset: 0.2 }, { opacity: 1, offset: 0.65 }, { opacity: 0 },
        ], { duration: 1700, delay: arrival, easing: 'ease-in-out', fill: 'none' });
      }
    }

    if (move.capture != null) {
      const taken = this.createStone(move.color === 'WHITE' ? 'BLACK' : 'WHITE', move.capture);
      taken.classList.add('taken');
      this.stoneLayer.append(taken);
      const fade = animate(taken.querySelector('.stone-body'), [
        { opacity: 1, transform: 'none' },
        { opacity: 0, transform: 'translateY(-10px) scale(0.7)' },
      ], { duration: 420, delay: arrival + 450, easing: 'ease-in', fill: 'forwards' });
      if (fade) fade.finished.then(() => taken.remove(), () => taken.remove());
      else taken.remove();
    }
  }
}
