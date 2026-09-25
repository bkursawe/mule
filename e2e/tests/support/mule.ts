// Knowledge about the Mühle page that the tests share: fields, texts and how to start a game.
import { expect, type APIRequestContext, type Locator, type Page } from '@playwright/test';

export type Color = 'WHITE' | 'BLACK';
export type Strength = 'EASY' | 'MEDIUM' | 'HARD';

/** Field names in the order of the engine: row by row from the top. */
const FIELDS = [
  'a7', 'd7', 'g7',
  'b6', 'd6', 'f6',
  'c5', 'd5', 'e5',
  'a4', 'b4', 'c4', 'e4', 'f4', 'g4',
  'c3', 'd3', 'e3',
  'b2', 'd2', 'f2',
  'a1', 'd1', 'g1',
] as const;
export type Field = (typeof FIELDS)[number];

/** A position for the test API, fields in the usual notation. The computer plays with the other color. */
export interface Position {
  humanColor?: Color;
  activeColor?: Color;
  white?: Field[];
  black?: Field[];
  whiteStonesInHand?: number;
  blackStonesInHand?: number;
  movesWithoutCapture?: number;
}

export class MulePage {
  constructor(readonly page: Page, private readonly request: APIRequestContext) {}

  /** Opens the page for the first time: the app starts a new game with these settings. */
  async openNewGame({ humanColor = 'WHITE' as Color, strength = 'EASY' as Strength } = {}) {
    await this.page.addInitScript((settings) => {
      if (sessionStorage.getItem('e2e.opened')) return;
      sessionStorage.setItem('e2e.opened', '1');
      localStorage.setItem('mule.settings', JSON.stringify(settings));
    }, { humanColor, strength });
    await this.page.goto('/');
    await expect(this.status).not.toContainText('geladen');
  }

  /** Starts a game at a position through the test API and opens it; the computer plays easy. */
  async openPosition(position: Position) {
    const response = await this.request.post('/api/test/games', {
      data: {
        strength: 'EASY',
        ...position,
        white: (position.white ?? []).map(index),
        black: (position.black ?? []).map(index),
      },
    });
    expect(response.status(), await response.text()).toBe(201);
    const game = await response.json();
    await this.page.addInitScript((id) => {
      if (sessionStorage.getItem('e2e.opened')) return;
      sessionStorage.setItem('e2e.opened', '1');
      localStorage.setItem('mule.game', id);
    }, game.id);
    await this.page.goto('/');
    await expect(this.status).not.toContainText('geladen');
    return game;
  }

  /** A point on the board, named like "d6, frei" or "d6, weißer Stein". */
  field(name: Field): Locator {
    return this.page.getByRole('button', { name: new RegExp(`^${name}, `) });
  }

  async expectStone(name: Field, stone: 'frei' | 'weißer Stein' | 'schwarzer Stein') {
    await expect(this.field(name)).toHaveAccessibleName(`${name}, ${stone}`);
  }

  get status(): Locator {
    return this.page.getByRole('status');
  }

  player(name: 'Du' | 'Computer'): Locator {
    return this.page.getByRole('list', { name: 'Steine der Spieler' }).getByRole('listitem', { name: new RegExp(`^${name},`) });
  }

  get moveRounds(): Locator {
    return this.page.getByRole('region', { name: 'Züge' }).getByRole('listitem');
  }

  get newGameButton(): Locator {
    return this.page.getByRole('button', { name: 'Neues Spiel beginnen' });
  }

  /** Waits until the computer has answered and it is the human's turn again. */
  async expectHumanTurn() {
    await expect(this.status).toContainText('Du bist am Zug');
  }

  /** Holds requests to the URL until release() is called; arrived resolves when the first one comes in. */
  async holdRequests(url: string) {
    let arrive!: () => void;
    const arrived = new Promise<void>((resolve) => { arrive = resolve; });
    let release!: () => void;
    const released = new Promise<void>((resolve) => { release = resolve; });
    await this.page.route(url, async (route) => {
      arrive();
      await released;
      await route.continue();
    });
    return { arrived, release };
  }

  /** Counts the moves the page sends to the backend. */
  countMoveRequests(): { count: number } {
    const counter = { count: 0 };
    this.page.on('request', (request) => {
      if (request.method() === 'POST' && request.url().endsWith('/moves')) counter.count++;
    });
    return counter;
  }
}

function index(field: Field): number {
  return FIELDS.indexOf(field);
}
