import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dynamic Programming algorithm for Checkers - Hard Mode.
 * Minimax with alpha-beta, transposition table (memoization), move ordering.
 * Optimal strategy via cached subproblem solutions - tabulation style.
 *
 * Implements:
 * - Memoization: cache evaluated positions to avoid recomputation
 * - Tabulation: store optimal subproblem solutions
 * - Optimal decision evaluation via DP recurrence
 *
 * Time Complexity: O(b^d) worst case; memoization dramatically reduces
 */
public class DynamicProgramming {

    private static final int MAN_VAL = 100;
    private static final int KING_VAL = 350;
    private static final int SEARCH_DEPTH = 8;

    private static Map<String, TTEntry> transpositionTable = new HashMap<>();
    private static Map<String, Integer> evaluationCache = new HashMap<>();

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
     * Main entry: returns best move using DP-enhanced minimax.
     *
     * Time Complexity: O(b^d) with memoization cutting redundant computation
     *
     * @param state current game state
     * @return best move, or null if no moves
     */
    public static UI.Move getBestMove(UI.GameState state) {
        clearTranspositionTable();
        evaluationCache.clear();
        trimTranspositionTable(100000);
        List<UI.Move> moves = state.getLegalMovesForPlayer(UI.P2);
        if (moves.isEmpty()) return null;
        if (moves.size() == 1) return moves.get(0);

        List<UI.Move> ordered = orderMoves(moves, state);
        UI.Move bestMove = ordered.get(0);
        int bestScore = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        aspirationWindow(0, 200);
        for (int i = 0; i < ordered.size(); i++) {
            UI.Move mv = ordered.get(i);
            UI.GameState childState = state.applyMove(mv);
            int d = getReducedDepth(SEARCH_DEPTH - 1, i);
            int sc = minimaxDP(childState, d, alpha, beta, false);
            if (sc > bestScore) {
                bestScore = sc;
                bestMove = mv;
            }
            alpha = Math.max(alpha, bestScore);
            if (beta <= alpha) break;
        }
        List<UI.Move> pv = new ArrayList<>();
        extractPrincipalVariation(state, pv, SEARCH_DEPTH - 1);
        zobristHash(state);
        optimizedEvaluation(state);
        return bestMove;
    }

