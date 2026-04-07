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
        int nextRow = wrap(view.getSelectedRow() + rowDelta);
        int nextCol = wrap(view.getSelectedCol() + colDelta);
        onCellSelected(nextRow, nextCol);
    }

    public void onNumberEntered(int value) {
        int row = view.getSelectedRow();
        int col = view.getSelectedCol();
        if (row < 0 || col < 0) {
            view.showStatus("Please select a cell first.");
            return;
        }
        if (value < 1 || value > 9) {
            view.showStatus("Only values 1-9 are allowed.");
            return;
        }
        if (!model.isEditableCell(row, col)) {
            view.showStatus("Selected cell is fixed and cannot be modified.");
            return;
        }
        if (!model.setCellValue(row, col, value)) {
            view.showStatus("Move rejected. You can only edit empty original cells with values 1-9.");
        } else {
            view.showStatus("Set (" + (row + 1) + ", " + (col + 1) + ") to " + value + ".");
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
        } else {
            view.showStatus("Cleared (" + (row + 1) + ", " + (col + 1) + ").");
        }
    }

    public void onUndoRequested() {
        if (!model.undoLastAction()) {
            view.showStatus("Nothing to undo.");
        } else {
            view.showStatus("Undo completed.");
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
        view.showStatus("Puzzle reset.");
    }

    public void onNewGameRequested() {
        completionShown = false;
        model.newGame();
        view.setSelectedCell(0, 0);
        view.showStatus("New puzzle loaded.");
    }

    public void onValidationFeedbackToggled(boolean enabled) {
        model.setValidationFeedbackEnabled(enabled);
        view.showStatus(enabled ? "Validation feedback enabled." : "Validation feedback disabled.");
    }

    public void onHintFlagToggled(boolean enabled) {
        model.setHintEnabled(enabled);
        view.showStatus(enabled ? "Hint enabled." : "Hint disabled.");
    }

    public void onRandomSelectionToggled(boolean enabled) {
        model.setRandomPuzzleSelectionEnabled(enabled);
        view.showStatus(enabled ? "Random puzzle selection enabled." : "Random puzzle selection disabled.");
    }

    public void onFixedPuzzleIndexChanged(int index) {
        try {
            model.setFixedPuzzleIndex(index);
            view.showStatus("Fixed puzzle set to #" + (index + 1) + ". Click New Game to load it.");
        } catch (IllegalArgumentException e) {
            view.showStatus("Invalid fixed puzzle index.");
        }
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
        view.setHintButtonEnabled(model.isHintEnabled() && model.hasEditableEmptyCell());
        view.syncFlagControls(
                model.isValidationFeedbackEnabled(),
                model.isHintEnabled(),
                model.isRandomPuzzleSelectionEnabled()
        );
        view.syncFixedPuzzleControls(
                model.getPuzzleCount(),
                model.getFixedPuzzleIndex(),
                model.isRandomPuzzleSelectionEnabled()
        );
        int row = view.getSelectedRow();
        int col = view.getSelectedCol();
        boolean selected = row >= 0 && col >= 0;
        boolean editableSelected = selected && model.isEditableCell(row, col);
        view.showSelectionInfo(buildSelectionInfo(row, col, selected, editableSelected));
        view.setNumberInputEnabled(editableSelected);
        boolean canErase = editableSelected && model.getCellValue(row, col) != 0;
        view.setEraseEnabled(canErase);
    }

    private String buildSelectionInfo(int row, int col, boolean selected, boolean editableSelected) {
        if (!selected) {
            return "Selected: -";
        }
        int value = model.getCellValue(row, col);
        String mode = editableSelected ? "editable" : "fixed";
        String valueText = value == 0 ? "." : Integer.toString(value);
        return "Selected: (" + (row + 1) + "," + (col + 1) + ") "
                + mode + " value=" + valueText;
    }

    private static int wrap(int value) {
        int result = value % Model.SIZE;
        if (result < 0) {
            result += Model.SIZE;
        }
        return result;
    }
}
