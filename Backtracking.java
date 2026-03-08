import java.util.ArrayList;
import java.util.List;

/**
 * Backtracking algorithm for Checkers - Medium Mode.
 * State-space search with alpha-beta pruning and constraint checking.
 * Explores decision tree, backtracks when branch cannot improve result.
 *
 * Implements: State-space search, recursive decision tree, constraint checking.
 */
public class Backtracking {

    private static final int MAN_VAL = 100;
    private static final int KING_VAL = 330;
    private static final int SEARCH_DEPTH = 5;

    /**
     * Returns the best move using backtracking search with alpha-beta pruning.
     *
     * Time Complexity: O(b^d) with alpha-beta reducing effective branching.
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
            int score = backtrackSearch(nextState, SEARCH_DEPTH - 1, alpha, beta, false);
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
     * Backtracking search with alpha-beta: prune branches that cannot affect result.
     * Time Complexity: O(b^d) with pruning.
     */
    private static int backtrackSearch(UI.GameState state, int depth, int alpha, int beta, boolean isMaximizing) {
        List<UI.Move> moves = state.getLegalMovesForPlayer(state.turn);

        if (depth == 0 || moves.isEmpty()) {
            return evaluate(state);
        }

        List<UI.Move> ordered = orderMoves(moves, state);

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (UI.Move move : ordered) {
                UI.GameState next = state.applyMove(move);
                int eval = backtrackSearch(next, depth - 1, alpha, beta, false);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (UI.Move move : ordered) {
                UI.GameState next = state.applyMove(move);
                int eval = backtrackSearch(next, depth - 1, alpha, beta, true);
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
            int capA = (a.captured != null ? a.captured.size() : 0) * 200;
            int capB = (b.captured != null ? b.captured.size() : 0) * 200;
            int jumpA = a.type.equals("jump") ? 400 : 0;
            int jumpB = b.type.equals("jump") ? 400 : 0;
            UI.GameState nextA = state.applyMove(a);
            UI.GameState nextB = state.applyMove(b);
            return Integer.compare(evaluate(nextB) + jumpB + capB, evaluate(nextA) + jumpA + capA);
        });
        return result;
    }

    /**
     * Enhanced evaluation: mobility, vulnerability, advancement, piece count.
     * Time Complexity: O(n^2) for fixed board.
     */
    private static int evaluate(UI.GameState state) {
        int p1Score = 0, p2Score = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.EMPTY) continue;

                int centerBonus = (r >= 2 && r <= 5 && c >= 2 && c <= 5) ? 15 : 0;
                int p1Back = (piece == UI.P1 && r == 7) ? 25 : 0;
                int p2Back = (piece == UI.P2 && r == 0) ? 25 : 0;
                int p2Advance = (piece == UI.P2) ? (7 - r) * 10 : 0;
                int p1Advance = (piece == UI.P1) ? r * 10 : 0;
                int vuln = vulnerabilityPenalty(state, r, c, piece);

                if (piece == UI.P1) p1Score += MAN_VAL + centerBonus + p1Back + p1Advance - vuln;
                else if (piece == UI.P1_KING) p1Score += KING_VAL + centerBonus - vuln;
                else if (piece == UI.P2) p2Score += MAN_VAL + centerBonus + p2Back + p2Advance - vuln;
                else if (piece == UI.P2_KING) p2Score += KING_VAL + centerBonus - vuln;
            }
        }
        int mobility = (state.getLegalMovesForPlayer(UI.P2).size() - state.getLegalMovesForPlayer(UI.P1).size()) * 5;
        return p2Score - p1Score + mobility;
    }

    private static int vulnerabilityPenalty(UI.GameState state, int r, int c, int piece) {
        boolean isP2 = piece == UI.P2 || piece == UI.P2_KING;
        if (!isP2) return 0;
        int penalty = 0;
        int[][] dirs = {{-1,-1},{-1,1},{1,-1},{1,1}};
        for (int[] d : dirs) {
            int jumpR = r + 2*d[0], jumpC = c + 2*d[1];
            int overR = r + d[0], overC = c + d[1];
            if (state.inBounds(jumpR, jumpC) && state.inBounds(overR, overC)) {
                int over = state.board[overR][overC];
                int land = state.board[jumpR][jumpC];
                if (state.isP1(over) && land == UI.EMPTY) penalty += 30;
            }
        }
        return penalty;
    }
}
