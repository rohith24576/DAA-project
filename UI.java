import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Main Checkers application with graphical UI.
 */
public class UI extends JFrame {

    public static final int BOARD_SIZE = 8;
    public static final int P1 = 1;
    public static final int P1_KING = 2;
    public static final int P2 = -1;
    public static final int P2_KING = -2;
    public static final int EMPTY = 0;

    public static final String GREEDY = "Greedy";
    public static final String DIVIDE_AND_CONQUER = "Divide and Conquer";
    public static final String BACKTRACKING = "Backtracking";
    public static final String DYNAMIC_PROGRAMMING = "Dynamic Programming";

    private GameState gameState;
    private String selectedMode = GREEDY;
    private int selectedRow = -1, selectedCol = -1;
    private JPanel boardPanel;
    private JButton[] diffButtons;
    private JButton undoButton;
    private JTextArea moveHistoryArea;
    private boolean gameOver = false;
    private static final int SQUARE_SIZE = 80;
    private List<GameState> stateHistory = new ArrayList<>();

    private static final Color BG_DARK = new Color(0x2c2c2c);
    private static final Color BG_PANEL = new Color(0x3a3a3a);
    private static final Color BG_CARD = new Color(0x404040);
    private static final Color SUCCESS = new Color(0x4a7c4e);
    private static final Color TEXT_PRIMARY = new Color(0xf5f5f5);
    private static final Color TEXT_MUTED = new Color(0xaaaaaa);
    private static final Color DARK_SQ = new Color(0x8b4513);
    private static final Color LIGHT_SQ = new Color(0xdeb887);
    private static final Color PIECE_RED = new Color(0xc41e3a);
    private static final Color PIECE_BLACK = new Color(0x1a1a1a);
    private static final Color KING_GOLD = new Color(0xd4af37);
    private static final Color BOARD_BORDER = new Color(0x2d5a27);

    public UI() {
        setTitle("Checkers - DAA Project");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_DARK);

        gameState = new GameState();

        JPanel leftPanel = createLeftPanel();
        add(leftPanel, BorderLayout.WEST);

        JPanel centerPanel = createCenterPanel();
        add(centerPanel, BorderLayout.CENTER);

        JPanel rightPanel = createRightPanel();
        add(rightPanel, BorderLayout.EAST);

