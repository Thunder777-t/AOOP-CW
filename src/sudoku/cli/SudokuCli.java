package sudoku.cli;

import sudoku.model.Model;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public final class SudokuCli {
    private final Model model;
    private final Scanner scanner;

    private SudokuCli(Model model) {
        this.model = model;
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        Path puzzlePath = args.length > 0 ? Paths.get(args[0]) : Paths.get("puzzles.txt");
        try {
            Model model = new Model(puzzlePath);
            SudokuCli cli = new SudokuCli(model);
            cli.run();
        } catch (IOException e) {
            System.err.println("Failed to read puzzles file: " + puzzlePath);
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (RuntimeException e) {
            System.err.println("Failed to start CLI: " + e.getMessage());
            System.exit(1);
        }
    }

    private void run() {
        System.out.println("Sudoku CLI");
        System.out.println("Type 'help' to see commands.");
        printBoard();
        while (true) {
            if (model.isBoardCompleted()) {
                System.out.println("Puzzle completed! Congratulations.");
            }
            System.out.print("> ");
            if (!scanner.hasNextLine()) {
                System.out.println();
                return;
            }
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            if (!handleCommand(line)) {
                return;
            }
        }
    }

    private boolean handleCommand(String input) {
        String[] tokens = input.split("\\s+");
        String command = tokens[0].toLowerCase();

        switch (command) {
            case "help":
            case "h":
                printHelp();
                return true;
            case "show":
                printBoard();
                return true;
            case "set":
                return handleSet(tokens);
            case "clear":
                return handleClear(tokens);
            case "undo":
                return handleUndo(tokens);
            case "hint":
                return handleHint(tokens);
            case "reset":
                return handleReset(tokens);
            case "new":
                return handleNew(tokens);
            case "status":
                return handleStatus(tokens);
            case "quit":
            case "exit":
                return handleExit(tokens);
            default:
                System.out.println("Unknown command. Type 'help' for valid commands.");
                return true;
        }
    }

    private boolean handleSet(String[] tokens) {
        if (tokens.length != 4) {
            System.out.println("Usage: set <row 1-9> <col 1-9> <value 1-9>");
            return true;
        }
        Integer row = parseIndex(tokens[1]);
        Integer col = parseIndex(tokens[2]);
        Integer value = parseValue(tokens[3]);
        if (row == null || col == null || value == null) {
            return true;
        }
        boolean changed = model.setCellValue(row, col, value);
        if (!changed) {
            System.out.println("Move rejected. Cell may be fixed, out of range, or value is unchanged.");
            return true;
        }
        System.out.println("Cell (" + (row + 1) + ", " + (col + 1) + ") set to " + value + ".");
        onBoardMutation();
        return true;
    }

    private boolean handleClear(String[] tokens) {
        if (tokens.length != 3) {
            System.out.println("Usage: clear <row 1-9> <col 1-9>");
            return true;
        }
        Integer row = parseIndex(tokens[1]);
        Integer col = parseIndex(tokens[2]);
        if (row == null || col == null) {
            return true;
        }
        boolean changed = model.clearCell(row, col);
        if (!changed) {
            System.out.println("Clear rejected. Cell may be fixed, out of range, or already empty.");
            return true;
        }
        System.out.println("Cell (" + (row + 1) + ", " + (col + 1) + ") cleared.");
        onBoardMutation();
        return true;
    }

    private boolean handleUndo(String[] tokens) {
        if (tokens.length != 1) {
            System.out.println("Usage: undo");
            return true;
        }
        if (!model.undoLastAction()) {
            System.out.println("No action available to undo.");
            return true;
        }
        System.out.println("Last action has been undone.");
        onBoardMutation();
        return true;
    }

    private boolean handleHint(String[] tokens) {
        if (tokens.length != 1) {
            System.out.println("Usage: hint");
            return true;
        }
        Model.Hint hint = model.requestHint();
        if (hint == null) {
            if (!model.isHintEnabled()) {
                System.out.println("Hint is currently disabled.");
            } else {
                System.out.println("No empty editable cell is available for hint.");
            }
            return true;
        }
        System.out.println("Hint filled cell (" + (hint.getRow() + 1) + ", " + (hint.getCol() + 1) + ") with " + hint.getValue() + ".");
        onBoardMutation();
        return true;
    }

    private boolean handleReset(String[] tokens) {
        if (tokens.length != 1) {
            System.out.println("Usage: reset");
            return true;
        }
        model.reset();
        System.out.println("Puzzle reset to initial state.");
        printBoard();
        return true;
    }

    private boolean handleNew(String[] tokens) {
        if (tokens.length != 1) {
            System.out.println("Usage: new");
            return true;
        }
        model.newGame();
        System.out.println("New puzzle loaded.");
        printBoard();
        return true;
    }

    private boolean handleStatus(String[] tokens) {
        if (tokens.length != 1) {
            System.out.println("Usage: status");
            return true;
        }
        System.out.println("Validation feedback: " + model.isValidationFeedbackEnabled());
        System.out.println("Hint enabled: " + model.isHintEnabled());
        System.out.println("Random puzzle selection: " + model.isRandomPuzzleSelectionEnabled());
        System.out.println("Undo available: " + model.hasUndoableAction());
        System.out.println("Completed: " + model.isBoardCompleted());
        return true;
    }

    private boolean handleExit(String[] tokens) {
        if (tokens.length != 1) {
            System.out.println("Usage: exit");
            return true;
        }
        System.out.println("Goodbye.");
        return false;
    }

    private void onBoardMutation() {
        printBoard();
        if (model.isValidationFeedbackEnabled()) {
            printInvalidCells();
        }
    }

    private void printInvalidCells() {
        List<Model.CellPosition> invalid = model.getInvalidCells();
        if (invalid.isEmpty()) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Invalid cells: ");
        for (int i = 0; i < invalid.size(); i++) {
            Model.CellPosition cell = invalid.get(i);
            if (i > 0) {
                builder.append(", ");
            }
            builder.append("(")
                    .append(cell.getRow() + 1)
                    .append(",")
                    .append(cell.getCol() + 1)
                    .append(")");
            if (i == 14 && invalid.size() > 15) {
                builder.append(", ...");
                break;
            }
        }
        System.out.println(builder);
    }

    private void printBoard() {
        int[][] board = model.getBoardCopy();
        System.out.println();
        System.out.println("    1 2 3   4 5 6   7 8 9");
        for (int row = 0; row < Model.SIZE; row++) {
            if (row % 3 == 0) {
                System.out.println("  +-------+-------+-------+");
            }
            StringBuilder line = new StringBuilder();
            line.append(row + 1).append(" | ");
            for (int col = 0; col < Model.SIZE; col++) {
                int value = board[row][col];
                line.append(value == 0 ? "." : Integer.toString(value));
                if (col % 3 == 2) {
                    line.append(" | ");
                } else {
                    line.append(" ");
                }
            }
            System.out.println(line);
        }
        System.out.println("  +-------+-------+-------+");
        System.out.println();
    }

    private void printHelp() {
        System.out.println("Commands:");
        System.out.println("  show                         Display current grid");
        System.out.println("  set <row> <col> <value>      Set a value (1-9) in an editable cell");
        System.out.println("  clear <row> <col>            Clear an editable cell");
        System.out.println("  undo                         Undo the most recent action");
        System.out.println("  hint                         Fill one empty editable cell with a correct value");
        System.out.println("  reset                        Restore the puzzle to initial state");
        System.out.println("  new                          Start a new puzzle");
        System.out.println("  status                       Show current model flags and state");
        System.out.println("  help                         Show this help");
        System.out.println("  exit                         Quit the program");
    }

    private Integer parseIndex(String text) {
        Integer value = parseInt(text, "Row/Col");
        if (value == null) {
            return null;
        }
        if (value < 1 || value > 9) {
            System.out.println("Row/Col must be between 1 and 9.");
            return null;
        }
        return value - 1;
    }

    private Integer parseValue(String text) {
        Integer value = parseInt(text, "Value");
        if (value == null) {
            return null;
        }
        if (value < 1 || value > 9) {
            System.out.println("Value must be between 1 and 9.");
            return null;
        }
        return value;
    }

    private Integer parseInt(String text, String label) {
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException e) {
            System.out.println(label + " must be an integer.");
            return null;
        }
    }
}
