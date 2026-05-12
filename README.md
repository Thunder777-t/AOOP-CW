# AOOP Coursework: Sudoku (GUI + CLI)

This project implements two Java versions of Sudoku that share the same `Model`:

- GUI version (`Swing`, MVC): `sudoku.gui.SudokuGuiMain`
- CLI version: `sudoku.cli.SudokuCli`

## Project Layout

- `src/sudoku/model/SudokuModel.java`: shared model interface depended on by GUI and CLI.
- `src/sudoku/model/Model.java`: concrete model implementation (rules, puzzle loading, validation, hint/undo/reset/new game, flags).
- `src/sudoku/gui/*`: GUI `View` + `Controller` + `Main`.
- `src/sudoku/cli/SudokuCli.java`: command-line program using the same model.
- `test/sudoku/model/ModelTest.java`: three JUnit scenarios focused on the model.
- `puzzles.txt`: puzzle source file.

## Compile

```powershell
javac -encoding UTF-8 -d out `
  src/sudoku/model/Model.java `
  src/sudoku/cli/SudokuCli.java `
  src/sudoku/gui/SudokuController.java `
  src/sudoku/gui/SudokuView.java `
  src/sudoku/gui/SudokuGuiMain.java
```

## Run CLI

```powershell
java -cp out sudoku.cli.SudokuCli puzzles.txt
```

## Run GUI

```powershell
java -cp out sudoku.gui.SudokuGuiMain puzzles.txt
```

## CLI Commands

- `show`
- `set <row> <col> <value>`
- `clear <row> <col>`
- `undo`
- `hint`
- `reset`
- `new`
- `status`
- `check`
- `flag`
- `flag <validation|hint|random> <on|off>`
- `fixed <index>`
- `help`
- `exit`


