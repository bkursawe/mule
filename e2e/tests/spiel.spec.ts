// The main flows of a game, each once from start to its visible result.
import { expect, test } from './support/fixtures';

test.describe('Spielbeginn', () => {
  test('zeigt ein leeres Brett und fordert zum Setzen auf', async ({ mule }) => {
    await mule.openNewGame();

    await expect(mule.status).toContainText('Du bist am Zug. Setze einen Stein auf einen freien Punkt.');
    await expect(mule.page.getByRole('button', { name: /, frei$/ })).toHaveCount(24);
    await expect(mule.player('Du')).toHaveAccessibleName('Du, Weiß: 9 Steine zu setzen');
    await expect(mule.player('Computer')).toHaveAccessibleName('Computer, Schwarz: 9 Steine zu setzen');
    await expect(mule.page.getByText('Noch kein Zug gespielt.')).toBeVisible();
  });

  test('als Schwarz eröffnet der Computer', async ({ mule }) => {
    await mule.openNewGame();

    await mule.page.getByText('Neues Spiel', { exact: true }).click();
    await mule.page.getByRole('radio', { name: 'Schwarz' }).check();
    await mule.page.getByRole('radio', { name: 'locker' }).check();
    await mule.newGameButton.click();

    await mule.expectHumanTurn();
    await expect(mule.page.getByRole('button', { name: /, weißer Stein$/ })).toHaveCount(1);
    await expect(mule.player('Du')).toHaveAccessibleName(/^Du, Schwarz/);
  });
});

test.describe('Setzen', () => {
  test('ein gesetzter Stein bleibt liegen und der Computer antwortet', async ({ mule }) => {
    await mule.openNewGame();

    await mule.field('d6').click();

    await mule.expectStone('d6', 'weißer Stein');
    await expect(mule.status).toContainText(/Der Computer hat auf [a-g][1-7] gesetzt\./);
    await expect(mule.page.getByRole('button', { name: /, schwarzer Stein$/ })).toHaveCount(1);
    await expect(mule.moveRounds).toHaveCount(1);
    await expect(mule.player('Du')).toHaveAccessibleName('Du, Weiß: 8 Steine zu setzen');
  });

  test('ein Zug lässt sich allein mit der Tastatur spielen', async ({ mule }) => {
    await mule.openNewGame();

    await mule.page.keyboard.press('Tab');
    await expect(mule.field('a7')).toBeFocused();
    await mule.page.keyboard.press('Enter');

    await mule.expectStone('a7', 'weißer Stein');
    await mule.expectHumanTurn();
  });

  test('nach dem Neuladen geht das Spiel weiter', async ({ mule }) => {
    await mule.openNewGame();
    await mule.field('d6').click();
    await mule.expectHumanTurn();

    await mule.page.reload();

    await mule.expectStone('d6', 'weißer Stein');
    await expect(mule.moveRounds).toHaveCount(1);
    await expect(mule.status).toContainText('Du bist am Zug');
  });
});