        setSize(1200, 800);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createLeftPanel() {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(200, 0));
        panel.setBackground(BG_DARK);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(24, 24, 24, 0));

        JLabel logo = new JLabel("CHECKERS");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        logo.setForeground(TEXT_PRIMARY);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(logo);

        panel.add(Box.createVerticalStrut(8));

        JLabel sub = new JLabel("DAA Project");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(TEXT_MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(sub);

        panel.add(Box.createVerticalStrut(40));

        JLabel turnLabel = new JLabel("Turn");
        turnLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        turnLabel.setForeground(TEXT_MUTED);
        turnLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(turnLabel);

        JPanel turnIndicator = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        turnIndicator.setBackground(BG_DARK);
        turnIndicator.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel turnDot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int s = 12;
                g2.setColor(gameState.turn == P1 ? PIECE_RED : PIECE_BLACK);
                g2.fillOval(0, 0, s, s);
                if (gameOver) g2.setColor(TEXT_MUTED);
            }
        };
        turnDot.setPreferredSize(new Dimension(12, 12));
        turnDot.setOpaque(false);
        turnDot.setBackground(BG_DARK);
        turnIndicator.add(turnDot);
        JLabel turnText = new JLabel("Your turn");
        turnText.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        turnText.setForeground(TEXT_PRIMARY);
        turnIndicator.add(turnText);
        panel.add(turnIndicator);

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel boardFrame = new JPanel(new BorderLayout());
        boardFrame.setBackground(BOARD_BORDER);
        boardFrame.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BOARD_BORDER, 4),
            new EmptyBorder(12, 12, 12, 12)
        ));

        int boardPixels = BOARD_SIZE * SQUARE_SIZE;
        JPanel grid = new JPanel(new GridLayout(BOARD_SIZE, BOARD_SIZE, 0, 0));
        grid.setPreferredSize(new Dimension(boardPixels, boardPixels));
        grid.setMinimumSize(new Dimension(boardPixels, boardPixels));
        grid.setBackground(DARK_SQ);
        grid.setOpaque(true);

        boardPanel = new JPanel(new BorderLayout());
        boardPanel.setOpaque(true);
        boardPanel.setBackground(BOARD_BORDER);
        boardPanel.add(grid, BorderLayout.CENTER);

        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                grid.add(createSquare(r, c));
            }
        }

        boardFrame.add(boardPanel, BorderLayout.CENTER);
        panel.add(boardFrame, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSquare(int r, int c) {
        final int row = r, col = c;
        boolean isDark = (r + c) % 2 != 0;
        JPanel square = new JPanel(new GridBagLayout());
        square.setPreferredSize(new Dimension(SQUARE_SIZE, SQUARE_SIZE));
        square.setMinimumSize(new Dimension(SQUARE_SIZE, SQUARE_SIZE));
        square.setBackground(isDark ? DARK_SQ : LIGHT_SQ);
        square.setOpaque(true);
        square.setCursor(new Cursor(Cursor.HAND_CURSOR));
        square.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleSquareClick(row, col);
            }
        });

        int piece = gameState.board[r][c];
        if (piece != EMPTY) {
            square.add(new CheckerPiece(piece));
        }
        return square;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(300, 0));
        panel.setBackground(BG_DARK);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel cpuLabel = new JLabel("CPU Opponent");
        cpuLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cpuLabel.setForeground(TEXT_PRIMARY);
        cpuLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(cpuLabel);
        panel.add(Box.createVerticalStrut(12));

        JLabel diffLabel = new JLabel("Difficulty");
        diffLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        diffLabel.setForeground(TEXT_MUTED);
        diffLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(diffLabel);
        panel.add(Box.createVerticalStrut(8));

        JPanel diffPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        diffPanel.setBackground(BG_DARK);
        String[] modes = { GREEDY, DIVIDE_AND_CONQUER, BACKTRACKING, DYNAMIC_PROGRAMMING };
        diffButtons = new JButton[4];
        for (int i = 0; i < modes.length; i++) {
            final String mode = modes[i];
            JButton btn = createDiffButton(mode);
            btn.addActionListener(e -> selectDifficulty(mode));
            diffButtons[i] = btn;
            diffPanel.add(btn);
        }
        panel.add(diffPanel);
        panel.add(Box.createVerticalStrut(24));

        JLabel histLabel = new JLabel("Move History");
        histLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        histLabel.setForeground(TEXT_MUTED);
        histLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(histLabel);
        panel.add(Box.createVerticalStrut(8));

        JTextArea moveHistoryArea = new JTextArea(12, 22);
        moveHistoryArea.setEditable(false);
        moveHistoryArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        moveHistoryArea.setBackground(BG_CARD);
        moveHistoryArea.setForeground(TEXT_PRIMARY);
        moveHistoryArea.setCaretColor(TEXT_PRIMARY);
        moveHistoryArea.setBorder(new EmptyBorder(12, 12, 12, 12));
        moveHistoryArea.setMargin(new Insets(8, 8, 8, 8));
        JScrollPane scroll = new JScrollPane(moveHistoryArea);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0x30363d), 1));
        scroll.setBackground(BG_DARK);
        panel.add(scroll);
        panel.add(Box.createVerticalStrut(20));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnPanel.setBackground(BG_DARK);
        undoButton = createStyledButton("Undo", false);
        undoButton.setEnabled(false);
        undoButton.addActionListener(e -> undoMove());
        JButton newBtn = createStyledButton("New Game", true);
        newBtn.addActionListener(e -> resetGame());
        btnPanel.add(undoButton);
        btnPanel.add(Box.createHorizontalStrut(8));
        btnPanel.add(newBtn);
        panel.add(btnPanel);

        this.moveHistoryArea = moveHistoryArea;
        return panel;
    }

    private JButton createDiffButton(String mode) {
        return createStyledButton(mode, () -> selectedMode.equals(mode));
    }

    private JButton createStyledButton(String text, boolean primary) {
        return createStyledButton(text, () -> primary);
    }

    private JButton createStyledButton(String text, java.util.function.BooleanSupplier isPrimary) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                boolean primary = isPrimary.getAsBoolean();
                Color bg = primary ? SUCCESS : BG_CARD;
                if (getModel().isPressed()) bg = bg.darker();
                else if (getModel().isRollover() && isEnabled()) bg = primary ? SUCCESS.brighter() : new Color(0x30363d);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setForeground(TEXT_PRIMARY);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorder(new EmptyBorder(10, 16, 10, 16));
        return btn;
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
        for (JButton b : diffButtons) {
            b.setEnabled(!locked);
            b.repaint();
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
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
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

    private Move getAIMove() {
        switch (selectedMode) {
            case GREEDY: return Greedy.getBestMove(gameState);
            case DIVIDE_AND_CONQUER: return DivideAndConquer.getBestMove(gameState);
            case BACKTRACKING: return Backtracking.getBestMove(gameState);
            case DYNAMIC_PROGRAMMING: return DynamicProgramming.getBestMove(gameState);
            default: return Greedy.getBestMove(gameState);
        }
    }

    private void repaintBoard() {
        Component grid = null;
        for (Component c : boardPanel.getComponents()) {
            if (c instanceof JPanel) {
                grid = c;
                break;
            }
        }
        if (grid instanceof JPanel) {
            JPanel g = (JPanel) grid;
            g.removeAll();
            for (int r = 0; r < BOARD_SIZE; r++) {
                for (int c = 0; c < BOARD_SIZE; c++) {
                    g.add(createSquareWithPiece(r, c));
                }
            }
            g.revalidate();
            g.repaint();
        }
    }

    private JPanel createSquareWithPiece(int r, int c) {
        final int row = r, col = c;
        boolean isDark = (r + c) % 2 != 0;
        Color base = isDark ? DARK_SQ : LIGHT_SQ;

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

        JPanel square = new JPanel(new GridBagLayout());
        square.setPreferredSize(new Dimension(SQUARE_SIZE, SQUARE_SIZE));
        square.setMinimumSize(new Dimension(SQUARE_SIZE, SQUARE_SIZE));
        if (isSelected) square.setBorder(BorderFactory.createLineBorder(new Color(255, 215, 0), 3));
        else square.setBorder(null);
        if (isSelected) square.setBackground(new Color(255, 255, 200));
        else if (isValidDest) square.setBackground(new Color(200, 255, 200));
        else square.setBackground(base);
        square.setOpaque(true);
        square.setCursor(new Cursor(Cursor.HAND_CURSOR));
        square.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleSquareClick(row, col);
            }
        });

        int piece = gameState.board[r][c];
        if (piece != EMPTY) {
            square.add(new CheckerPiece(piece));
        }
        return square;
    }

    private static class CheckerPiece extends JComponent {
        private final int piece;

        CheckerPiece(int piece) {
            this.piece = piece;
            setPreferredSize(new Dimension(SQUARE_SIZE - 16, SQUARE_SIZE - 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            int w = getWidth();
            int h = getHeight();
            int size = Math.min(w, h);
            int x = (w - size) / 2;
            int y = (h - size) / 2;

            boolean isRed = piece == P1 || piece == P1_KING;
            Color fill = isRed ? PIECE_RED : PIECE_BLACK;
            Color edge = isRed ? new Color(0xa01830) : new Color(0x333333);

            GradientPaint gp = new GradientPaint(x, y, fill, x + size, y + size,
                isRed ? new Color(0xe03040) : new Color(0x2a2a2a));
            g2.setPaint(gp);
            g2.fillOval(x + 2, y + 2, size - 4, size - 4);
            g2.setColor(edge);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(x + 2, y + 2, size - 4, size - 4);

            if (piece == P1_KING || piece == P2_KING) {
                g2.setColor(KING_GOLD);
                g2.setFont(new Font("Segoe UI", Font.BOLD, size / 2));
                FontMetrics fm = g2.getFontMetrics();
                String crown = "K";
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
                    if (!pieceJumps.isEmpty()) jumps.addAll(pieceJumps);
                    else if (jumps.isEmpty()) steps.addAll(getPieceStepsFrom(r, c, piece));
                }
            }
            return jumps.isEmpty() ? steps : jumps;
        }

        private List<Move> getPieceStepsFrom(int fromR, int fromC, int piece) {
            List<Move> valid = new ArrayList<>();
            for (int[] d : getForwardDirs(piece)) {
                int toR = fromR + d[0], toC = fromC + d[1];
                if (inBounds(toR, toC) && board[toR][toC] == EMPTY)
                    valid.add(new Move("step", fromR, fromC, toR, toC, null));
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
                    if (willCrown) allJumps.add(newPath.toMove());
                    else backtrackJumps(landR, landC, piece, newPath, allJumps);
                }
            }
            if (!foundJump && !path.captured.isEmpty()) allJumps.add(path.toMove());
        }

        public GameState applyMove(Move move) {
            GameState next = clone();
            int piece = next.board[move.from.r][move.from.c];
            next.board[move.from.r][move.from.c] = EMPTY;
            next.board[move.to.r][move.to.c] = piece;
            for (Cell cap : move.captured) next.board[cap.r][cap.c] = EMPTY;
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
