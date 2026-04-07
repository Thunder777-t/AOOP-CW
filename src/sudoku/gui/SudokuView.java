package sudoku.gui;

import sudoku.model.Model;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

public final class SudokuView extends JFrame implements Observer {
    private final Model model;
    private SudokuController controller;

    private final JButton[][] cellButtons;
    private final Set<Model.CellPosition> invalidCells;
    private final JButton[] numberPadButtons;
    private JButton eraseButton;
    private JButton undoButton;
    private JButton hintButton;
    private JCheckBox validationCheckBox;
    private JCheckBox hintCheckBox;
    private JCheckBox randomCheckBox;
    private JSpinner fixedPuzzleSpinner;
    private JLabel fixedPuzzleLabel;
    private final JLabel statusLabel;
    private JLabel selectionLabel;

    private int selectedRow;
    private int selectedCol;
    private boolean syncingFlagControls;
    private boolean syncingFixedPuzzleControl;

    public SudokuView(Model model) {
        super("Sudoku GUI");
        this.model = model;
        this.cellButtons = new JButton[Model.SIZE][Model.SIZE];
        this.invalidCells = new HashSet<Model.CellPosition>();
        this.numberPadButtons = new JButton[9];
        this.selectedRow = 0;
        this.selectedCol = 0;
        this.syncingFlagControls = false;
        this.syncingFixedPuzzleControl = false;

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(840, 720));
        setLayout(new BorderLayout(10, 10));

        JPanel boardPanel = createBoardPanel();
        JPanel rightPanel = createRightPanel();
        JPanel topPanel = createTopPanel();

        add(topPanel, BorderLayout.NORTH);
        add(boardPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        statusLabel = new JLabel("Ready.");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        add(statusLabel, BorderLayout.SOUTH);

        configureKeyboardActions();
        model.addObserver(this);
        refreshBoard();
        pack();
        setLocationRelativeTo(null);
    }

    public void setController(SudokuController controller) {
        this.controller = controller;
    }

    public int getSelectedRow() {
        return selectedRow;
    }

    public int getSelectedCol() {
        return selectedCol;
    }

    public void setSelectedCell(int row, int col) {
        this.selectedRow = row;
        this.selectedCol = col;
        refreshBoard();
    }

    public void setInvalidCells(List<Model.CellPosition> invalidCells) {
        this.invalidCells.clear();
        this.invalidCells.addAll(invalidCells);
        refreshBoard();
    }

    public void clearInvalidCells() {
        this.invalidCells.clear();
        refreshBoard();
    }

    public void setUndoEnabled(boolean enabled) {
        undoButton.setEnabled(enabled);
    }

    public void setHintButtonEnabled(boolean enabled) {
        hintButton.setEnabled(enabled);
    }

    public void setEraseEnabled(boolean enabled) {
        eraseButton.setEnabled(enabled);
    }

    public void setNumberInputEnabled(boolean enabled) {
        for (JButton button : numberPadButtons) {
            button.setEnabled(enabled);
        }
    }

    public void syncFlagControls(boolean validationEnabled, boolean hintEnabled, boolean randomEnabled) {
        syncingFlagControls = true;
        validationCheckBox.setSelected(validationEnabled);
        hintCheckBox.setSelected(hintEnabled);
        randomCheckBox.setSelected(randomEnabled);
        syncingFlagControls = false;
    }

    public void syncFixedPuzzleControls(int puzzleCount, int fixedIndex, boolean randomEnabled) {
        syncingFixedPuzzleControl = true;
        SpinnerNumberModel spinnerModel = (SpinnerNumberModel) fixedPuzzleSpinner.getModel();
        int max = Math.max(1, puzzleCount);
        spinnerModel.setMinimum(1);
        spinnerModel.setMaximum(max);
        int displayValue = Math.min(Math.max(fixedIndex + 1, 1), max);
        spinnerModel.setValue(displayValue);
        boolean enabled = !randomEnabled;
        fixedPuzzleSpinner.setEnabled(enabled);
        fixedPuzzleLabel.setEnabled(enabled);
        syncingFixedPuzzleControl = false;
    }

    public void showStatus(String text) {
        statusLabel.setText(text);
    }

    public void showSelectionInfo(String text) {
        selectionLabel.setText(text);
    }

    public void showCompletionMessage(String text) {
        JOptionPane.showMessageDialog(this, text, "Sudoku", JOptionPane.INFORMATION_MESSAGE);
        showStatus(text);
    }

    @Override
    public void update(Observable o, Object arg) {
        refreshBoard();
        if (controller != null) {
            controller.onModelChanged(arg);
        }
    }

