import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dynamic Programming algorithm for Checkers AI - Hard Mode.
 * Uses memoization (transposition table) to cache evaluated positions.
 * Avoids recomputing the same board states - optimal strategy optimization.
 *
 * Implements: Memoization/tabulation, optimal decision evaluation, strategy optimization.
 * Hard difficulty: makes optimal decisions, difficult for user to beat.
 */
public class DynamicProgramming {

    private static final int MAN_VAL = 100;
    private static final int KING_VAL = 300;
    private static final int SEARCH_DEPTH = 6;

    private static Map<String, TTEntry> transpositionTable = new HashMap<>();

    private static class TTEntry {
        int depth;
        int score;
        UI.Move move;

        TTEntry(int depth, int score, UI.Move move) {
            this.depth = depth;
            this.score = score;
            this.move = move;
        }
    }

    /**
     * Returns the best move using DP-enhanced minimax with alpha-beta pruning.
     * Memoizes board positions to avoid redundant computation.
     *
     * Time Complexity: O(b^d) worst case; reduced by memoization and alpha-beta pruning.
     *
     * @param state current game state
     * @return best move, or null if no moves
     */
    public static UI.Move getBestMove(UI.GameState state) {
        transpositionTable.clear();
        List<UI.Move> moves = state.getLegalMovesForPlayer(UI.P2);
        if (moves.isEmpty()) return null;
        if (moves.size() == 1) return moves.get(0);

        UI.Move bestMove = moves.get(0);
        int bestScore = Integer.MIN_VALUE;

        List<UI.Move> ordered = orderMoves(moves, state);

        for (UI.Move move : ordered) {
            UI.GameState nextState = state.applyMove(move);
            int score = minimaxDP(nextState, SEARCH_DEPTH - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    /**
     * Minimax with alpha-beta pruning and transposition table (DP memoization).
     * Caches (state, depth) -> (score, move) to avoid recomputation.
     *
     * Time Complexity: O(b^d) worst case, but with memoization many states
     * are computed once. Effective complexity reduced by cache hits.
     */
    private static int minimaxDP(UI.GameState state, int depth, int alpha, int beta, boolean isMaximizing) {
        List<UI.Move> moves = state.getLegalMovesForPlayer(state.turn);

        if (depth == 0 || moves.isEmpty()) {
            return evaluate(state);
        }

        String hash = hashState(state);
        if (transpositionTable.containsKey(hash)) {
            TTEntry entry = transpositionTable.get(hash);
            if (entry.depth >= depth) {
                return entry.score;
            }
        }

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            UI.Move bestMove = moves.get(0);
            List<UI.Move> ordered = orderMoves(moves, state);

            for (UI.Move move : ordered) {
                UI.GameState next = state.applyMove(move);
                int eval = minimaxDP(next, depth - 1, alpha, beta, false);
                if (eval > maxEval) {
                    maxEval = eval;
                    bestMove = move;
                }
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }
            transpositionTable.put(hash, new TTEntry(depth, maxEval, bestMove));
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            UI.Move bestMove = moves.get(0);
            List<UI.Move> ordered = orderMoves(moves, state);

            for (UI.Move move : ordered) {
                UI.GameState next = state.applyMove(move);
                int eval = minimaxDP(next, depth - 1, alpha, beta, true);
                if (eval < minEval) {
                    minEval = eval;
                    bestMove = move;
                }
                beta = Math.min(beta, eval);
                if (beta <= alpha) break;
            }
            transpositionTable.put(hash, new TTEntry(depth, minEval, bestMove));
            return minEval;
        }
    }

    /**
     * Move ordering: evaluate moves heuristically to improve alpha-beta pruning.
     * Better moves first = more cutoffs = faster search.
     * Time Complexity: O(m) where m = number of moves, each eval is O(1).
     */
    private static List<UI.Move> orderMoves(List<UI.Move> moves, UI.GameState state) {
        List<UI.Move> result = new ArrayList<>(moves);
        result.sort((a, b) -> {
            UI.GameState nextA = state.applyMove(a);
            UI.GameState nextB = state.applyMove(b);
            int scoreA = evaluate(nextA) + (a.type.equals("jump") ? 500 : 0);
            int scoreB = evaluate(nextB) + (b.type.equals("jump") ? 500 : 0);
            return Integer.compare(scoreB, scoreA);
        });
        return result;
    }

    /**
     * Hash board state for memoization key.
     * Time Complexity: O(n^2) for board iteration.
     */
    private static String hashState(UI.GameState state) {
        StringBuilder sb = new StringBuilder();
        sb.append(state.turn);
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                sb.append(state.board[r][c]);
            }
        }
        return sb.toString();
    }

    /**
     * Static evaluation with full heuristics.
     * Time Complexity: O(n^2) for fixed board.
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
                int p2Advance = (piece == UI.P2) ? (7 - r) * 8 : 0;
                int p1Advance = (piece == UI.P1) ? r * 8 : 0;

                if (piece == UI.P1) p1Score += MAN_VAL + centerBonus + p1Back + p1Advance;
                else if (piece == UI.P1_KING) p1Score += KING_VAL + centerBonus;
                else if (piece == UI.P2) p2Score += MAN_VAL + centerBonus + p2Back + p2Advance;
                else if (piece == UI.P2_KING) p2Score += KING_VAL + centerBonus;
            }
        }
        return p2Score - p1Score;
    }
}
