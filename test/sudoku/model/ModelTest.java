package sudoku.model;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelTest {
    private static final String PUZZLE_A = "706008100020304568850609200200756041087200030601090052908400000475132980000080417";
    private static final String PUZZLE_B = "534678912672195348198342567859761423426853791713924856961537284287419635345286170";
    private static final String PUZZLE_C = "534678912672195348198342567859761423426853791713924856961537284287419635345286179";

    @Test
    void setCellValue_shouldRejectFixedCell_andAcceptEditableCell() throws IOException {
        // Scenario: player tries one illegal write to a fixed cell and one legal write to an editable empty cell.
        Model model = createModel(PUZZLE_A);

        assertFalse(model.setCellValue(0, 0, 5), "Fixed cell must reject edits");
        assertTrue(model.setCellValue(0, 1, 3), "Editable empty cell should accept valid value");
        assertEquals(3, model.getCellValue(0, 1));
        assertFalse(model.isEditableCell(0, 0));
        assertTrue(model.isEditableCell(0, 1));
    }

    @Test
    void duplicateInput_shouldBeReportedAndNotComplete() throws IOException {
        // Scenario: player enters a value that duplicates another value in the same row.
        Model model = createModel(PUZZLE_A);

        assertTrue(model.setCellValue(0, 1, 7), "Model accepts temporary invalid states by design");
        List<Model.CellPosition> invalid = model.getInvalidCells();

        assertTrue(invalid.contains(new Model.CellPosition(0, 0)));
        assertTrue(invalid.contains(new Model.CellPosition(0, 1)));
        assertFalse(model.isBoardCompleted(), "Board with duplicates cannot be complete");
    }

    @Test
    void hintUndoResetAndPuzzleSelection_shouldRespectFlagsAndState() throws IOException {
        // Scenario: verify hint flag behavior, undo/reset lifecycle, and fixed-puzzle selection behavior.
        Model model = createModel(PUZZLE_B, PUZZLE_C);

        model.setHintEnabled(false);
        assertNull(model.requestHint(), "Hint disabled should return null");

        model.setHintEnabled(true);
        Model.Hint hint = model.requestHint();
        assertNotNull(hint);
        assertEquals(8, hint.getRow());
        assertEquals(8, hint.getCol());
        assertEquals(9, hint.getValue());
        assertTrue(model.isBoardCompleted());
        assertTrue(model.hasUndoableAction());

        assertTrue(model.undoLastAction());
        assertEquals(0, model.getCellValue(8, 8));
        assertFalse(model.isBoardCompleted());

        assertTrue(model.setCellValue(8, 8, 9));
        assertTrue(model.isBoardCompleted());
        model.reset();
        assertEquals(0, model.getCellValue(8, 8));
        assertFalse(model.isBoardCompleted());

        model.setRandomPuzzleSelectionEnabled(false);
        model.setFixedPuzzleIndex(1);
        model.newGame();
        assertEquals(charToDigit(PUZZLE_C.charAt(0)), model.getCellValue(0, 0));
    }

    private static Model createModel(String... puzzleLines) throws IOException {
        Path puzzleFile = Files.createTempFile("aoop-sudoku-test-", ".txt");
        Files.write(puzzleFile, Arrays.asList(puzzleLines), StandardCharsets.UTF_8);
        puzzleFile.toFile().deleteOnExit();
        return new Model(puzzleFile);
    }

    private static int charToDigit(char c) {
        return c - '0';
    }
}
