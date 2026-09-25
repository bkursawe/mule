// Edge cases, derived from the states and error branches of frontend/js/app.js.
import AxeBuilder from '@axe-core/playwright';
import { expect, test } from './support/fixtures';

// White can push g4 to g7 and close the mule a7-d7-g7; the black stones b6, d6, f6 form a mule, d1 does not
const MULE_READY = { white: ['a7', 'd7', 'g4', 'f2'], black: ['b6', 'd6', 'f6', 'd1'] } as const;

test.describe('Mühle und Schlagen', () => {
  test('Esc nimmt den mühleschließenden Zug zurück', async ({ mule }) => {
    await mule.openPosition({ white: [...MULE_READY.white], black: [...MULE_READY.black] });
    await mule.field('g4').click();
    await mule.field('g7').click();
    await expect(mule.status).toContainText('Mühle!');

    await mule.page.keyboard.press('Escape');

    await mule.expectStone('g7', 'frei');
    await mule.expectStone('g4', 'weißer Stein');
    await expect(mule.status).toContainText('Wähle einen Stein, den du ziehen willst.');
  });

  test('Steine in einer Mühle des Computers lassen sich nicht nehmen, solange andere frei stehen', async ({ mule }) => {
    await mule.openPosition({ white: [...MULE_READY.white], black: [...MULE_READY.black] });
    const moves = mule.countMoveRequests();
    await mule.field('g4').click();
    await mule.field('g7').click();

    await expect(mule.field('d6')).toHaveAttribute('aria-disabled', 'true');
    await expect(mule.field('d1')).toHaveAttribute('aria-disabled', 'false');
    // Clicked anyway, as a mouse does on a disabled point
    await mule.field('d6').click({ force: true });

    await expect(mule.status).toContainText('Mühle!');
    await mule.expectStone('d6', 'schwarzer Stein');
    expect(moves.count).toBe(0);
  });
});

test.describe('Eingaben zur falschen Zeit', () => {
  test('während der Computer überlegt, nimmt das Brett keine Züge an', async ({ mule }) => {
    await mule.openNewGame();
    const computer = await mule.holdRequests('**/computer-move');
    const moves = mule.countMoveRequests();

    await mule.field('d6').click();
    await computer.arrived;
    await expect(mule.status).toContainText('Der Computer überlegt');
    await expect(mule.field('a7')).toHaveAttribute('aria-disabled', 'true');
    await mule.field('a7').click({ force: true });
    computer.release();

    await mule.expectHumanTurn();
    await mule.expectStone('a7', 'frei');
    expect(moves.count).toBe(1);
  });

  test('ein zweiter Klick, solange der eigene Zug unterwegs ist, setzt keinen weiteren Stein', async ({ mule }) => {
    await mule.openNewGame();
    const move = await mule.holdRequests('**/moves');
    const moves = mule.countMoveRequests();

    await mule.field('a7').click();
    await move.arrived;
    await expect(mule.field('g1')).toHaveAttribute('aria-disabled', 'true');
    await mule.field('g1').click({ force: true });
    move.release();

    await mule.expectHumanTurn();
    await mule.expectStone('a7', 'weißer Stein');
    await expect(mule.page.getByRole('button', { name: /, weißer Stein$/ })).toHaveCount(1);
    expect(moves.count).toBe(1);
  });

  test('nach Spielende sind alle Punkte gesperrt', async ({ mule }) => {
    // Black is to move and every black stone is surrounded
    await mule.openPosition({
      activeColor: 'BLACK',
      white: ['d6', 'g4', 'b4', 'a1'],
      black: ['a7', 'd7', 'g7', 'a4'],
    });
    const moves = mule.countMoveRequests();

    await expect(mule.status).toContainText('Du hast gewonnen.');
    await expect(mule.status).toContainText('Der Computer kann keinen Stein mehr ziehen.');
    await expect(mule.page.getByRole('button', { name: /^[a-g][1-7], / , disabled: false })).toHaveCount(0);
    await mule.field('d5').click({ force: true });
    expect(moves.count).toBe(0);
  });
});

