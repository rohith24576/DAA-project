import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dynamic Programming algorithm for Checkers - Hard Mode.
 * Full minimax with alpha-beta, transposition table (memoization), move ordering.
 * Optimal strategy optimization via cached subproblem solutions.
 *
 * Implements: Memoization, tabulation-style caching, optimal decision evaluation.
 */
public class DynamicProgramming {

    private static final int MAN_VAL = 100;
    private static final int KING_VAL = 350;
    private static final int SEARCH_DEPTH = 8;

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
     * Returns the best move using DP-enhanced minimax with alpha-beta and memoization.
     *
     * Time Complexity: O(b^d) worst case; memoization and alpha-beta reduce it significantly.
     *
     * @param state current game state
     * @return best move, or null if no moves
     */
    public static UI.Move getBestMove(UI.GameState state) {
        transpositionTable.clear();
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
            int score = minimaxDP(nextState, SEARCH_DEPTH - 1, alpha, beta, false);
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
     * Minimax with alpha-beta pruning and transposition table.
     * Caches (state hash, depth) -> score to avoid recomputation.
     *
     * Time Complexity: O(b^d) with memoization cutting redundant branches.
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

        List<UI.Move> ordered = orderMoves(moves, state);

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            UI.Move bestMove = ordered.get(0);

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
            UI.Move bestMove = ordered.get(0);

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
     * Move ordering: captures first, then by evaluation. Critical for alpha-beta efficiency.
     * Time Complexity: O(m) for m moves.
     */
    private static List<UI.Move> orderMoves(List<UI.Move> moves, UI.GameState state) {
        List<UI.Move> result = new ArrayList<>(moves);
        result.sort((a, b) -> {
            int capA = (a.captured != null ? a.captured.size() : 0) * 300;
            int capB = (b.captured != null ? b.captured.size() : 0) * 300;
            int jumpA = a.type.equals("jump") ? 600 : 0;
            int jumpB = b.type.equals("jump") ? 600 : 0;
            UI.GameState nextA = state.applyMove(a);
            UI.GameState nextB = state.applyMove(b);
            return Integer.compare(evaluate(nextB) + jumpB + capB, evaluate(nextA) + jumpA + capA);
        });
        return result;
    }

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
     * Full evaluation: piece count, position, mobility, vulnerability, endgame.
     * Time Complexity: O(n^2) for fixed board.
     */
    private static int evaluate(UI.GameState state) {
        int p1Count = 0, p2Count = 0;
        int p1Score = 0, p2Score = 0;

        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.EMPTY) continue;

                if (piece == UI.P1 || piece == UI.P1_KING) p1Count++;
                else p2Count++;

                int centerBonus = (r >= 2 && r <= 5 && c >= 2 && c <= 5) ? 18 : 0;
                int p1Back = (piece == UI.P1 && r == 7) ? 28 : 0;
                int p2Back = (piece == UI.P2 && r == 0) ? 28 : 0;
                int p2Advance = (piece == UI.P2) ? (7 - r) * 12 : 0;
                int p1Advance = (piece == UI.P1) ? r * 12 : 0;
                int vuln = vulnerabilityPenalty(state, r, c, piece);
                int doubleCorner = ((r == 0 || r == 7) && (c == 0 || c == 7)) ? 10 : 0;

                if (piece == UI.P1) p1Score += MAN_VAL + centerBonus + p1Back + p1Advance - vuln + doubleCorner;
                else if (piece == UI.P1_KING) p1Score += KING_VAL + centerBonus - vuln + doubleCorner;
                else if (piece == UI.P2) p2Score += MAN_VAL + centerBonus + p2Back + p2Advance - vuln + doubleCorner;
                else if (piece == UI.P2_KING) p2Score += KING_VAL + centerBonus - vuln + doubleCorner;
            }
        }

        int mobility = (state.getLegalMovesForPlayer(UI.P2).size() - state.getLegalMovesForPlayer(UI.P1).size()) * 8;
        int endgame = (p1Count + p2Count <= 6) ? endgameBonus(state, p1Count, p2Count) : 0;
        return p2Score - p1Score + mobility + endgame;
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
                if (state.isP1(over) && land == UI.EMPTY) penalty += 35;
            }
        }
        return penalty;
    }

    private static int endgameBonus(UI.GameState state, int p1Count, int p2Count) {
        int bonus = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.P2_KING) bonus += (7 - Math.min(r, 7 - r)) * 5;
                if (piece == UI.P1_KING) bonus -= (7 - Math.min(r, 7 - r)) * 5;
            }
        }
        return bonus;
    }
}
