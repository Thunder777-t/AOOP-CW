package sudoku.gui;

import sudoku.model.Model;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class SudokuGuiMain {
    private SudokuGuiMain() {
    }

    public static void main(String[] args) {
        Path puzzlePath = args.length > 0 ? Paths.get(args[0]) : Paths.get("puzzles.txt");
        SwingUtilities.invokeLater(() -> launchGui(puzzlePath));
    }

    private static void launchGui(Path puzzlePath) {
        try {
            Model model = new Model(puzzlePath);
            SudokuView view = new SudokuView(model);
            SudokuController controller = new SudokuController(model, view);
            view.setController(controller);
            view.setVisible(true);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Cannot load puzzles file: " + puzzlePath + System.lineSeparator() + e.getMessage(),
                    "Startup Error",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Failed to start GUI: " + e.getMessage(),
                    "Startup Error",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
        }
    }
}
