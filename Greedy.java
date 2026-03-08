import java.util.ArrayList;
import java.util.List;

/**
 * Greedy algorithm for Checkers - Easy Mode.
 * Uses greedy strategy with enhanced heuristics and 2-ply lookahead.
 * Activity selection: at each step, considers opponent's best response.
 *
 * Implements: Greedy strategy selection, activity selection, coin-change style prioritization.
 */
public class Greedy {

    private static final int MAN_VAL = 100;
    private static final int KING_VAL = 320;
    private static final int CAPTURE_BONUS = 600;
    private static final int LOOKAHEAD_PLY = 1;

    /**
     * Returns the best move using greedy strategy with lookahead.
     * Evaluates each move and simulates opponent's best reply for robustness.
     *
     * Time Complexity: O(m * m') where m = our moves, m' = opponent moves per position.
     *
     * @param state current game state
     * @return best move according to greedy heuristic, or null if no moves
     */
    public static UI.Move getBestMove(UI.GameState state) {
        List<UI.Move> moves = state.getLegalMovesForPlayer(UI.P2);
        if (moves.isEmpty()) return null;
        if (moves.size() == 1) return moves.get(0);

        List<UI.Move> ordered = orderMovesByPriority(moves, state);
        UI.Move bestMove = ordered.get(0);
        int bestScore = Integer.MIN_VALUE;

        for (UI.Move move : ordered) {
            UI.GameState nextState = state.applyMove(move);
            int score = evaluateWithLookahead(nextState, LOOKAHEAD_PLY);
            int moveBonus = move.type.equals("jump") ? CAPTURE_BONUS : 0;
            if (score + moveBonus > bestScore) {
                bestScore = score + moveBonus;
                bestMove = move;
            }
        }
        return bestMove;
    }

    /**
     * Greedy lookahead: assume opponent plays best reply, evaluate resulting position.
     * Time Complexity: O(m) where m = opponent's legal moves.
     */
    private static int evaluateWithLookahead(UI.GameState state, int ply) {
        if (ply <= 0) return evaluate(state);

        List<UI.Move> oppMoves = state.getLegalMovesForPlayer(state.turn);
        if (oppMoves.isEmpty()) return evaluate(state);

        int worstForUs = Integer.MAX_VALUE;
        for (UI.Move m : oppMoves) {
            UI.GameState next = state.applyMove(m);
            int score = evaluate(next);
            worstForUs = Math.min(worstForUs, score);
        }
        return worstForUs;
    }

    /**
     * Order moves by greedy priority: captures first, then advancing moves.
     * Time Complexity: O(m log m).
     */
    private static List<UI.Move> orderMovesByPriority(List<UI.Move> moves, UI.GameState state) {
        List<UI.Move> result = new ArrayList<>(moves);
        result.sort((a, b) -> {
            int prioA = (a.type.equals("jump") ? 1000 : 0) + captureCount(a) * 100;
            int prioB = (b.type.equals("jump") ? 1000 : 0) + captureCount(b) * 100;
            if (prioA != prioB) return Integer.compare(prioB, prioA);
            UI.GameState nextA = state.applyMove(a);
            UI.GameState nextB = state.applyMove(b);
            return Integer.compare(evaluate(nextB), evaluate(nextA));
        });
        return result;
    }

    private static int captureCount(UI.Move m) {
        return m.captured != null ? m.captured.size() : 0;
    }

    /**
     * Enhanced evaluation: piece count, position, mobility, vulnerability.
     * Time Complexity: O(n^2) for fixed 8x8 board.
     */
    private static int evaluate(UI.GameState state) {
        int p1Score = 0, p2Score = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.EMPTY) continue;

                int centerBonus = (r >= 2 && r <= 5 && c >= 2 && c <= 5) ? 12 : 0;
                int p1Back = (piece == UI.P1 && r == 7) ? 25 : 0;
                int p2Back = (piece == UI.P2 && r == 0) ? 25 : 0;
                int p2Advance = (piece == UI.P2) ? (7 - r) * 6 : 0;
                int p1Advance = (piece == UI.P1) ? r * 6 : 0;
                int doubleCorner = ((r == 0 || r == 7) && (c == 0 || c == 7)) ? 8 : 0;

                if (piece == UI.P1) p1Score += MAN_VAL + centerBonus + p1Back + p1Advance + doubleCorner;
                else if (piece == UI.P1_KING) p1Score += KING_VAL + centerBonus + doubleCorner;
                else if (piece == UI.P2) p2Score += MAN_VAL + centerBonus + p2Back + p2Advance + doubleCorner;
                else if (piece == UI.P2_KING) p2Score += KING_VAL + centerBonus + doubleCorner;
            }
        }
        int mobility = countMobility(state);
        return p2Score - p1Score + mobility;
    }

    private static int countMobility(UI.GameState state) {
        int p2Moves = state.getLegalMovesForPlayer(UI.P2).size();
        int p1Moves = state.getLegalMovesForPlayer(UI.P1).size();
        return (p2Moves - p1Moves) * 3;
    }
}
