// The main flows of a game, each once from start to its visible result.
import { expect, test } from './support/fixtures';

test.describe('Spielbeginn', () => {
  test('zeigt ein leeres Brett und fordert zum Setzen auf', async ({ mule }) => {
    await mule.openNewGame();

    await expect(mule.status).toContainText('Du bist am Zug. Setze einen Stein auf einen freien Punkt.');
    await expect(mule.page.getByRole('button', { name: /, frei$/ })).toHaveCount(24);
    await expect(mule.player('Du')).toHaveAccessibleName('Du, Weiß: 9 Steine zu setzen');
    await expect(mule.player('Computer')).toHaveAccessibleName('Computer (Mittel), Schwarz: 9 Steine zu setzen');
    await expect(mule.page.getByText('Noch kein Zug gespielt.')).toBeVisible();
  });

  test('als Schwarz eröffnet der Computer', async ({ mule }) => {
    await mule.openNewGame();

    await mule.openNewGameDialog();
    await mule.page.getByRole('radio', { name: 'Schwarz' }).check();
    await mule.levelSlider.fill('1');
    await mule.newGameButton.click();

    await expect(mule.newGameDialog).toBeHidden();
    await mule.expectHumanTurn();
    await expect(mule.page.getByRole('button', { name: /, weißer Stein$/ })).toHaveCount(1);
    await expect(mule.player('Du')).toHaveAccessibleName(/^Du, Schwarz/);
    await expect(mule.player('Computer')).toHaveAccessibleName(/^Computer \(Anfänger\), Weiß/);
  });
});

