import java.util.List;

/**
 * Greedy algorithm for Checkers AI - Easy Mode.
 * Uses greedy strategy: at each step, selects the move that gives the best
 * immediate evaluation without looking ahead.
 *
 * Implements: Greedy strategy selection / Activity selection style decision making.
 * The algorithm picks the locally optimal choice at each step.
 */
public class Greedy {

    private static final int MAN_VAL = 100;
    private static final int KING_VAL = 300;

    /**
     * Returns the best move using greedy strategy.
     * Picks the move that maximizes immediate board evaluation.
     *
     * Time Complexity: O(m) where m = number of legal moves; each iteration does O(1) eval for fixed board.
     *
     * @param state current game state
     * @return best move according to greedy heuristic, or null if no moves
     */
    public static UI.Move getBestMove(UI.GameState state) {
        List<UI.Move> moves = state.getLegalMovesForPlayer(UI.P2);
        if (moves.isEmpty()) return null;
        if (moves.size() == 1) return moves.get(0);

        UI.Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        for (UI.Move move : moves) {
            UI.GameState nextState = state.applyMove(move);
            int score = evaluate(nextState);
            if (move.type.equals("jump")) score += 500;
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    /**
     * Static evaluation: positive = good for P2 (AI), negative = good for P1 (Human).
     * Uses simple piece count and positional bonuses.
     *
     * Time Complexity: O(n^2) where n = BOARD_SIZE (8), so O(64) = O(1) for fixed board
     */
    private static int evaluate(UI.GameState state) {
        int p1Score = 0, p2Score = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.EMPTY) continue;

                int centerBonus = (r >= 3 && r <= 4 && c >= 3 && c <= 4) ? 15 : 0;
                int p1Back = (piece == UI.P1 && r == 7) ? 20 : 0;
                int p2Back = (piece == UI.P2 && r == 0) ? 20 : 0;

                if (piece == UI.P1) p1Score += MAN_VAL + centerBonus + p1Back;
                else if (piece == UI.P1_KING) p1Score += KING_VAL + centerBonus;
                else if (piece == UI.P2) p2Score += MAN_VAL + centerBonus + p2Back;
                else if (piece == UI.P2_KING) p2Score += KING_VAL + centerBonus;
            }
        }
        return p2Score - p1Score;
    }
}