test.describe('Ziehen und Schlagen', () => {
  // White can push g4 to g7 and close the mule a7-d7-g7; the black stones b6, d6, f6 form a mule.
  // No stones in hand: the test API counts the stones missing from the board as lost.
  const position = {
    white: ['a7', 'd7', 'g4', 'f2'],
    black: ['b6', 'd6', 'f6', 'd1'],
  } as const;

  test('ein Stein zieht auf einen benachbarten freien Punkt', async ({ mule }) => {
    await mule.openPosition({ white: [...position.white], black: [...position.black] });
    await expect(mule.status).toContainText('Wähle einen Stein, den du ziehen willst.');

    await mule.field('f2').click();
    await expect(mule.status).toContainText('Wohin soll der Stein?');
    await mule.field('d2').click();

    await mule.expectStone('d2', 'weißer Stein');
    await mule.expectStone('f2', 'frei');
    await expect(mule.status).toContainText(/Der Computer hat von [a-g][1-7] nach [a-g][1-7] gezogen\./);
  });

  test('eine Mühle erlaubt, einen Stein des Computers zu nehmen', async ({ mule }) => {
    await mule.openPosition({ white: [...position.white], black: [...position.black] });

    await mule.field('g4').click();
    await mule.field('g7').click();
    await expect(mule.status).toContainText('Mühle! Nimm einen Stein des Computers.');
    await mule.field('d1').click();

    await mule.expectStone('g7', 'weißer Stein');
    await expect(mule.player('Computer')).toHaveAccessibleName('Computer, Schwarz: 3 Steine, darf springen, 6 verloren');
    await mule.expectHumanTurn();
  });

  test('schließt der Computer eine Mühle, nennt er den genommenen Stein', async ({ mule }) => {
    // Black closes b2-d2-f2 with f4 to f2; White cannot reach f2
    await mule.openPosition({ white: ['a7', 'd7', 'c5', 'e5'], black: ['b2', 'd2', 'f4', 'b4'] });

    await mule.field('c5').click();
    await mule.field('c4').click();

    await expect(mule.status).toContainText(
      /Der Computer hat von f4 nach f2 gezogen\. Er hat eine Mühle geschlossen und deinen Stein auf [a-g][1-7] genommen\./);
    await expect(mule.player('Du')).toHaveAccessibleName('Du, Weiß: 3 Steine, darf springen, 6 verloren');
  });

  test('mit drei Steinen springt ein Stein auf jeden freien Punkt', async ({ mule }) => {
    await mule.openPosition({ white: ['a7', 'd7', 'e3'], black: ['b6', 'd6', 'f6', 'b2', 'd2'] });
    await expect(mule.status).toContainText('Du hast nur noch drei Steine und darfst springen.');

    await mule.field('e3').click();
    await mule.field('g1').click();

    await mule.expectStone('g1', 'weißer Stein');
    await mule.expectStone('e3', 'frei');
    await expect(mule.status).toContainText(/Der Computer hat von [a-g][1-7] nach [a-g][1-7] gezogen\./);
    await expect(mule.status).toContainText('Du hast nur noch drei Steine und darfst springen.');
  });
});

test.describe('Spielende', () => {
  test('der Mensch gewinnt, wenn der Computer nur noch zwei Steine hat', async ({ mule }) => {
    // All black stones stand in a mule, so each of them may be taken
    await mule.openPosition({ white: ['a7', 'd7', 'g4', 'f2'], black: ['b6', 'd6', 'f6'] });

    await mule.field('g4').click();
    await mule.field('g7').click();
    await mule.field('d6').click();

    await expect(mule.status).toContainText('Du hast gewonnen.');
    await expect(mule.status).toContainText('Der Computer hat nur noch zwei Steine.');
    await expect(mule.newGameButton).toBeVisible();
  });

  test('der Computer gewinnt, wenn er eine Mühle schließt und nur zwei Steine übrig lässt', async ({ mule }) => {
    // Black threatens f4 to f2, closing b2-d2-f2; White jumps elsewhere
    await mule.openPosition({ white: ['a7', 'd7', 'e3'], black: ['b2', 'd2', 'f4', 'c5'] });

    await mule.field('e3').click();
    await mule.field('g1').click();

    await expect(mule.status).toContainText('Der Computer hat gewonnen.');
    await expect(mule.status).toContainText('Du hast nur noch zwei Steine.');
  });

  test('nach 50 Zügen ohne Schlagen ist das Spiel unentschieden', async ({ mule }) => {
    await mule.openPosition({
      white: ['a7', 'd7', 'g4', 'f2'],
      black: ['b6', 'd6', 'f6', 'd1'],
      movesWithoutCapture: 49,
    });

    await mule.field('f2').click();
    await mule.field('d2').click();

    await expect(mule.status).toContainText('Unentschieden.');
    await expect(mule.status).toContainText('50 Züge lang hat niemand einen Stein geschlagen.');
  });

  test('ein neues Spiel beginnt mit leerem Brett', async ({ mule }) => {
    await mule.openNewGame();
    await mule.field('d6').click();
    await mule.expectHumanTurn();

    await mule.page.getByText('Neues Spiel', { exact: true }).click();
    await mule.newGameButton.click();

    await expect(mule.page.getByRole('button', { name: /, frei$/ })).toHaveCount(24);
    await expect(mule.page.getByText('Noch kein Zug gespielt.')).toBeVisible();
  });
});
