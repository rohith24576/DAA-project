import java.util.List;

/**
 * Divide and Conquer algorithm for Checkers AI - Easy Mode.
 * Uses minimax-style divide and conquer: recursively divides the game tree,
 * evaluates base cases (leaf nodes), and combines results.
 *
 * Implements: Minimax style decision making with recursive board evaluation.
 * Shallow search depth keeps it easy for the user to win.
 */
public class DivideAndConquer {

    private static final int MAN_VAL = 100;
    private static final int KING_VAL = 300;
    private static final int SEARCH_DEPTH = 2;

    /**
     * Returns the best move using divide-and-conquer minimax.
     * Divides: explore each possible move as a subproblem.
     * Conquer: choose the move with best minimax value.
     *
     * Time Complexity: O(b^d) where b = branching factor, d = search depth (2).
     *
     * @param state current game state
     * @return best move, or null if no moves
     */
    public static UI.Move getBestMove(UI.GameState state) {
        List<UI.Move> moves = state.getLegalMovesForPlayer(UI.P2);
        if (moves.isEmpty()) return null;
        if (moves.size() == 1) return moves.get(0);

        UI.Move bestMove = moves.get(0);
        int bestScore = Integer.MIN_VALUE;

        for (UI.Move move : moves) {
            UI.GameState nextState = state.applyMove(move);
            int score = minimax(nextState, SEARCH_DEPTH - 1, false);
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    /**
     * Minimax: divide game tree recursively, conquer by choosing min/max.
     * P2 (AI) maximizes, P1 (Human) minimizes.
     *
     * Time Complexity: O(b^d) where b = branching factor, d = depth.
     * With depth 2 and ~10 moves/position: O(100) approximately.
     */
    private static int minimax(UI.GameState state, int depth, boolean isMaximizing) {
        List<UI.Move> moves = state.getLegalMovesForPlayer(state.turn);
        if (depth == 0 || moves.isEmpty()) {
            return evaluate(state);
        }

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (UI.Move move : moves) {
                UI.GameState next = state.applyMove(move);
                int eval = minimax(next, depth - 1, false);
                maxEval = Math.max(maxEval, eval);
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (UI.Move move : moves) {
                UI.GameState next = state.applyMove(move);
                int eval = minimax(next, depth - 1, true);
                minEval = Math.min(minEval, eval);
            }
            return minEval;
        }
    }

    /**
     * Static evaluation of board from P2 (AI) perspective.
     * Time Complexity: O(n^2) = O(64) for fixed 8x8 board.
     */
    private static int evaluate(UI.GameState state) {
        int p1Score = 0, p2Score = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.EMPTY) continue;

                int centerBonus = (r >= 3 && r <= 4 && c >= 3 && c <= 4) ? 15 : 0;
                if (piece == UI.P1) p1Score += MAN_VAL + centerBonus;
                else if (piece == UI.P1_KING) p1Score += KING_VAL + centerBonus;
                else if (piece == UI.P2) p2Score += MAN_VAL + centerBonus;
                else if (piece == UI.P2_KING) p2Score += KING_VAL + centerBonus;
            }
        }
        return p2Score - p1Score;
    }
}
