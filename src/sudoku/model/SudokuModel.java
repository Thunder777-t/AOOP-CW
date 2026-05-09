package sudoku.model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Observer;

public interface SudokuModel {
    int SIZE = 9;

    enum ChangeType {
        NEW_GAME,
        RESET,
        CELL_UPDATED,
        UNDO,
        FLAGS_UPDATED
    }

    final class CellPosition {
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

    final class Hint {
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

    void addObserver(Observer observer);

    void deleteObserver(Observer observer);

    void loadPuzzles(Path puzzleFilePath) throws IOException;

    void newGame();

    void reset();

    boolean setCellValue(int row, int col, int value);

    boolean clearCell(int row, int col);

    boolean undoLastAction();

    Hint requestHint();

    List<CellPosition> getInvalidCells();

    boolean isBoardCompleted();

    int getCellValue(int row, int col);

    boolean isEditableCell(int row, int col);

    int[][] getBoardCopy();

    boolean isValidationFeedbackEnabled();

    void setValidationFeedbackEnabled(boolean validationFeedbackEnabled);

    boolean isHintEnabled();

    void setHintEnabled(boolean hintEnabled);

    boolean isRandomPuzzleSelectionEnabled();

    void setRandomPuzzleSelectionEnabled(boolean randomPuzzleSelectionEnabled);

    int getFixedPuzzleIndex();

    void setFixedPuzzleIndex(int fixedPuzzleIndex);

    int getPuzzleCount();

    boolean hasUndoableAction();

    boolean hasEditableEmptyCell();
}