test.describe('Neues Spiel', () => {
  test('alle Einstellungen und der Startknopf liegen im sichtbaren Bereich', async ({ mule }) => {
    await mule.openNewGame();

    await mule.openNewGameDialog();

    await expect(mule.page.getByRole('radio', { name: 'Weiß, du beginnst' })).toBeInViewport({ ratio: 1 });
    await expect(mule.levelSlider).toBeInViewport({ ratio: 1 });
    await expect(mule.newGameButton).toBeInViewport({ ratio: 1 });
  });

  test('der Schieberegler nennt die Stufe und merkt sie sich für das nächste Spiel', async ({ mule }) => {
    await mule.openNewGame();
    await mule.openNewGameDialog();

    await mule.levelSlider.fill('5');
    await expect(mule.levelSlider).toHaveAttribute('aria-valuetext', 'Meister');
    await expect(mule.newGameDialog).toContainText('verzeiht kaum einen Fehler');
    await mule.levelSlider.fill('1');
    await expect(mule.levelSlider).toHaveAttribute('aria-valuetext', 'Anfänger');
    await mule.newGameButton.click();
    await mule.expectHumanTurn();

    await mule.openNewGameDialog();
    await expect(mule.levelSlider).toHaveValue('1');
  });

  test('Abbrechen lässt das laufende Spiel stehen', async ({ mule }) => {
    await mule.openNewGame();
    await mule.field('d6').click();
    await mule.expectHumanTurn();

    await mule.openNewGameDialog();
    await mule.levelSlider.fill('4');
    await mule.page.getByRole('button', { name: 'Abbrechen' }).click();

    await expect(mule.newGameDialog).toBeHidden();
    await mule.expectStone('d6', 'weißer Stein');
    await expect(mule.player('Computer')).toHaveAccessibleName(/^Computer \(Mittel\)/);
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

    // The header with "Neues Spiel" comes first, then the board
    await mule.page.keyboard.press('Tab');
    await expect(mule.page.getByRole('button', { name: 'Neues Spiel', exact: true })).toBeFocused();
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
    await expect(mule.player('Computer')).toHaveAccessibleName('Computer (Mittel), Schwarz: 3 Steine, darf springen, 6 verloren');
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
    await mule.playAgainButton.click();
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

  test('nach 20 Zügen je Seite ohne Mühle ist das Spiel unentschieden', async ({ mule }) => {
    await mule.openPosition({
      white: ['a7', 'd7', 'g4', 'f2'],
      black: ['b6', 'd6', 'f6', 'd1'],
      movesWithoutCapture: 39,
    });

    await mule.field('f2').click();
    await mule.field('d2').click();

    await expect(mule.status).toContainText('Unentschieden.');
    await expect(mule.status).toContainText('Ihr habt beide 20 Züge lang keine Mühle geschlossen.');
  });

  test('ein neues Spiel beginnt mit leerem Brett', async ({ mule }) => {
    await mule.openNewGame();
    await mule.field('d6').click();
    await mule.expectHumanTurn();

    await mule.openNewGameDialog();
    await mule.newGameButton.click();

    await expect(mule.page.getByRole('button', { name: /, frei$/ })).toHaveCount(24);
    await expect(mule.page.getByText('Noch kein Zug gespielt.')).toBeVisible();
  });
});

test.describe('Züge bewerten', () => {
  test('vor dem Setzen ist der Punkt, der eine Mühle verhindert, sehr gut und jeder andere schlecht', async ({ mule }) => {
    // Black threatens to close a7-d7-g7; only a white stone on g7 stops it
    await mule.openPosition({
      white: ['d6', 'd2'], whiteStonesInHand: 7,
      black: ['a7', 'd7', 'f4'], blackStonesInHand: 6,
    });

    await mule.ratingSwitch.check();

    await expect(mule.field('g7')).toHaveAccessibleName('g7, frei, sehr gut');
    await expect(mule.field('a1')).toHaveAccessibleName('a1, frei, schlecht');
    await expect(mule.page.getByRole('list', { name: 'Farben der Bewertung' })).toBeVisible();
  });

  test('beim Ziehen zeigt die Bewertung erst den Stein und dann das Ziel', async ({ mule }) => {
    // White closes b6-d6-f6 with d7 to d6 before Black can block it with d5 to d6
    await mule.openPosition({ white: ['d7', 'b6', 'f6', 'a1', 'b4'], black: ['d5', 'e4', 'd3', 'g1', 'd1'] });
    await mule.ratingSwitch.check();

    await expect(mule.field('d7')).toHaveAccessibleName('d7, weißer Stein, sehr gut');
    await mule.field('d7').click();

    await expect(mule.field('d6')).toHaveAccessibleName('d6, frei, sehr gut');
  });
});

test.describe('Zug zurücknehmen', () => {
  test('dein Zug und die Antwort des Computers lassen sich zurücknehmen', async ({ mule }) => {
    await mule.openNewGame();
    await expect(mule.takeBackButton).toBeDisabled();
    await mule.field('d6').click();
    await mule.expectHumanTurn();

    await mule.takeBackButton.click();

    await expect(mule.page.getByRole('button', { name: /, frei$/ })).toHaveCount(24);
    await expect(mule.page.getByText('Noch kein Zug gespielt.')).toBeVisible();
    await expect(mule.status).toContainText('Du bist am Zug. Setze einen Stein auf einen freien Punkt.');
    await expect(mule.status).toContainText('Dein Zug und die Antwort des Computers sind zurückgenommen.');
    await expect(mule.player('Du')).toHaveAccessibleName('Du, Weiß: 9 Steine zu setzen');
    await expect(mule.takeBackButton).toBeDisabled();
  });

  test('nach einer Niederlage lässt sich der letzte Zug zurücknehmen und besser spielen', async ({ mule }) => {
    // Black threatens f4 to f2, closing b2-d2-f2; jumping to g1 loses, jumping to f2 blocks
    await mule.openPosition({ white: ['a7', 'd7', 'e3'], black: ['b2', 'd2', 'f4', 'c5'] });
    await mule.field('e3').click();
    await mule.field('g1').click();
    await expect(mule.status).toContainText('Der Computer hat gewonnen.');

    await mule.takeBackButton.click();

    await expect(mule.status).toContainText('Du hast nur noch drei Steine und darfst springen.');
    await mule.expectStone('e3', 'weißer Stein');
    await mule.expectStone('g1', 'frei');
    await mule.expectStone('f2', 'frei');
    await expect(mule.playAgainButton).toBeHidden();
    await mule.field('e3').click();
    await mule.field('f2').click();
    await mule.expectStone('f2', 'weißer Stein');
    await expect(mule.status).toContainText(/Der Computer (hat|ist) von [a-g][1-7] nach [a-g][1-7]/);
  });
});
