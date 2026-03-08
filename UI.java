import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Main Checkers application with graphical UI.
 * Contains game state, board rendering, and integration with algorithm modules.
 * UI design must NOT be changed - only backend logic should be modified.
 */
public class UI extends JFrame {

    // Game constants
    public static final int BOARD_SIZE = 8;
    public static final int P1 = 1;       // White (Human)
    public static final int P1_KING = 2;
    public static final int P2 = -1;      // Red/Black (AI)
    public static final int P2_KING = -2;
    public static final int EMPTY = 0;

    // Difficulty modes - must match UI options
    public static final String GREEDY = "Greedy";
    public static final String DIVIDE_AND_CONQUER = "Divide and Conquer";
    public static final String BACKTRACKING = "Backtracking";
    public static final String DYNAMIC_PROGRAMMING = "Dynamic Programming";

    private GameState gameState;
    private String selectedMode = GREEDY;
    private int selectedRow = -1, selectedCol = -1;
    private JPanel boardPanel;
    private JPanel rightPanel;
    private JTextArea moveHistoryArea;
    private JButton[] diffButtons;
    private JButton undoButton;
    private boolean gameOver = false;
    private static final int SQUARE_SIZE = 72;
    private List<GameState> stateHistory = new ArrayList<>();

    public UI() {
        setTitle("Checkers - DAA Project");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(0x1e1e1e));

        gameState = new GameState();

        // Left sidebar
        JPanel sidebar = createSidebar();
        add(sidebar, BorderLayout.WEST);

        // Center - Board (occupies main area, positioned for clear visibility)
        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(new Color(0x121212));
        mainContent.setBorder(new EmptyBorder(10, 10, 30, 10));
        boardPanel = createBoardPanel();
        JPanel boardWrapper = new JPanel(new GridBagLayout());
        boardWrapper.setBackground(new Color(0x121212));
        boardWrapper.add(boardPanel);
        mainContent.add(boardWrapper, BorderLayout.SOUTH);
        add(mainContent, BorderLayout.CENTER);

        // Right panel - Difficulty and Move History
        rightPanel = createRightPanel();
        add(rightPanel, BorderLayout.EAST);

