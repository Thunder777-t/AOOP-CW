package sudoku.gui;

import sudoku.model.Model;

import java.util.List;

public final class SudokuController {
    private final Model model;
    private final SudokuView view;
    private boolean completionShown;

    public SudokuController(Model model, SudokuView view) {
        this.model = model;
        this.view = view;
        this.completionShown = false;
        this.view.setSelectedCell(0, 0);
        syncViewState();
    }

    public void onCellSelected(int row, int col) {
        if (row < 0 || row >= Model.SIZE || col < 0 || col >= Model.SIZE) {
            return;
        }
        view.setSelectedCell(row, col);
        syncViewState();
    }

    public void onMoveSelection(int rowDelta, int colDelta) {
        int nextRow = clamp(view.getSelectedRow() + rowDelta);
        int nextCol = clamp(view.getSelectedCol() + colDelta);
        onCellSelected(nextRow, nextCol);
    }

    public void onNumberEntered(int value) {
        int row = view.getSelectedRow();
        int col = view.getSelectedCol();
        if (row < 0 || col < 0) {
            view.showStatus("Please select a cell first.");
            return;
        }
        if (!model.setCellValue(row, col, value)) {
            view.showStatus("Move rejected. You can only edit empty original cells with values 1-9.");
        }
    }

    public void onEraseRequested() {
        int row = view.getSelectedRow();
        int col = view.getSelectedCol();
        if (row < 0 || col < 0) {
            view.showStatus("Please select a cell first.");
            return;
        }
        if (!model.clearCell(row, col)) {
            view.showStatus("Erase rejected. Selected cell may be fixed or already empty.");
        }
    }

    public void onUndoRequested() {
        if (!model.undoLastAction()) {
            view.showStatus("Nothing to undo.");
        }
    }

    public void onHintRequested() {
        Model.Hint hint = model.requestHint();
        if (hint == null) {
            if (!model.isHintEnabled()) {
                view.showStatus("Hint is disabled.");
            } else {
                view.showStatus("No editable empty cells available for hint.");
            }
        } else {
            view.showStatus("Hint: filled (" + (hint.getRow() + 1) + ", " + (hint.getCol() + 1) + ") with " + hint.getValue() + ".");
            view.setSelectedCell(hint.getRow(), hint.getCol());
        }
    }

    public void onResetRequested() {
        completionShown = false;
        model.reset();
    }

    public void onNewGameRequested() {
        completionShown = false;
        model.newGame();
        view.setSelectedCell(0, 0);
    }

    public void onValidationFeedbackToggled(boolean enabled) {
        model.setValidationFeedbackEnabled(enabled);
    }

    public void onHintFlagToggled(boolean enabled) {
        model.setHintEnabled(enabled);
    }

    public void onRandomSelectionToggled(boolean enabled) {
        model.setRandomPuzzleSelectionEnabled(enabled);
    }

    public void onModelChanged(Object ignoredChangeType) {
        syncViewState();
        boolean completed = model.isBoardCompleted();
        if (completed && !completionShown) {
            completionShown = true;
            view.showCompletionMessage("Puzzle completed!");
        } else if (!completed) {
            completionShown = false;
        }
    }

    private void syncViewState() {
        if (model.isValidationFeedbackEnabled()) {
            List<Model.CellPosition> invalidCells = model.getInvalidCells();
            view.setInvalidCells(invalidCells);
        } else {
            view.clearInvalidCells();
        }
        view.setUndoEnabled(model.hasUndoableAction());
        view.setHintButtonEnabled(model.isHintEnabled());
        view.syncFlagControls(
                model.isValidationFeedbackEnabled(),
                model.isHintEnabled(),
                model.isRandomPuzzleSelectionEnabled()
        );
        int row = view.getSelectedRow();
        int col = view.getSelectedCol();
        boolean selected = row >= 0 && col >= 0;
        boolean canErase = selected && model.isEditableCell(row, col) && model.getCellValue(row, col) != 0;
        view.setEraseEnabled(canErase);
    }

    private static int clamp(int value) {
        if (value < 0) {
            return 0;
        }
        if (value >= Model.SIZE) {
            return Model.SIZE - 1;
        }
        return value;
    }
}