test.describe('Server- und Netzwerkfehler', () => {
  test('ein Serverfehler beim Zug lässt das Brett unverändert und der Zug gelingt beim zweiten Versuch', async ({ mule }) => {
    await mule.openNewGame();
    await mule.page.route('**/moves', (route) => route.fulfill({ status: 500, json: { message: 'boom' } }));

    await mule.field('d6').click();

    await expect(mule.status).toContainText('Der Zug wurde nicht angenommen.');
    await mule.expectStone('d6', 'frei');

    await mule.page.unroute('**/moves');
    await mule.field('d6').click();
    await mule.expectStone('d6', 'weißer Stein');
    await mule.expectHumanTurn();
  });

  test('ohne Verbindung zum Server kommt ein Hinweis statt eines stillen Hängers', async ({ mule }) => {
    await mule.openNewGame();
    await mule.page.route('**/moves', (route) => route.abort('failed'));

    await mule.field('d6').click();

    await expect(mule.status).toContainText('Der Server antwortet nicht.');
    await mule.expectStone('d6', 'frei');
  });

  test('ist das Spiel auf dem Server verschwunden, lässt sich sofort ein neues beginnen', async ({ mule }) => {
    await mule.openNewGame();
    await mule.page.route('**/moves', (route) => route.fulfill({ status: 404, json: { message: 'No game' } }));

    await mule.field('d6').click();

    await expect(mule.status).toContainText('Dieses Spiel gibt es nicht mehr.');
    await mule.page.unroute('**/moves');
    await mule.newGameButton.click();
    await expect(mule.status).toContainText('Du bist am Zug. Setze einen Stein');
    await expect(mule.page.getByRole('button', { name: /, frei$/ })).toHaveCount(24);
  });

  // F-01: such an answer used to replace the game and freeze the board
  for (const [what, body] of [['ohne gültiges JSON', 'kein json'], ['mit JSON, aber ohne Spiel', '{"ok":true}']]) {
    test(`eine Antwort ${what} führt zu einer Fehlermeldung, und das Brett bleibt bedienbar`, async ({ mule }) => {
      const errors: Error[] = [];
      mule.page.on('pageerror', (error) => errors.push(error));
      await mule.openNewGame();
      await mule.page.route('**/moves', (route) => route.fulfill({ status: 200, contentType: 'application/json', body }));

      await mule.field('d6').click();

      await expect(mule.status).toContainText('Der Zug wurde nicht angenommen.');
      await mule.expectStone('d6', 'frei');
      await mule.page.unroute('**/moves');
      await mule.field('d6').click();
      await mule.expectStone('d6', 'weißer Stein');
      await mule.expectHumanTurn();
      expect(errors).toEqual([]);
    });
  }
});

test.describe('Browser', () => {
  test('eine unbekannte gespeicherte Spiel-ID startet still ein neues Spiel', async ({ mule }) => {
    await mule.page.addInitScript(() => localStorage.setItem('mule.game', 'gibt-es-nicht'));

    await mule.page.goto('/');

    await expect(mule.status).toContainText('Du bist am Zug. Setze einen Stein');
    await expect(mule.page.getByRole('button', { name: /, frei$/ })).toHaveCount(24);
  });

  test('ohne lokalen Speicher lässt sich trotzdem spielen', async ({ mule }) => {
    await mule.page.addInitScript(() => {
      Object.defineProperty(window, 'localStorage', {
        get() { throw new DOMException('Speicher gesperrt', 'SecurityError'); },
      });
    });

    await mule.page.goto('/');
    await expect(mule.status).toContainText('Du bist am Zug');
    await mule.field('d6').click();

    await mule.expectStone('d6', 'weißer Stein');
  });

  test('ein veralteter zweiter Tab kann keinen Zug auf einem besetzten Punkt spielen', async ({ mule, context }) => {
    await mule.openNewGame();
    const second = await context.newPage();
    await second.goto('/');
    await expect(second.getByRole('status')).toContainText('Du bist am Zug');

    await mule.field('d6').click();
    await mule.expectHumanTurn();
    await second.getByRole('button', { name: /^d6, frei$/ }).click();

    await expect(second.getByRole('status')).toContainText('Der Zug wurde nicht angenommen.');
    await expect(second.getByRole('status')).toContainText('Lade die Seite neu');
  });

  test('eine offene Schlag-Auswahl ist nach dem Neuladen verworfen', async ({ mule }) => {
    await mule.openPosition({ white: [...MULE_READY.white], black: [...MULE_READY.black] });
    await mule.field('g4').click();
    await mule.field('g7').click();
    await expect(mule.status).toContainText('Mühle!');

    await mule.page.reload();

    await mule.expectStone('g7', 'frei');
    await mule.expectStone('g4', 'weißer Stein');
    await expect(mule.status).toContainText('Wähle einen Stein, den du ziehen willst.');
  });
});