    private JPanel createBoardPanel() {
        JPanel board = new JPanel(new GridLayout(Model.SIZE, Model.SIZE));
        board.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        Font cellFont = new Font(Font.SANS_SERIF, Font.BOLD, 24);
        for (int row = 0; row < Model.SIZE; row++) {
            for (int col = 0; col < Model.SIZE; col++) {
                JButton button = new JButton();
                button.setFocusPainted(false);
                button.setFont(cellFont);
                button.setOpaque(true);
                button.setHorizontalAlignment(SwingConstants.CENTER);
                button.setBorder(createCellBorder(row, col));
                final int r = row;
                final int c = col;
                button.addActionListener(e -> {
                    if (controller != null) {
                        controller.onCellSelected(r, c);
                    }
                });
                cellButtons[row][col] = button;
                board.add(button);
            }
        }
        return board;
    }

    private JPanel createRightPanel() {
        JPanel right = new JPanel();
        right.setLayout(new BorderLayout(8, 8));
        right.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));

        JPanel controls = new JPanel(new GridLayout(0, 1, 6, 6));
        eraseButton = new JButton("Erase");
        undoButton = new JButton("Undo");
        hintButton = new JButton("Hint");
        JButton resetButton = new JButton("Reset");
        JButton newGameButton = new JButton("New Game");
        eraseButton.setToolTipText("Clear selected editable cell (Backspace/Delete)");
        undoButton.setToolTipText("Undo last board action (U)");
        hintButton.setToolTipText("Reveal one correct value (H)");
        resetButton.setToolTipText("Reset current puzzle (R)");
        newGameButton.setToolTipText("Load a new puzzle (N)");

        eraseButton.addActionListener(e -> {
            if (controller != null) {
                controller.onEraseRequested();
            }
        });
        undoButton.addActionListener(e -> {
            if (controller != null) {
                controller.onUndoRequested();
            }
        });
        hintButton.addActionListener(e -> {
            if (controller != null) {
                controller.onHintRequested();
            }
        });
        resetButton.addActionListener(e -> {
            if (controller != null) {
                controller.onResetRequested();
            }
        });
        newGameButton.addActionListener(e -> {
            if (controller != null) {
                controller.onNewGameRequested();
            }
        });

        controls.add(eraseButton);
        controls.add(undoButton);
        controls.add(hintButton);
        controls.add(resetButton);
        controls.add(newGameButton);

        JPanel flags = new JPanel(new GridLayout(0, 1, 4, 4));
        validationCheckBox = new JCheckBox("Validation Feedback");
        hintCheckBox = new JCheckBox("Hint Enabled");
        randomCheckBox = new JCheckBox("Random Puzzle");
        validationCheckBox.setToolTipText("Highlight duplicate conflicts");
        hintCheckBox.setToolTipText("Enable or disable hint action");
        randomCheckBox.setToolTipText("When off, New Game uses fixed puzzle index");
        fixedPuzzleLabel = new JLabel("Fixed Puzzle #");
        fixedPuzzleSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
        fixedPuzzleSpinner.setToolTipText("Used when Random Puzzle is off");
        fixedPuzzleSpinner.addChangeListener(e -> {
            if (controller == null || syncingFixedPuzzleControl) {
                return;
            }
            Object value = fixedPuzzleSpinner.getValue();
            if (value instanceof Integer) {
                controller.onFixedPuzzleIndexChanged(((Integer) value).intValue() - 1);
            }
        });
        validationCheckBox.addActionListener(this::onFlagCheckboxChanged);
        hintCheckBox.addActionListener(this::onFlagCheckboxChanged);
        randomCheckBox.addActionListener(this::onFlagCheckboxChanged);
        flags.add(validationCheckBox);
        flags.add(hintCheckBox);
        flags.add(randomCheckBox);
        flags.add(fixedPuzzleLabel);
        flags.add(fixedPuzzleSpinner);

        JPanel numberPad = new JPanel(new GridLayout(3, 3, 4, 4));
        Font padFont = new Font(Font.SANS_SERIF, Font.PLAIN, 18);
        for (int i = 1; i <= 9; i++) {
            JButton key = new JButton(String.valueOf(i));
            key.setFont(padFont);
            key.setToolTipText("Input " + i);
            final int value = i;
            key.addActionListener(e -> {
                if (controller != null) {
                    controller.onNumberEntered(value);
                }
            });
            numberPad.add(key);
            numberPadButtons[i - 1] = key;
        }

        JPanel wrapper = new JPanel(new BorderLayout(8, 8));
        wrapper.add(controls, BorderLayout.NORTH);
        wrapper.add(flags, BorderLayout.CENTER);
        wrapper.add(numberPad, BorderLayout.SOUTH);
        right.add(wrapper, BorderLayout.NORTH);
        return right;
    }

    private JPanel createTopPanel() {
        JLabel title = new JLabel("Sudoku", SwingConstants.LEFT);
        title.setFont(new Font(Font.SERIF, Font.BOLD, 28));
        selectionLabel = new JLabel("Selected: -");
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        panel.add(title, BorderLayout.WEST);
        panel.add(selectionLabel, BorderLayout.EAST);
        return panel;
    }

    private void onFlagCheckboxChanged(ActionEvent event) {
        if (controller == null) {
            return;
        }
        Object source = event.getSource();
        if (source == validationCheckBox && !syncingFlagControls) {
            controller.onValidationFeedbackToggled(validationCheckBox.isSelected());
        } else if (source == hintCheckBox && !syncingFlagControls) {
            controller.onHintFlagToggled(hintCheckBox.isSelected());
        } else if (source == randomCheckBox && !syncingFlagControls) {
            controller.onRandomSelectionToggled(randomCheckBox.isSelected());
        }
    }

    private void configureKeyboardActions() {
        for (int value = 1; value <= 9; value++) {
            final int number = value;
            String actionKey = "num-" + value;
            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(String.valueOf(value)), actionKey);
            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke("NUMPAD" + value), actionKey);
            getRootPane().getActionMap().put(actionKey, new UIAction(() -> {
                if (controller != null) {
                    controller.onNumberEntered(number);
                }
            }));
        }
        bindKey("BACK_SPACE", () -> {
            if (controller != null) {
                controller.onEraseRequested();
            }
        });
        bindKey("DELETE", () -> {
            if (controller != null) {
                controller.onEraseRequested();
            }
        });
        bindKey("LEFT", () -> {
            if (controller != null) {
                controller.onMoveSelection(0, -1);
            }
        });
        bindKey("RIGHT", () -> {
            if (controller != null) {
                controller.onMoveSelection(0, 1);
            }
        });
        bindKey("UP", () -> {
            if (controller != null) {
                controller.onMoveSelection(-1, 0);
            }
        });
        bindKey("DOWN", () -> {
            if (controller != null) {
                controller.onMoveSelection(1, 0);
            }
        });
        bindKey("A", () -> {
            if (controller != null) {
                controller.onMoveSelection(0, -1);
            }
        });
        bindKey("D", () -> {
            if (controller != null) {
                controller.onMoveSelection(0, 1);
            }
        });
        bindKey("W", () -> {
            if (controller != null) {
                controller.onMoveSelection(-1, 0);
            }
        });
        bindKey("S", () -> {
            if (controller != null) {
                controller.onMoveSelection(1, 0);
            }
        });
        bindKey("0", () -> {
            if (controller != null) {
                controller.onEraseRequested();
            }
        });
        bindKey("NUMPAD0", () -> {
            if (controller != null) {
                controller.onEraseRequested();
            }
        });
        bindKey("H", () -> {
            if (controller != null) {
                controller.onHintRequested();
            }
        });
        bindKey("U", () -> {
            if (controller != null) {
                controller.onUndoRequested();
            }
        });
        bindKey("R", () -> {
            if (controller != null) {
                controller.onResetRequested();
            }
        });
        bindKey("N", () -> {
            if (controller != null) {
                controller.onNewGameRequested();
            }
        });
    }

    private void bindKey(String keyStroke, Runnable runnable) {
        String actionKey = "key-" + keyStroke;
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(keyStroke), actionKey);
        getRootPane().getActionMap().put(actionKey, new UIAction(runnable));
    }

    private void refreshBoard() {
        Runnable refreshTask = () -> {
            for (int row = 0; row < Model.SIZE; row++) {
                for (int col = 0; col < Model.SIZE; col++) {
                    JButton button = cellButtons[row][col];
                    int value = model.getCellValue(row, col);
                    button.setText(value == 0 ? "" : String.valueOf(value));
                    styleCell(button, row, col, value);
                }
            }
            repaint();
        };
        if (SwingUtilities.isEventDispatchThread()) {
            refreshTask.run();
        } else {
            SwingUtilities.invokeLater(refreshTask);
        }
    }

    private void styleCell(JButton button, int row, int col, int value) {
        boolean editable = model.isEditableCell(row, col);
        boolean selected = row == selectedRow && col == selectedCol;
        boolean invalid = invalidCells.contains(new Model.CellPosition(row, col));

        Color background;
        Color foreground;
        Font base = button.getFont();
        if (!editable) {
            background = new Color(224, 231, 255);
            foreground = new Color(15, 23, 42);
            button.setFont(base.deriveFont(Font.BOLD));
        } else {
            background = Color.WHITE;
            foreground = new Color(17, 24, 39);
            button.setFont(base.deriveFont(Font.PLAIN));
        }

        if (invalid && value != 0) {
            background = new Color(255, 228, 230);
            foreground = new Color(153, 27, 27);
        }
        if (selected) {
            background = new Color(191, 219, 254);
            foreground = new Color(30, 58, 138);
        }

        button.setBackground(background);
        button.setForeground(foreground);
    }

    private static javax.swing.border.Border createCellBorder(int row, int col) {
        int top = row % 3 == 0 ? 2 : 1;
        int left = col % 3 == 0 ? 2 : 1;
        int bottom = row == Model.SIZE - 1 ? 2 : 1;
        int right = col == Model.SIZE - 1 ? 2 : 1;
        return BorderFactory.createMatteBorder(top, left, bottom, right, UIManager.getColor("Label.foreground"));
    }

    private static final class UIAction extends javax.swing.AbstractAction {
        private final Runnable runnable;

        private UIAction(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            runnable.run();
        }
    }
}