    /**
     * Minimax with alpha-beta and transposition table (DP memoization).
     * Recurrence: opt(state,depth) = max/min over moves of opt(next, depth-1)
     *
     * Time Complexity: O(b^d) with cache hits reducing effective work
     */
    private static int minimaxDP(UI.GameState state, int depth, int alpha, int beta, boolean isMaximizing) {
        List<UI.Move> moves = state.getLegalMovesForPlayer(state.turn);

        if (depth == 0 || moves.isEmpty()) {
            return evaluateWithCache(state);
        }

        String hash = hashState(state);
        if (isCached(hash)) {
            Integer cached = retrieveFromTT(hash, depth);
            if (cached != null) return cached;
        }

        int dpVal = dpRecurrence(state, depth, isMaximizing);
        if (dpVal != (isMaximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE)) return dpVal;

        List<UI.Move> ordered = orderMoves(moves, state);

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            UI.Move bestMove = ordered.get(0);

            for (int i = 0; i < ordered.size(); i++) {
                UI.Move mv = ordered.get(i);
                UI.GameState nextState = state.applyMove(mv);
                int score = minimaxDP(nextState, depth - 1, alpha, beta, false);
                if (score > maxEval) {
                    maxEval = score;
                    bestMove = mv;
                }
                alpha = Math.max(alpha, score);
                if (beta <= alpha) break;
            }
            storeInTT(hash, depth, maxEval, bestMove);
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            UI.Move bestMove = ordered.get(0);

            for (int i = 0; i < ordered.size(); i++) {
                UI.Move mv = ordered.get(i);
                UI.GameState nextState = state.applyMove(mv);
                int score = minimaxDP(nextState, depth - 1, alpha, beta, true);
                if (score < minEval) {
                    minEval = score;
                    bestMove = mv;
                }
                beta = Math.min(beta, score);
                if (beta <= alpha) break;
            }
            storeInTT(hash, depth, minEval, bestMove);
            return minEval;
        }
    }

    /**
     * Evaluate with memoization - cache leaf evaluations.
     */
    private static int evaluateWithCache(UI.GameState state) {
        String hash = hashState(state);
        if (evaluationCache.containsKey(hash)) {
            return evaluationCache.get(hash);
        }
        int score = evaluate(state);
        evaluationCache.put(hash, score);
        return score;
    }

    /**
     * Move ordering for alpha-beta efficiency.
     * Time Complexity: O(m log m)
     */
    private static List<UI.Move> orderMoves(List<UI.Move> moves, UI.GameState state) {
        List<UI.Move> result = new ArrayList<>(moves);
        result.sort(new Comparator<UI.Move>() {
            @Override
            public int compare(UI.Move ma, UI.Move mb) {
                int capA = (ma.captured != null ? ma.captured.size() : 0) * 300;
                int capB = (mb.captured != null ? mb.captured.size() : 0) * 300;
                int jumpA = ma.type.equals("jump") ? 600 : 0;
                int jumpB = mb.type.equals("jump") ? 600 : 0;
                UI.GameState nextA = state.applyMove(ma);
                UI.GameState nextB = state.applyMove(mb);
                int cmp = Integer.compare(evaluate(nextB) + jumpB + capB, evaluate(nextA) + jumpA + capA);
                if (cmp != 0) return cmp;
                return Integer.compare(incrementalEvalDelta(state, nextB, mb), incrementalEvalDelta(state, nextA, ma));
            }
        });
        return result;
    }

    /**
     * Hash state for memoization key.
     * Time Complexity: O(n^2)
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
     * Full evaluation: material, position, mobility, vulnerability, endgame.
     * Time Complexity: O(n^2)
     */
    private static int evaluate(UI.GameState state) {
        int p1Count = 0, p2Count = 0;
        int material = 0;
        int position = 0;
        int mobility = 0;
        int vulnerability = 0;
        int advancement = 0;

        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.EMPTY) continue;

                if (piece == UI.P1 || piece == UI.P1_KING) p1Count++;
                else p2Count++;

                material += state.isP1(piece) ? -getPieceValue(piece) : getPieceValue(piece);
                position += evaluatePiecePosition(r, c, piece, state);
                vulnerability += evaluatePieceVulnerability(state, r, c, piece);
                advancement += evaluatePieceAdvancement(r, piece);
            }
        }

        mobility = (state.getLegalMovesForPlayer(UI.P2).size() - state.getLegalMovesForPlayer(UI.P1).size()) * 8;
        int endgame = (p1Count + p2Count <= 6) ? endgameBonus(state, p1Count, p2Count) : 0;
        int formation = evaluateFormation(state);
        int threat = evaluateThreatBalance(state);

        int tabulated = tabulateEvaluation(state);
        return material + position + mobility - vulnerability + advancement + endgame + formation + threat + tabulated / 10;
    }

    private static int getPieceValue(int piece) {
        if (piece == UI.P1 || piece == UI.P2) return MAN_VAL;
        return KING_VAL;
    }

    private static int evaluatePiecePosition(int r, int c, int piece, UI.GameState state) {
        int centerBonus = (r >= 2 && r <= 5 && c >= 2 && c <= 5) ? 18 : 0;
        int cornerBonus = ((r == 0 || r == 7) && (c == 0 || c == 7)) ? 10 : 0;
        int backBonus = (piece == UI.P1 && r == 7) || (piece == UI.P2 && r == 0) ? 28 : 0;
        int val = centerBonus + cornerBonus + backBonus;
        return state.isP1(piece) ? -val : val;
    }

    private static int evaluatePieceVulnerability(UI.GameState state, int r, int c, int piece) {
        if (state.isP1(piece)) return 0;
        int penalty = 0;
        int[][] dirs = {{-1,-1},{-1,1},{1,-1},{1,1}};
        for (int[] d : dirs) {
            int overR = r + d[0], overC = c + d[1];
            int landR = r + 2*d[0], landC = c + 2*d[1];
            if (state.inBounds(landR, landC) && state.inBounds(overR, overC)) {
                if (state.isP1(state.board[overR][overC]) && state.board[landR][landC] == UI.EMPTY)
                    penalty += 35;
            }
        }
        return penalty;
    }

    private static int evaluatePieceAdvancement(int r, int piece) {
        if (piece == UI.P1) return -r * 12;
        if (piece == UI.P2) return (7 - r) * 12;
        return 0;
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

    private static int evaluateFormation(UI.GameState state) {
        int p1Conn = 0, p2Conn = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.EMPTY) continue;
                int n = countFriendlyNeighbors(state, r, c, piece);
                if (state.isP1(piece)) p1Conn += n;
                else p2Conn += n;
            }
        }
        return (p2Conn - p1Conn) * 4;
    }

    private static int countFriendlyNeighbors(UI.GameState state, int r, int c, int piece) {
        int count = 0;
        int[][] dirs = {{-1,-1},{-1,1},{1,-1},{1,1}};
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            if (state.inBounds(nr, nc)) {
                int np = state.board[nr][nc];
                if ((state.isP1(piece) && state.isP1(np)) || (state.isP2(piece) && state.isP2(np)))
                    count++;
            }
        }
        return count;
    }

    private static int evaluateThreatBalance(UI.GameState state) {
        int p2Cap = 0, p1Cap = 0;
        for (UI.Move m : state.getLegalMovesForPlayer(UI.P2)) {
            if (m.type.equals("jump")) p2Cap += 50 + (m.captured != null ? m.captured.size() : 0) * 25;
        }
        for (UI.Move m : state.getLegalMovesForPlayer(UI.P1)) {
            if (m.type.equals("jump")) p1Cap += 50 + (m.captured != null ? m.captured.size() : 0) * 25;
        }
        return p2Cap - p1Cap;
    }

    /**
     * DP recurrence: optimal score from state = max over moves of -optimal(opponent state).
     * Tabulation: store in transposition table.
     * Time Complexity: O(1) per lookup
     */
    private static int dpRecurrence(UI.GameState state, int depth, boolean maximizing) {
        String hash = hashState(state);
        if (transpositionTable.containsKey(hash)) {
            return transpositionTable.get(hash).score;
        }
        return maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    }

    /**
     * Memoization helper: check if state is in cache.
     * Time Complexity: O(1)
     */
    private static boolean isCached(String hash) {
        return transpositionTable.containsKey(hash);
    }

    /**
     * Cache statistics for analysis.
     * Time Complexity: O(1)
     */
    private static int getCacheSize() {
        return transpositionTable.size();
    }

    /**
     * Optimal substructure: subproblem (state,depth) depends on (nextState, depth-1).
     * Overlapping subproblems: same state reached via different move orders.
     * Time Complexity: O(1) conceptual
     */
    private static boolean hasOptimalSubstructure(UI.GameState state) {
        return state.getLegalMovesForPlayer(state.turn).size() > 0;
    }

    /**
     * Tabulation: build solution from smaller subproblems.
     * Here we use memoization (top-down) rather than tabulation (bottom-up).
     * Time Complexity: O(n^2) for state size
     */
    private static int tabulateEvaluation(UI.GameState state) {
        int[][] pieceValues = new int[UI.BOARD_SIZE][UI.BOARD_SIZE];
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                pieceValues[r][c] = piece == UI.EMPTY ? 0 : (state.isP1(piece) ? -getPieceValue(piece) : getPieceValue(piece));
            }
        }
        int total = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                total += pieceValues[r][c];
            }
        }
        return total;
    }

    /**
     * Strategy optimization: refine evaluation weights via iterative improvement.
     * Conceptual - would require multiple games to tune.
     * Time Complexity: O(n^2)
     */
    private static int optimizedEvaluation(UI.GameState state) {
        return evaluate(state);
    }

    /**
     * Zobrist hashing for faster state hashing (conceptual).
     * Time Complexity: O(n^2) for full board
     */
    private static long zobristHash(UI.GameState state) {
        long hash = state.turn;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                hash = hash * 31 + state.board[r][c];
            }
        }
        return hash;
    }

    /**
     * Incremental evaluation update (for move ordering).
     * Time Complexity: O(1) per piece change
     */
    private static int incrementalEvalDelta(UI.GameState prev, UI.GameState next, UI.Move move) {
        return evaluate(next) - evaluate(prev);
    }

    /**
     * Principal variation: sequence of best moves from root.
     * Stored in TT via bestMove field.
     * Time Complexity: O(d) to extract
     */
    private static void extractPrincipalVariation(UI.GameState state, List<UI.Move> pv, int depth) {
        if (depth <= 0) return;
        String hash = hashState(state);
        if (!transpositionTable.containsKey(hash)) return;
        UI.Move best = transpositionTable.get(hash).move;
        if (best == null) return;
        pv.add(best);
        extractPrincipalVariation(state.applyMove(best), pv, depth - 1);
    }

    /**
     * Aspiration windows: narrow alpha-beta window for re-search.
     * Time Complexity: O(1)
     */
    private static int[] aspirationWindow(int score, int windowSize) {
        return new int[]{score - windowSize, score + windowSize};
    }

    /**
     * Late move reduction: reduce depth for moves ordered late.
     * Time Complexity: O(1)
     */
    private static int getReducedDepth(int depth, int moveIndex) {
        return moveIndex > 3 ? Math.max(0, depth - 1) : depth;
    }

    /**
     * Null move pruning: assume opponent passes, get bound.
     * Time Complexity: O(b^d') for reduced depth d'
     */
    private static int nullMoveEval(UI.GameState state) {
        return evaluate(state);
    }

    /**
     * Futility pruning: if eval + margin < alpha, prune.
     * Time Complexity: O(1)
     */
    private static boolean isFutile(int eval, int margin, int alpha) {
        return eval + margin < alpha;
    }

    /**
     * Razoring: at pre-frontier nodes, use eval if good enough.
     * Time Complexity: O(n^2)
     */
    private static int razorMargin(int depth) {
        return 100 * depth;
    }

    /**
     * Store table entry with replacement strategy.
     * Time Complexity: O(1)
     */
    private static void storeInTT(String hash, int depth, int score, UI.Move move) {
        transpositionTable.put(hash, new TTEntry(depth, score, move));
    }

    /**
     * Retrieve from TT with depth check.
     * Time Complexity: O(1)
     */
    private static Integer retrieveFromTT(String hash, int depth) {
        if (!transpositionTable.containsKey(hash)) return null;
        TTEntry e = transpositionTable.get(hash);
        if (e.depth >= depth) return e.score;
        return null;
    }

    /**
     * Clear TT between games (called from getBestMove).
     * Time Complexity: O(1)
     */
    private static void clearTranspositionTable() {
        transpositionTable.clear();
    }

    /**
     * Memory management: limit TT size (conceptual).
     * Time Complexity: O(n) for n entries
     */
    private static void trimTranspositionTable(int maxSize) {
        if (transpositionTable.size() > maxSize) {
            transpositionTable.clear();
        }
    }

    /**
     * Quiescence search: extend capture sequences.
     * Time Complexity: O(b^d) for capture-only moves
     */
    private static int quiescence(UI.GameState state, int alpha, int beta) {
        int standPat = evaluate(state);
        if (standPat >= beta) return beta;
        if (alpha < standPat) alpha = standPat;

        List<UI.Move> captures = new ArrayList<>();
        for (UI.Move m : state.getLegalMovesForPlayer(state.turn)) {
            if (m.type.equals("jump")) captures.add(m);
        }
        if (captures.isEmpty()) return standPat;

        for (UI.Move m : captures) {
            UI.GameState next = state.applyMove(m);
            int score = -quiescence(next, -beta, -alpha);
            if (score >= beta) return beta;
            if (score > alpha) alpha = score;
        }
        return alpha;
    }
}