test.describe('Texte', () => {
  test('ein einzelner Stein in der Hand steht im Singular', async ({ mule }) => {
    await mule.openPosition({
      white: ['a7', 'g1'], whiteStonesInHand: 1,
      black: ['d6', 'f4', 'b2'], blackStonesInHand: 1,
    });

    await expect(mule.player('Du')).toHaveAccessibleName('Du, Weiß: 1 Stein zu setzen, 6 verloren');
  });

  test('ist der Mensch eingesperrt, nennt die Niederlage den Grund', async ({ mule }) => {
    // White is to move and every white stone is surrounded
    await mule.openPosition({ white: ['a7', 'd7', 'g7', 'a4'], black: ['d6', 'g4', 'b4', 'a1'] });

    await expect(mule.status).toContainText('Der Computer hat gewonnen.');
    await expect(mule.status).toContainText('Du kannst keinen Stein mehr ziehen.');
    await expect(mule.newGameButton).toBeVisible();
  });

  test('eine dreifache Wiederholung wird als Grund für das Unentschieden genannt', async ({ mule }) => {
    await mule.page.route('**/api/games/*', async (route) => {
      const response = await route.fetch();
      const game = await response.json();
      await route.fulfill({ response, json: { ...game, legalMoves: [], result: { status: 'REMIS', reason: 'REPETITION' } } });
    });
    const game = await mule.openPosition({ white: [...MULE_READY.white], black: [...MULE_READY.black] });
    expect(game.id).toBeTruthy();

    await expect(mule.status).toContainText('Unentschieden.');
    await expect(mule.status).toContainText('Dieselbe Stellung ist dreimal vorgekommen.');
  });
});

test.describe('Darstellung und Barrierefreiheit', () => {
  test('auf einem 375 px breiten Bildschirm passt alles ohne seitliches Scrollen', async ({ mule }) => {
    await mule.page.setViewportSize({ width: 375, height: 740 });
    await mule.openNewGame();

    const overflow = await mule.page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
    expect(overflow).toBeLessThanOrEqual(0);
    await expect(mule.field('g1')).toBeInViewport();
  });

  test('bei reduzierter Bewegung laufen nach einem Zug keine Animationen', async ({ mule }) => {
    await mule.page.emulateMedia({ reducedMotion: 'reduce' });
    await mule.openNewGame();

    await mule.field('d6').click();
    await mule.expectHumanTurn();

    const running = await mule.page.evaluate(() => document.getAnimations().length);
    expect(running).toBe(0);
  });

  test('die Startseite hat keine schweren Barrierefreiheitsfehler', async ({ mule }) => {
    await mule.openNewGame();

    const results = await new AxeBuilder({ page: mule.page }).analyze();
    const serious = results.violations.filter((v) => v.impact === 'serious' || v.impact === 'critical');

    expect(serious.map((v) => `${v.id}: ${v.help}`)).toEqual([]);
  });

  test('während der Schlag-Auswahl gibt es keine schweren Barrierefreiheitsfehler', async ({ mule }) => {
    await mule.openPosition({ white: [...MULE_READY.white], black: [...MULE_READY.black] });
    await mule.field('g4').click();
    await mule.field('g7').click();
    await expect(mule.status).toContainText('Mühle!');

    const results = await new AxeBuilder({ page: mule.page }).analyze();
    const serious = results.violations.filter((v) => v.impact === 'serious' || v.impact === 'critical');

    expect(serious.map((v) => `${v.id}: ${v.help}`)).toEqual([]);
  });
});
