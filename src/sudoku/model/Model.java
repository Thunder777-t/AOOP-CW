package sudoku.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Observable;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public class Model extends Observable {
    public static final int SIZE = 9;
    private static final int BOX_SIZE = 3;

    public enum ChangeType {
        NEW_GAME,
        RESET,
        CELL_UPDATED,
        UNDO,
        FLAGS_UPDATED
    }

    public static final class CellPosition {
        private final int row;
        private final int col;

        public CellPosition(int row, int col) {
            this.row = row;
            this.col = col;
        }

        public int getRow() {
            return row;
        }

        public int getCol() {
            return col;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof CellPosition)) {
                return false;
            }
            CellPosition that = (CellPosition) other;
            return row == that.row && col == that.col;
        }

        @Override
        public int hashCode() {
            return 31 * row + col;
        }
    }

    public static final class Hint {
        private final int row;
        private final int col;
        private final int value;

        public Hint(int row, int col, int value) {
            this.row = row;
            this.col = col;
            this.value = value;
        }

        public int getRow() {
            return row;
        }

        public int getCol() {
            return col;
        }

        public int getValue() {
            return value;
        }
    }

    private static final class Move {
        private final int row;
        private final int col;
        private final int previousValue;
        private final int newValue;

        private Move(int row, int col, int previousValue, int newValue) {
            this.row = row;
            this.col = col;
            this.previousValue = previousValue;
            this.newValue = newValue;
        }
    }

    private final Random random = new Random();
    private final List<int[][]> puzzlePool = new ArrayList<int[][]>();

    private boolean validationFeedbackEnabled = true;
    private boolean hintEnabled = true;
    private boolean randomPuzzleSelectionEnabled = true;

    private int fixedPuzzleIndex = 0;
    private int[][] initialBoard;
    private int[][] currentBoard;
    private int[][] solvedBoard;
    private boolean[][] fixedCells;
    private Move lastMove;

    public Model(String puzzleFilePath) throws IOException {
        this(Paths.get(Objects.requireNonNull(puzzleFilePath, "Puzzle file path must not be null")));
    }

    public Model(Path puzzleFilePath) throws IOException {
        assert puzzleFilePath != null : "Puzzle file path must not be null";
        loadPuzzles(puzzleFilePath);
        newGame();
        assertModelInvariant();
    }

    public final void loadPuzzles(Path puzzleFilePath) throws IOException {
        assert puzzleFilePath != null : "Puzzle file path must not be null";
        assertModelInvariant();
        List<String> lines = Files.readAllLines(puzzleFilePath, StandardCharsets.UTF_8);
        List<int[][]> parsed = new ArrayList<int[][]>();
        for (String raw : lines) {
            if (raw == null) {
                continue;
            }
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            parsed.add(parsePuzzleLine(line));
        }
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException("No valid puzzles in file: " + puzzleFilePath);
        }
        puzzlePool.clear();
        puzzlePool.addAll(parsed);
        if (fixedPuzzleIndex >= puzzlePool.size()) {
            fixedPuzzleIndex = 0;
        }
        assert !puzzlePool.isEmpty() : "Puzzle pool must not be empty after loading";
        assertModelInvariant();
    }

    public void newGame() {
        assertModelInvariant();
        assert !puzzlePool.isEmpty() : "Puzzle pool must not be empty";
        int index;
        if (randomPuzzleSelectionEnabled) {
            index = random.nextInt(puzzlePool.size());
        } else {
            index = fixedPuzzleIndex;
        }
        initialBoard = deepCopy(puzzlePool.get(index));
        currentBoard = deepCopy(initialBoard);
        fixedCells = buildFixedCells(initialBoard);
        solvedBoard = deepCopy(initialBoard);
        if (!solveInPlace(solvedBoard)) {
            throw new IllegalStateException("Puzzle has no solution for index " + index);
        }
        lastMove = null;
        notifyChange(ChangeType.NEW_GAME);
        assertModelInvariant();
    }

    public void reset() {
        assertModelInvariant();
        ensureGameLoaded();
        currentBoard = deepCopy(initialBoard);
        lastMove = null;
        notifyChange(ChangeType.RESET);
        assertModelInvariant();
    }

    public boolean setCellValue(int row, int col, int value) {
        assertModelInvariant();
        assert row >= 0 && row < SIZE : "Row out of range";
        assert col >= 0 && col < SIZE : "Column out of range";
        assert value >= 1 && value <= 9 : "Value must be 1-9";
        if (!isInsideBoard(row, col) || !isEditableCell(row, col) || !isValue(value)) {
            return false;
        }
        int previous = currentBoard[row][col];
        if (previous == value) {
            return false;
        }
        currentBoard[row][col] = value;
        lastMove = new Move(row, col, previous, value);
        notifyChange(ChangeType.CELL_UPDATED);
        assert currentBoard[row][col] == value : "Cell should contain assigned value";
        assertModelInvariant();
        return true;
    }

    public boolean clearCell(int row, int col) {
        assertModelInvariant();
        assert row >= 0 && row < SIZE : "Row out of range";
        assert col >= 0 && col < SIZE : "Column out of range";
        if (!isInsideBoard(row, col) || !isEditableCell(row, col)) {
            return false;
        }
        int previous = currentBoard[row][col];
        if (previous == 0) {
            return false;
        }
        currentBoard[row][col] = 0;
        lastMove = new Move(row, col, previous, 0);
        notifyChange(ChangeType.CELL_UPDATED);
        assert currentBoard[row][col] == 0 : "Cleared cell should be zero";
        assertModelInvariant();
        return true;
    }

    public boolean undoLastAction() {
        assertModelInvariant();
        if (lastMove == null) {
            return false;
        }
        currentBoard[lastMove.row][lastMove.col] = lastMove.previousValue;
        lastMove = null;
        notifyChange(ChangeType.UNDO);
        assertModelInvariant();
        return true;
    }

    public Hint requestHint() {
        assertModelInvariant();
        if (!hintEnabled) {
            return null;
        }
        ensureGameLoaded();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (isEditableCell(row, col) && currentBoard[row][col] == 0) {
                    int value = solvedBoard[row][col];
                    currentBoard[row][col] = value;
                    lastMove = new Move(row, col, 0, value);
                    notifyChange(ChangeType.CELL_UPDATED);
                    assertModelInvariant();
                    return new Hint(row, col, value);
                }
            }
        }
        return null;
    }

    public List<CellPosition> getInvalidCells() {
        assertModelInvariant();
        ensureGameLoaded();
        Set<CellPosition> invalid = new LinkedHashSet<CellPosition>();
        collectRowDuplicates(invalid);
        collectColumnDuplicates(invalid);
        collectBoxDuplicates(invalid);
        List<CellPosition> result = new ArrayList<CellPosition>(invalid);
        assertModelInvariant();
        return result;
    }

    public boolean isBoardCompleted() {
        assertModelInvariant();
        ensureGameLoaded();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (currentBoard[row][col] != solvedBoard[row][col]) {
                    return false;
                }
            }
        }
        return true;
    }

    public int getCellValue(int row, int col) {
        assertModelInvariant();
        ensureGameLoaded();
        if (!isInsideBoard(row, col)) {
            throw new IllegalArgumentException("Cell out of range: (" + row + ", " + col + ")");
        }
        return currentBoard[row][col];
    }

    public boolean isEditableCell(int row, int col) {
        assertModelInvariant();
        ensureGameLoaded();
        if (!isInsideBoard(row, col)) {
            return false;
        }
        return !fixedCells[row][col];
    }

    public int[][] getBoardCopy() {
        assertModelInvariant();
        ensureGameLoaded();
        return deepCopy(currentBoard);
    }

    public boolean isValidationFeedbackEnabled() {
        return validationFeedbackEnabled;
    }

    public void setValidationFeedbackEnabled(boolean validationFeedbackEnabled) {
        assertModelInvariant();
        this.validationFeedbackEnabled = validationFeedbackEnabled;
        notifyChange(ChangeType.FLAGS_UPDATED);
        assertModelInvariant();
    }

    public boolean isHintEnabled() {
        return hintEnabled;
    }

    public void setHintEnabled(boolean hintEnabled) {
        assertModelInvariant();
        this.hintEnabled = hintEnabled;
        notifyChange(ChangeType.FLAGS_UPDATED);
        assertModelInvariant();
    }

    public boolean isRandomPuzzleSelectionEnabled() {
        return randomPuzzleSelectionEnabled;
    }

    public void setRandomPuzzleSelectionEnabled(boolean randomPuzzleSelectionEnabled) {
        assertModelInvariant();
        this.randomPuzzleSelectionEnabled = randomPuzzleSelectionEnabled;
        notifyChange(ChangeType.FLAGS_UPDATED);
        assertModelInvariant();
    }

    public int getFixedPuzzleIndex() {
        return fixedPuzzleIndex;
    }

    public void setFixedPuzzleIndex(int fixedPuzzleIndex) {
        assertModelInvariant();
        if (fixedPuzzleIndex < 0 || fixedPuzzleIndex >= puzzlePool.size()) {
            throw new IllegalArgumentException("Fixed puzzle index out of range: " + fixedPuzzleIndex);
        }
        this.fixedPuzzleIndex = fixedPuzzleIndex;
        notifyChange(ChangeType.FLAGS_UPDATED);
        assert this.fixedPuzzleIndex == fixedPuzzleIndex : "Fixed index should be updated";
        assertModelInvariant();
    }

    public int getPuzzleCount() {
        return puzzlePool.size();
    }

    public boolean hasUndoableAction() {
        return lastMove != null;
    }

    public boolean hasEditableEmptyCell() {
        assertModelInvariant();
        ensureGameLoaded();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (isEditableCell(row, col) && currentBoard[row][col] == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private void notifyChange(ChangeType changeType) {
        setChanged();
        notifyObservers(changeType);
    }

    private void collectRowDuplicates(Set<CellPosition> invalid) {
        for (int row = 0; row < SIZE; row++) {
            List<List<CellPosition>> byValue = createValueBuckets();
            for (int col = 0; col < SIZE; col++) {
                int value = currentBoard[row][col];
                if (!isValue(value)) {
                    continue;
                }
                byValue.get(value).add(new CellPosition(row, col));
            }
            addGroupsWithDuplicates(invalid, byValue);
        }
    }

    private void collectColumnDuplicates(Set<CellPosition> invalid) {
        for (int col = 0; col < SIZE; col++) {
            List<List<CellPosition>> byValue = createValueBuckets();
            for (int row = 0; row < SIZE; row++) {
                int value = currentBoard[row][col];
                if (!isValue(value)) {
                    continue;
                }
                byValue.get(value).add(new CellPosition(row, col));
            }
            addGroupsWithDuplicates(invalid, byValue);
        }
    }

    private void collectBoxDuplicates(Set<CellPosition> invalid) {
        for (int boxRow = 0; boxRow < SIZE; boxRow += BOX_SIZE) {
            for (int boxCol = 0; boxCol < SIZE; boxCol += BOX_SIZE) {
                List<List<CellPosition>> byValue = createValueBuckets();
                for (int row = boxRow; row < boxRow + BOX_SIZE; row++) {
                    for (int col = boxCol; col < boxCol + BOX_SIZE; col++) {
                        int value = currentBoard[row][col];
                        if (!isValue(value)) {
                            continue;
                        }
                        byValue.get(value).add(new CellPosition(row, col));
                    }
                }
                addGroupsWithDuplicates(invalid, byValue);
            }
        }
    }

    private static void addGroupsWithDuplicates(Set<CellPosition> invalid, List<List<CellPosition>> byValue) {
        for (int value = 1; value <= 9; value++) {
            List<CellPosition> cells = byValue.get(value);
            if (cells.size() > 1) {
                invalid.addAll(cells);
            }
        }
    }

    private static List<List<CellPosition>> createValueBuckets() {
        List<List<CellPosition>> buckets = new ArrayList<List<CellPosition>>(10);
        for (int i = 0; i <= 9; i++) {
            buckets.add(new ArrayList<CellPosition>());
        }
        return buckets;
    }

    private static int[][] parsePuzzleLine(String line) {
        if (line.length() != SIZE * SIZE) {
            throw new IllegalArgumentException("Puzzle line must contain exactly 81 digits");
        }
        int[][] board = new int[SIZE][SIZE];
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException("Puzzle line contains non-digit characters");
            }
            board[i / SIZE][i % SIZE] = c - '0';
        }
        return board;
    }

    private static boolean[][] buildFixedCells(int[][] board) {
        boolean[][] fixed = new boolean[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                fixed[row][col] = board[row][col] != 0;
            }
        }
        return fixed;
    }

    private static boolean solveInPlace(int[][] board) {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (board[row][col] == 0) {
                    for (int value = 1; value <= 9; value++) {
                        if (isSafe(board, row, col, value)) {
                            board[row][col] = value;
                            if (solveInPlace(board)) {
                                return true;
                            }
                            board[row][col] = 0;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isSafe(int[][] board, int row, int col, int value) {
        for (int i = 0; i < SIZE; i++) {
            if (board[row][i] == value || board[i][col] == value) {
                return false;
            }
        }
        int startRow = row - (row % BOX_SIZE);
        int startCol = col - (col % BOX_SIZE);
        for (int r = startRow; r < startRow + BOX_SIZE; r++) {
            for (int c = startCol; c < startCol + BOX_SIZE; c++) {
                if (board[r][c] == value) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int[][] deepCopy(int[][] source) {
        int[][] copy = new int[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            System.arraycopy(source[row], 0, copy[row], 0, SIZE);
        }
        return copy;
    }

    private static boolean isInsideBoard(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    private static boolean isValue(int value) {
        return value >= 1 && value <= 9;
    }

    private void ensureGameLoaded() {
        if (currentBoard == null || fixedCells == null || solvedBoard == null) {
            throw new IllegalStateException("Game has not been initialized");
        }
    }

    private void assertModelInvariant() {
        assert puzzlePool != null : "Puzzle pool must exist";
        if (puzzlePool.isEmpty()) {
            assert initialBoard == null : "Initial board should be null before loading";
            assert currentBoard == null : "Current board should be null before loading";
            assert solvedBoard == null : "Solved board should be null before loading";
            assert fixedCells == null : "Fixed cells should be null before loading";
            assert lastMove == null : "No move should exist before loading";
            return;
        }
        assert fixedPuzzleIndex >= 0 && fixedPuzzleIndex < puzzlePool.size() : "Fixed puzzle index out of range";
        for (int[][] puzzle : puzzlePool) {
            assert hasBoardShape(puzzle) : "Puzzle must be 9x9";
            assert hasCellRange(puzzle, 0, 9) : "Puzzle values must be in [0, 9]";
        }
        if (initialBoard != null || currentBoard != null || solvedBoard != null || fixedCells != null) {
            assert hasBoardShape(initialBoard) : "Initial board must be 9x9";
            assert hasBoardShape(currentBoard) : "Current board must be 9x9";
            assert hasBoardShape(solvedBoard) : "Solved board must be 9x9";
            assert hasFixedShape(fixedCells) : "Fixed cells matrix must be 9x9";
            assert hasCellRange(initialBoard, 0, 9) : "Initial board values must be in [0, 9]";
            assert hasCellRange(currentBoard, 0, 9) : "Current board values must be in [0, 9]";
            assert hasCellRange(solvedBoard, 1, 9) : "Solved board values must be in [1, 9]";
            assert fixedCellsMatchInitial(initialBoard, fixedCells) : "Fixed markers must match initial board";
            assert fixedCellsPreserved(initialBoard, currentBoard, fixedCells) : "Fixed cells cannot change";
        }
        if (lastMove != null) {
            assert isInsideBoard(lastMove.row, lastMove.col) : "Last move row/col must be valid";
            assert lastMove.previousValue >= 0 && lastMove.previousValue <= 9 : "Previous value range invalid";
            assert lastMove.newValue >= 0 && lastMove.newValue <= 9 : "New value range invalid";
        }
    }

    private static boolean hasBoardShape(int[][] board) {
        if (board == null || board.length != SIZE) {
            return false;
        }
        for (int row = 0; row < SIZE; row++) {
            if (board[row] == null || board[row].length != SIZE) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasFixedShape(boolean[][] fixed) {
        if (fixed == null || fixed.length != SIZE) {
            return false;
        }
        for (int row = 0; row < SIZE; row++) {
            if (fixed[row] == null || fixed[row].length != SIZE) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasCellRange(int[][] board, int min, int max) {
        if (!hasBoardShape(board)) {
            return false;
        }
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                int value = board[row][col];
                if (value < min || value > max) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean fixedCellsMatchInitial(int[][] initialBoard, boolean[][] fixedCells) {
        if (!hasBoardShape(initialBoard) || !hasFixedShape(fixedCells)) {
            return false;
        }
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                boolean expectedFixed = initialBoard[row][col] != 0;
                if (fixedCells[row][col] != expectedFixed) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean fixedCellsPreserved(int[][] initialBoard, int[][] currentBoard, boolean[][] fixedCells) {
        if (!hasBoardShape(initialBoard) || !hasBoardShape(currentBoard) || !hasFixedShape(fixedCells)) {
            return false;
        }
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (fixedCells[row][col] && currentBoard[row][col] != initialBoard[row][col]) {
                    return false;
                }
            }
        }
        return true;
    }
}
