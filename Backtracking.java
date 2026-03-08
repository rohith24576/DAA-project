import java.util.ArrayList;
import java.util.List;

/**
 * Backtracking algorithm for Checkers AI - Medium Mode.
 * Uses state-space search with recursive decision tree exploration.
 * Explores moves, backtracks when a path doesn't lead to improvement.
 *
 * Implements: State-space search, recursive decision tree, constraint checking.
 * Medium difficulty: user can win but must think more.
 */
public class Backtracking {

    private static final int MAN_VAL = 100;
    private static final int KING_VAL = 300;
    private static final int SEARCH_DEPTH = 4;

    /**
     * Returns the best move using backtracking search.
     * Explores the game tree, backtracks on dead ends.
     *
     * Time Complexity: O(b^d) where b = branching factor, d = search depth (4).
     *
     * @param state current game state
     * @return best move, or null if no moves
     */
    public static UI.Move getBestMove(UI.GameState state) {
        List<UI.Move> moves = state.getLegalMovesForPlayer(UI.P2);
        if (moves.isEmpty()) return null;
        if (moves.size() == 1) return moves.get(0);

        int[] bestScore = { Integer.MIN_VALUE };
        UI.Move[] bestMove = { moves.get(0) };

        for (UI.Move move : moves) {
            UI.GameState nextState = state.applyMove(move);
            int score = backtrackSearch(nextState, SEARCH_DEPTH - 1, false);
            if (score > bestScore[0]) {
                bestScore[0] = score;
                bestMove[0] = move;
            }
        }
        return bestMove[0];
    }

    /**
     * Backtracking search: recursively explore state space.
     * At each node, try all moves (branch), evaluate (constraint),
     * backtrack by not selecting worse options.
     *
     * Time Complexity: O(b^d) where b = branching factor, d = depth.
     * With depth 4: explores more of the tree than D&C.
     */
    private static int backtrackSearch(UI.GameState state, int depth, boolean isMaximizing) {
        List<UI.Move> moves = state.getLegalMovesForPlayer(state.turn);

        if (depth == 0 || moves.isEmpty()) {
            return evaluate(state);
        }

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (UI.Move move : moves) {
                UI.GameState next = state.applyMove(move);
                int eval = backtrackSearch(next, depth - 1, false);
                maxEval = Math.max(maxEval, eval);
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (UI.Move move : moves) {
                UI.GameState next = state.applyMove(move);
                int eval = backtrackSearch(next, depth - 1, true);
                minEval = Math.min(minEval, eval);
            }
            return minEval;
        }
    }

    /**
     * Static evaluation with positional heuristics.
     * Time Complexity: O(n^2) for fixed board size.
     */
    private static int evaluate(UI.GameState state) {
        int p1Score = 0, p2Score = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.EMPTY) continue;

                int centerBonus = (r >= 3 && r <= 4 && c >= 3 && c <= 4) ? 15 : 0;
                int advanceBonus = 0;
                if (piece == UI.P2) advanceBonus = (7 - r) * 5;
                else if (piece == UI.P1) advanceBonus = r * 5;

                if (piece == UI.P1) p1Score += MAN_VAL + centerBonus + advanceBonus;
                else if (piece == UI.P1_KING) p1Score += KING_VAL + centerBonus;
                else if (piece == UI.P2) p2Score += MAN_VAL + centerBonus + advanceBonus;
                else if (piece == UI.P2_KING) p2Score += KING_VAL + centerBonus;
            }
        }
        return p2Score - p1Score;
    }
}