        setSize(1100, 750);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(60, 0));
        sidebar.setBackground(new Color(0x1e1e1e));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0x333333)));

        JLabel title = new JLabel("C");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(Box.createVerticalStrut(20));
        sidebar.add(title);

        JLabel checkersLabel = new JLabel("Checkers");
        checkersLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        checkersLabel.setForeground(Color.WHITE);
        checkersLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(checkersLabel);
        sidebar.add(Box.createVerticalStrut(30));

        return sidebar;
    }

    private JPanel createBoardPanel() {
        JPanel frame = new JPanel(new BorderLayout());
        frame.setBackground(new Color(0x2d2d2d));
        frame.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x769656), 2),
            new EmptyBorder(10, 10, 10, 10)
        ));

        JPanel grid = new JPanel(new GridLayout(BOARD_SIZE, BOARD_SIZE));
        int boardPixels = BOARD_SIZE * SQUARE_SIZE;
        grid.setPreferredSize(new Dimension(boardPixels, boardPixels));
        grid.setBackground(new Color(0x769656));

        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                JPanel square = createSquare(r, c);
                grid.add(square);
            }
        }
        frame.add(grid, BorderLayout.CENTER);
        return frame;
    }

    private JPanel createSquare(int r, int c) {
        final int row = r, col = c;
        JPanel square = new JPanel(new GridBagLayout());
        square.setPreferredSize(new Dimension(SQUARE_SIZE, SQUARE_SIZE));
        boolean isDark = (r + c) % 2 != 0;
        square.setBackground(isDark ? new Color(0x769656) : new Color(0xeeeed2));
        square.setCursor(new Cursor(Cursor.HAND_CURSOR));
        square.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleSquareClick(row, col);
            }
        });
        square.setName("sq_" + r + "_" + c);
        return square;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(320, 0));
        panel.setBackground(new Color(0x1e1e1e));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(0x333333)));

        // Header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.setBackground(new Color(0x1e1e1e));
        JLabel aiLabel = new JLabel("CPU Opponent");
        aiLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        aiLabel.setForeground(Color.WHITE);
        header.add(aiLabel);
        panel.add(header);
        panel.add(Box.createVerticalStrut(10));

        // Difficulty section
        JLabel diffLabel = new JLabel("Difficulty");
        diffLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        diffLabel.setForeground(new Color(0xa3a3a3));
        diffLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(diffLabel);
        panel.add(Box.createVerticalStrut(5));

        JPanel diffPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        diffPanel.setBackground(new Color(0x1e1e1e));
        String[] modes = { GREEDY, DIVIDE_AND_CONQUER, BACKTRACKING, DYNAMIC_PROGRAMMING };
        diffButtons = new JButton[4];
        for (int i = 0; i < modes.length; i++) {
            final String mode = modes[i];
            JButton btn = new JButton(mode);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            btn.setBackground(selectedMode.equals(mode) ? new Color(0x769656) : new Color(0x2d2d2d));
            btn.setForeground(Color.WHITE);
            btn.setBorder(BorderFactory.createLineBorder(new Color(0x444444)));
            btn.setFocusPainted(false);
            btn.addActionListener(e -> selectDifficulty(mode));
            diffButtons[i] = btn;
            diffPanel.add(btn);
        }
        panel.add(diffPanel);
        panel.add(Box.createVerticalStrut(20));

        // Move History
        JLabel histLabel = new JLabel("Move History");
        histLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        histLabel.setForeground(new Color(0xa3a3a3));
        histLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(histLabel);
        panel.add(Box.createVerticalStrut(5));

        moveHistoryArea = new JTextArea(15, 25);
        moveHistoryArea.setEditable(false);
        moveHistoryArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        moveHistoryArea.setBackground(new Color(0x2d2d2d));
        moveHistoryArea.setForeground(Color.WHITE);
        moveHistoryArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane scrollPane = new JScrollPane(moveHistoryArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0x444444)));
        panel.add(scrollPane);
        panel.add(Box.createVerticalStrut(15));

        // Undo and New Game buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        buttonPanel.setBackground(new Color(0x1e1e1e));
        undoButton = new JButton("Undo Move");
        undoButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        undoButton.setBackground(new Color(0x2d2d2d));
        undoButton.setForeground(Color.WHITE);
        undoButton.setFocusPainted(false);
        undoButton.setEnabled(false);
        undoButton.addActionListener(e -> undoMove());
        buttonPanel.add(undoButton);
        JButton newGameBtn = new JButton("New Game");
        newGameBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        newGameBtn.setBackground(new Color(0x769656));
        newGameBtn.setForeground(Color.WHITE);
        newGameBtn.setFocusPainted(false);
        newGameBtn.addActionListener(e -> resetGame());
        buttonPanel.add(newGameBtn);
        panel.add(buttonPanel);

        return panel;
    }

    private void selectDifficulty(String mode) {
        if (isGameInProgress()) return;
        selectedMode = mode;
        updateDifficultyButtons();
    }

    private boolean isGameInProgress() {
        return !gameState.history.isEmpty() && !gameOver;
    }

    private void updateDifficultyButtons() {
        boolean locked = isGameInProgress();
        String[] modes = { GREEDY, DIVIDE_AND_CONQUER, BACKTRACKING, DYNAMIC_PROGRAMMING };
        for (int i = 0; i < diffButtons.length; i++) {
            diffButtons[i].setEnabled(!locked);
            diffButtons[i].setBackground(selectedMode.equals(modes[i]) ? new Color(0x769656) : new Color(0x2d2d2d));
        }
    }

    private void handleSquareClick(int r, int c) {
        if (gameOver) return;
        if (gameState.turn != P1) return;

        List<Move> legalMoves = gameState.getLegalMovesForPlayer(P1);

        if (selectedRow >= 0 && selectedCol >= 0) {
            if (gameState.board[r][c] == EMPTY) {
                Move move = findMove(legalMoves, selectedRow, selectedCol, r, c);
                if (move != null) {
                    applyMove(move);
                    selectedRow = -1;
                    selectedCol = -1;
                    repaintBoard();
                    scheduleAIMove();
                    return;
                }
            }
            selectedRow = -1;
            selectedCol = -1;
        }

        if (gameState.isCurrentPlayerPiece(r, c)) {
            boolean canMove = legalMoves.stream().anyMatch(m -> m.from.r == r && m.from.c == c);
            if (canMove) {
                selectedRow = r;
                selectedCol = c;
            }
        }
        repaintBoard();
    }

    private Move findMove(List<Move> moves, int fromR, int fromC, int toR, int toC) {
        for (Move m : moves) {
            if (m.from.r == fromR && m.from.c == fromC && m.to.r == toR && m.to.c == toC)
                return m;
        }
        return null;
    }

    private void applyMove(Move move) {
        stateHistory.add(gameState);
        gameState = gameState.applyMove(move);
        appendMoveNotation(move);
        updateDifficultyButtons();
        updateUndoButton();
        checkGameOver();
    }

    private void appendMoveNotation(Move move) {
        String fromStr = "" + (char) ('a' + move.from.c) + (8 - move.from.r);
        String toStr = "" + (char) ('a' + move.to.c) + (8 - move.to.r);
        String sep = move.type.equals("jump") ? "x" : "-";
        moveHistoryArea.append(fromStr + sep + toStr + "\n");
    }

    private void checkGameOver() {
        List<Move> p1Moves = gameState.getLegalMovesForPlayer(P1);
        List<Move> p2Moves = gameState.getLegalMovesForPlayer(P2);
        if (p1Moves.isEmpty() || p2Moves.isEmpty()) {
            gameOver = true;
            String winner = p2Moves.isEmpty() ? "You win!" : "CPU wins!";
            JOptionPane.showMessageDialog(this, "Game Over. " + winner);
        }
    }

    private void scheduleAIMove() {
        if (gameOver || gameState.turn != P2) return;

        SwingUtilities.invokeLater(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {}
            SwingUtilities.invokeLater(this::executeAIMove);
        });
    }

    private void executeAIMove() {
        if (gameOver || gameState.turn != P2) return;

        Move aiMove = getAIMove();
        if (aiMove != null) {
            applyMove(aiMove);
        } else {
            gameOver = true;
            JOptionPane.showMessageDialog(this, "Game Over. You win! (CPU has no moves)");
        }
        repaintBoard();
    }

    /**
     * Calls the corresponding algorithm based on selected mode.
     */
    private Move getAIMove() {
        switch (selectedMode) {
            case GREEDY:
                return Greedy.getBestMove(gameState);
            case DIVIDE_AND_CONQUER:
                return DivideAndConquer.getBestMove(gameState);
            case BACKTRACKING:
                return Backtracking.getBestMove(gameState);
            case DYNAMIC_PROGRAMMING:
                return DynamicProgramming.getBestMove(gameState);
            default:
                return Greedy.getBestMove(gameState);
        }
    }

    private void repaintBoard() {
        if (boardPanel.getComponentCount() == 0) return;
        JPanel grid = (JPanel) boardPanel.getComponent(0);
        grid.removeAll();

        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                JPanel square = createSquareWithPiece(r, c);
                grid.add(square);
            }
        }
        grid.revalidate();
        grid.repaint();
    }

    private JPanel createSquareWithPiece(int r, int c) {
        final int row = r, col = c;
        JPanel square = new JPanel(new GridBagLayout());
        square.setPreferredSize(new Dimension(SQUARE_SIZE, SQUARE_SIZE));
        boolean isDark = (r + c) % 2 != 0;
        Color baseColor = isDark ? new Color(0x769656) : new Color(0xeeeed2);

        boolean isSelected = (selectedRow == r && selectedCol == c);
        boolean isValidDest = false;
        if (selectedRow >= 0 && selectedCol >= 0) {
            List<Move> moves = gameState.getLegalMovesForPlayer(P1);
            for (Move m : moves) {
                if (m.from.r == selectedRow && m.from.c == selectedCol && m.to.r == r && m.to.c == c) {
                    isValidDest = true;
                    break;
                }
            }
        }

        if (isSelected) square.setBackground(new Color(255, 255, 100, 180));
        else if (isValidDest) square.setBackground(new Color(255, 255, 150, 150));
        else square.setBackground(baseColor);

        square.setCursor(new Cursor(Cursor.HAND_CURSOR));
        square.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleSquareClick(row, col);
            }
        });

        int piece = gameState.board[r][c];
        if (piece != EMPTY) {
            CirclePiece pieceComp = new CirclePiece(piece);
            square.add(pieceComp);
        }
        return square;
    }

    private static class CirclePiece extends JComponent {
        private final int piece;

        CirclePiece(int piece) {
            this.piece = piece;
            setPreferredSize(new Dimension(SQUARE_SIZE - 12, SQUARE_SIZE - 12));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int size = Math.min(w, h);
            int x = (w - size) / 2;
            int y = (h - size) / 2;

            boolean isWhite = piece == P1 || piece == P1_KING;
            g2.setColor(isWhite ? new Color(0xf0f0f0) : new Color(0x2b2b2b));
            g2.fillOval(x, y, size, size);
            g2.setColor(isWhite ? Color.LIGHT_GRAY : Color.DARK_GRAY);
            g2.drawOval(x, y, size, size);

            if (piece == P1_KING || piece == P2_KING) {
                g2.setColor(new Color(0xffd700));
                g2.setFont(new Font("Segoe UI", Font.BOLD, size / 2));
                FontMetrics fm = g2.getFontMetrics();
                String crown = "*";
                int tx = x + (size - fm.stringWidth(crown)) / 2;
                int ty = y + (size + fm.getAscent()) / 2 - fm.getDescent();
                g2.drawString(crown, tx, ty);
            }
        }
    }

    private void undoMove() {
        if (stateHistory.isEmpty()) return;
        gameState = stateHistory.remove(stateHistory.size() - 1);
        gameOver = false;
        selectedRow = -1;
        selectedCol = -1;
        refreshMoveHistoryDisplay();
        updateDifficultyButtons();
        updateUndoButton();
        repaintBoard();
    }

    private void refreshMoveHistoryDisplay() {
        moveHistoryArea.setText("");
        for (Move m : gameState.history) {
            String fromStr = "" + (char) ('a' + m.from.c) + (8 - m.from.r);
            String toStr = "" + (char) ('a' + m.to.c) + (8 - m.to.r);
            String sep = m.type.equals("jump") ? "x" : "-";
            moveHistoryArea.append(fromStr + sep + toStr + "\n");
        }
    }

    private void updateUndoButton() {
        undoButton.setEnabled(!stateHistory.isEmpty());
    }

    private void resetGame() {
        gameState = new GameState();
        stateHistory.clear();
        gameOver = false;
        selectedRow = -1;
        selectedCol = -1;
        moveHistoryArea.setText("");
        updateDifficultyButtons();
        updateUndoButton();
        repaintBoard();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UI());
    }

    // --- Game State and Move classes (shared with algorithms) ---

    public static class Move {
        public String type;
        public Cell from, to;
        public List<Cell> captured;

        public Move(String type, int fromR, int fromC, int toR, int toC, List<Cell> captured) {
            this.type = type;
            this.from = new Cell(fromR, fromC);
            this.to = new Cell(toR, toC);
            this.captured = captured != null ? captured : new ArrayList<>();
        }
    }

    public static class Cell {
        public int r, c;
        public Cell(int r, int c) { this.r = r; this.c = c; }
    }

    public static class GameState {
        public int[][] board;
        public int turn;
        public List<Move> history;

        private static final int[][] INITIAL_BOARD = {
            {0, P2, 0, P2, 0, P2, 0, P2},
            {P2, 0, P2, 0, P2, 0, P2, 0},
            {0, P2, 0, P2, 0, P2, 0, P2},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0},
            {P1, 0, P1, 0, P1, 0, P1, 0},
            {0, P1, 0, P1, 0, P1, 0, P1},
            {P1, 0, P1, 0, P1, 0, P1, 0}
        };

        public GameState() {
            this.board = deepCopy(INITIAL_BOARD);
            this.turn = P1;
            this.history = new ArrayList<>();
        }

        public GameState(int[][] board, int turn, List<Move> history) {
            this.board = deepCopy(board);
            this.turn = turn;
            this.history = new ArrayList<>(history);
        }

        public GameState clone() {
            return new GameState(board, turn, history);
        }

        public boolean isP1(int piece) { return piece == P1 || piece == P1_KING; }
        public boolean isP2(int piece) { return piece == P2 || piece == P2_KING; }
        public boolean isKing(int piece) { return piece == P1_KING || piece == P2_KING; }

        public boolean isCurrentPlayerPiece(int r, int c) {
            int piece = board[r][c];
            if (turn == P1) return isP1(piece);
            return isP2(piece);
        }

        public boolean inBounds(int r, int c) {
            return r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE;
        }

        public int[][] getForwardDirs(int piece) {
            if (isKing(piece)) return new int[][]{{-1,-1},{-1,1},{1,-1},{1,1}};
            if (isP1(piece)) return new int[][]{{-1,-1},{-1,1}};
            if (isP2(piece)) return new int[][]{{1,-1},{1,1}};
            return new int[0][0];
        }

        public List<Move> getLegalMovesForPlayer(int player) {
            List<Move> jumps = new ArrayList<>();
            List<Move> steps = new ArrayList<>();

            for (int r = 0; r < BOARD_SIZE; r++) {
                for (int c = 0; c < BOARD_SIZE; c++) {
                    int piece = board[r][c];
                    if (player == P1 && !isP1(piece)) continue;
                    if (player == P2 && !isP2(piece)) continue;

                    List<Move> pieceJumps = getPieceJumpsFrom(r, c, piece);
                    if (!pieceJumps.isEmpty()) {
                        jumps.addAll(pieceJumps);
                    } else if (jumps.isEmpty()) {
                        steps.addAll(getPieceStepsFrom(r, c, piece));
                    }
                }
            }
            return jumps.isEmpty() ? steps : jumps;
        }

        private List<Move> getPieceStepsFrom(int fromR, int fromC, int piece) {
            List<Move> valid = new ArrayList<>();
            for (int[] d : getForwardDirs(piece)) {
                int toR = fromR + d[0], toC = fromC + d[1];
                if (inBounds(toR, toC) && board[toR][toC] == EMPTY) {
                    valid.add(new Move("step", fromR, fromC, toR, toC, null));
                }
            }
            return valid;
        }

        private List<Move> getPieceJumpsFrom(int r, int c, int piece) {
            List<Move> allJumps = new ArrayList<>();
            JumpPath path = new JumpPath(r, c);
            backtrackJumps(r, c, piece, path, allJumps);
            return allJumps;
        }

        private void backtrackJumps(int r, int c, int piece, JumpPath path, List<Move> allJumps) {
            boolean foundJump = false;
            for (int[] d : getForwardDirs(piece)) {
                int overR = r + d[0], overC = c + d[1];
                int landR = r + 2*d[0], landC = c + 2*d[1];
                if (!inBounds(landR, landC)) continue;

                int overPiece = board[overR][overC];
                boolean isOpponent = (turn == P1 && isP2(overPiece)) || (turn == P2 && isP1(overPiece));
                boolean alreadyCaptured = path.captured.stream().anyMatch(cell -> cell.r == overR && cell.c == overC);

                if (isOpponent && !alreadyCaptured && board[landR][landC] == EMPTY) {
                    foundJump = true;
                    JumpPath newPath = path.extend(landR, landC, overR, overC);
                    boolean willCrown = (piece == P1 && landR == 0) || (piece == P2 && landR == BOARD_SIZE - 1);
                    if (willCrown) {
                        allJumps.add(newPath.toMove());
                    } else {
                        backtrackJumps(landR, landC, piece, newPath, allJumps);
                    }
                }
            }
            if (!foundJump && !path.captured.isEmpty()) {
                allJumps.add(path.toMove());
            }
        }

        public GameState applyMove(Move move) {
            GameState next = clone();
            int piece = next.board[move.from.r][move.from.c];

            next.board[move.from.r][move.from.c] = EMPTY;
            next.board[move.to.r][move.to.c] = piece;

            for (Cell cap : move.captured) {
                next.board[cap.r][cap.c] = EMPTY;
            }

            if (piece == P1 && move.to.r == 0) next.board[move.to.r][move.to.c] = P1_KING;
            else if (piece == P2 && move.to.r == BOARD_SIZE - 1) next.board[move.to.r][move.to.c] = P2_KING;

            next.turn = (next.turn == P1) ? P2 : P1;
            next.history.add(move);
            return next;
        }

        private static int[][] deepCopy(int[][] arr) {
            int[][] copy = new int[arr.length][];
            for (int i = 0; i < arr.length; i++) copy[i] = arr[i].clone();
            return copy;
        }
    }

    private static class JumpPath {
        int fromR, fromC;
        List<Cell> captured = new ArrayList<>();
        List<Cell> path = new ArrayList<>();

        JumpPath(int r, int c) { fromR = r; fromC = c; }

        JumpPath extend(int landR, int landC, int capR, int capC) {
            JumpPath p = new JumpPath(fromR, fromC);
            p.captured.addAll(captured);
            p.captured.add(new Cell(capR, capC));
            p.path.addAll(path);
            p.path.add(new Cell(landR, landC));
            return p;
        }

        Move toMove() {
            if (path.isEmpty()) return null;
            Cell last = path.get(path.size() - 1);
            return new Move("jump", fromR, fromC, last.r, last.c, captured);
        }
    }
}
