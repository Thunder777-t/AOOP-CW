# AOOP Coursework: Sudoku (GUI + CLI)

This project implements two Java versions of Sudoku that share the same `Model`:

- GUI version (`Swing`, MVC): `sudoku.gui.SudokuGuiMain`
- CLI version: `sudoku.cli.SudokuCli`

## Project Layout

- `src/sudoku/model/Model.java`: shared game logic, puzzle loading, validation, hint/undo/reset/new game, flags.
- `src/sudoku/gui/*`: GUI `View` + `Controller` + `Main`.
- `src/sudoku/cli/SudokuCli.java`: command-line program using the same model.
- `test/sudoku/model/ModelTest.java`: three JUnit scenarios focused on the model.
- `docs/class-diagram.puml`: class diagram source (PlantUML).
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

## Run JUnit Tests

1. Download JUnit Console Standalone JAR (example):
```powershell
Invoke-WebRequest `
  -Uri "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar" `
  -OutFile "$env:TEMP/junit-platform-console-standalone-1.11.4.jar"
```

2. Compile tests:
```powershell
javac -encoding UTF-8 `
  -cp "out;$env:TEMP/junit-platform-console-standalone-1.11.4.jar" `
  -d out-test `
  test/sudoku/model/ModelTest.java
```

3. Execute tests:
```powershell
java -jar "$env:TEMP/junit-platform-console-standalone-1.11.4.jar" `
  --class-path "out;out-test" `
  --scan-class-path
```
