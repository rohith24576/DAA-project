import java.util.ArrayList;
import java.util.List;

/**
 * Divide and Conquer algorithm for Checkers - Easy Mode.
 * Minimax with alpha-beta pruning and move ordering.
 * Divides: each move is a subproblem. Conquers: combine via min/max.
 *
 * Implements: Minimax, binary-search style divide strategy, recursive board evaluation.
 */
public class DivideAndConquer {

    private static final int MAN_VAL = 100;
    private static final int KING_VAL = 320;
    private static final int SEARCH_DEPTH = 3;

    /**
     * Returns the best move using divide-and-conquer minimax with alpha-beta.
     *
     * Time Complexity: O(b^d) worst case; alpha-beta reduces effective branching.
     *
     * @param state current game state
     * @return best move, or null if no moves
     */
    public static UI.Move getBestMove(UI.GameState state) {
        List<UI.Move> moves = state.getLegalMovesForPlayer(UI.P2);
        if (moves.isEmpty()) return null;
        if (moves.size() == 1) return moves.get(0);

        List<UI.Move> ordered = orderMoves(moves, state);
        UI.Move bestMove = ordered.get(0);
        int bestScore = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        for (UI.Move move : ordered) {
            UI.GameState nextState = state.applyMove(move);
            int score = minimax(nextState, SEARCH_DEPTH - 1, alpha, beta, false);
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
            alpha = Math.max(alpha, bestScore);
            if (beta <= alpha) break;
        }
        return bestMove;
    }

    /**
     * Minimax with alpha-beta pruning. P2 maximizes, P1 minimizes.
     * Time Complexity: O(b^d) with pruning reducing effective b.
     */
    private static int minimax(UI.GameState state, int depth, int alpha, int beta, boolean isMaximizing) {
        List<UI.Move> moves = state.getLegalMovesForPlayer(state.turn);
        if (depth == 0 || moves.isEmpty()) {
            return evaluate(state);
        }

        List<UI.Move> ordered = orderMoves(moves, state);

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (UI.Move move : ordered) {
                UI.GameState next = state.applyMove(move);
                int eval = minimax(next, depth - 1, alpha, beta, false);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (UI.Move move : ordered) {
                UI.GameState next = state.applyMove(move);
                int eval = minimax(next, depth - 1, alpha, beta, true);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

    private static List<UI.Move> orderMoves(List<UI.Move> moves, UI.GameState state) {
        List<UI.Move> result = new ArrayList<>(moves);
        result.sort((a, b) -> {
            int jumpA = a.type.equals("jump") ? 500 : 0;
            int jumpB = b.type.equals("jump") ? 500 : 0;
            UI.GameState nextA = state.applyMove(a);
            UI.GameState nextB = state.applyMove(b);
            return Integer.compare(evaluate(nextB) + jumpB, evaluate(nextA) + jumpA);
        });
        return result;
    }

    /**
     * Enhanced evaluation: piece value, position, advancement, back row.
     * Time Complexity: O(n^2) for fixed board.
     */
    private static int evaluate(UI.GameState state) {
        int p1Score = 0, p2Score = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.EMPTY) continue;

                int centerBonus = (r >= 3 && r <= 4 && c >= 3 && c <= 4) ? 18 : 0;
                int p1Back = (piece == UI.P1 && r == 7) ? 22 : 0;
                int p2Back = (piece == UI.P2 && r == 0) ? 22 : 0;
                int p2Advance = (piece == UI.P2) ? (7 - r) * 7 : 0;
                int p1Advance = (piece == UI.P1) ? r * 7 : 0;

                if (piece == UI.P1) p1Score += MAN_VAL + centerBonus + p1Back + p1Advance;
                else if (piece == UI.P1_KING) p1Score += KING_VAL + centerBonus;
                else if (piece == UI.P2) p2Score += MAN_VAL + centerBonus + p2Back + p2Advance;
                else if (piece == UI.P2_KING) p2Score += KING_VAL + centerBonus;
            }
        }
        return p2Score - p1Score;
    }
}
