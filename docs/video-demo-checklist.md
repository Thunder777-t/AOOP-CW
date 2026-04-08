# 5-Minute Video Demo Checklist

Use this order to cover most FR/NFR points quickly.

## 1. Architecture (30-40s)

- Show `Model`, `SudokuController`, `SudokuView`, `SudokuCli`.
- State: GUI uses MVC, CLI directly reuses the same `Model`.

## 2. GUI Demo (2-2.5 min)

- Start GUI from `SudokuGuiMain`.
- Show 9x9 board and 3x3 box separators.
- Show fixed vs editable cells (different colors).
- Select cells by mouse and keyboard (`arrow`/`WASD`).
- Input numbers via physical keyboard and on-screen keypad.
- Trigger duplicate conflict to show validation highlight.
- Toggle `Validation Feedback` off and show highlight disappears.
- Use buttons: `Erase`, `Undo`, `Hint`, `Reset`, `New Game`.
- Toggle `Random Puzzle` off, set fixed puzzle index, click `New Game`.
- Complete (or almost complete) puzzle and show completion dialog.

## 3. CLI Demo (1.5-2 min)

- Start CLI from `SudokuCli`.
- Show commands: `set`, `clear`, `undo`, `hint`, `reset`, `new`.
- Show `check` and `flag` commands.
- Toggle `flag validation off` and `flag hint off`.
- Set `fixed` index + `new` to show fixed puzzle selection.

## 4. Tests + Assertions (40-50s)

- Open `ModelTest` and describe the 3 different scenarios.
- Show test run output (3 passed).
- Point out `assert`-based invariants/pre-post checks in `Model`.

## 5. Repository Evidence (20-30s)

- Show private repository URL and commit history.
- Highlight frequent commits across development timeline.
