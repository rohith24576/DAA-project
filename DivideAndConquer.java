import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Divide and Conquer algorithm for Checkers - Easy Mode.
 * Minimax with alpha-beta pruning. Divides game tree into subproblems,
 * conquers by combining min/max results. Binary-search style divide strategy.
 *
 * Implements:
 * - Divide: split by possible moves (subproblems)
 * - Conquer: combine via minimax (min/max over subproblem results)
 * - Recursive board evaluation with multiple phases
 *
 * Time Complexity: O(b^d) where b = branching factor, d = depth
 */
public class DivideAndConquer {

    private static final int MAN_VAL = 100;
    private static final int KING_VAL = 320;
    private static final int SEARCH_DEPTH = 3;

    // Positional weight matrix for divide-phase evaluation
    private static final int[][] POSITION_WEIGHTS = {
        {0, 0, 0, 3, 3, 0, 0, 0},
        {0, 0, 4, 5, 5, 4, 0, 0},
        {0, 4, 6, 7, 7, 6, 4, 0},
        {3, 5, 7, 10, 10, 7, 5, 3},
        {3, 5, 7, 10, 10, 7, 5, 3},
        {0, 4, 6, 7, 7, 6, 4, 0},
        {0, 0, 4, 5, 5, 4, 0, 0},
        {0, 0, 0, 3, 3, 0, 0, 0}
    };

    /**
     * Main entry: returns best move using divide-and-conquer minimax.
     *
     * Time Complexity: O(b^d) with alpha-beta reducing effective branching
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
     * Minimax with alpha-beta: divide into subproblems, conquer with min/max.
     * Uses selectBest, recursiveCombine, shouldStopDividing, estimateSubproblemSize.
     * Time Complexity: O(b^d) with pruning
     */
    private static int minimax(UI.GameState state, int depth, int alpha, int beta, boolean isMaximizing) {
        List<UI.Move> moves = state.getLegalMovesForPlayer(state.turn);
        if (shouldStopDividing(depth) || moves.isEmpty()) {
            return evaluateRecursive(state, depth);
        }

        List<UI.Move> ordered = orderMoves(moves, state);
        int[] childEvals = new int[Math.min(ordered.size(), 20)];
        int idx = 0;

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (UI.Move move : ordered) {
                UI.GameState next = state.applyMove(move);
                int eval = minimax(next, depth - 1, alpha, beta, false);
                if (idx < childEvals.length) childEvals[idx++] = eval;
                maxEval = selectBest(maxEval, eval, true);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }
            return idx > 0 ? recursiveCombine(java.util.Arrays.copyOf(childEvals, idx), true) : maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (UI.Move move : ordered) {
                UI.GameState next = state.applyMove(move);
                int eval = minimax(next, depth - 1, alpha, beta, true);
                if (idx < childEvals.length) childEvals[idx++] = eval;
                minEval = selectBest(minEval, eval, false);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break;
            }
            return idx > 0 ? recursiveCombine(java.util.Arrays.copyOf(childEvals, idx), false) : minEval;
        }
    }

    /**
     * Order moves for better alpha-beta pruning (divide phase optimization).
     * Uses partitionMoves, isMovePromising, mergeMovesByScore.
     * Time Complexity: O(m log m)
     */
    private static List<UI.Move> orderMoves(List<UI.Move> moves, UI.GameState state) {
        List<UI.Move> captures = new ArrayList<>();
        List<UI.Move> nonCaptures = new ArrayList<>();
        partitionMoves(moves, captures, nonCaptures);
        List<UI.Move> orderedCaptures = mergeMovesByScore(captures, state);
        List<UI.Move> orderedNonCaptures = mergeMovesByScore(nonCaptures, state);
        List<UI.Move> result = mergeMoveLists(orderedCaptures, orderedNonCaptures, state);
        result.sort((a, b) -> {
            boolean promA = isMovePromising(a, state);
            boolean promB = isMovePromising(b, state);
            if (promA != promB) return promB ? 1 : -1;
            int jumpA = a.type.equals("jump") ? 500 : 0;
            int jumpB = b.type.equals("jump") ? 500 : 0;
            int capA = (a.captured != null ? a.captured.size() : 0) * 100;
            int capB = (b.captured != null ? b.captured.size() : 0) * 100;
            UI.GameState nextA = state.applyMove(a);
            UI.GameState nextB = state.applyMove(b);
            return Integer.compare(evaluate(nextB) + jumpB + capB, evaluate(nextA) + jumpA + capA);
        });
        return result;
    }

    /**
     * Full evaluation: material, position, advancement, safety, zones, phase, quadrants.
     * Time Complexity: O(n^2)
     */
    private static int evaluate(UI.GameState state) {
        if (shouldStopDividing(0) || estimateSubproblemSize(state.getLegalMovesForPlayer(state.turn).size(), 0) == 0)
            return baseCaseEval(state);
        int material = evaluateMaterial(state);
        int position = evaluatePosition(state);
        int advancement = evaluateAdvancement(state);
        int mobility = evaluateMobility(state);
        int safety = evaluateSafety(state);
        int backRow = evaluateBackRow(state);
        int center = evaluateCenterControl(state);
        int vuln = evaluateVulnerability(state);
        int zones = evaluateByZones(state);
        int phase = evaluateByPhase(state);
        int quadrants = evaluateQuadrants(state);
        int ranks = evaluateByRanks(state);
        int files = evaluateByFiles(state);
        int sideStrength = evaluateSideStrength(state);
        int pieceType = evaluateByPieceType(state);
        int[] scores = {material, position, zones, phase, quadrants, ranks, files, sideStrength, pieceType};
        int[] weights = {2, 1, 1, 1, 1, 1, 1, 1, 1};
        return weightedConquer(scores, weights) + conquerEvaluations(material, position, mobility) / 2 + safety + backRow + center + vuln;
    }

    private static int evaluateMaterial(UI.GameState state) {
        int p1 = 0, p2 = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.P1) p1 += MAN_VAL;
                else if (piece == UI.P1_KING) p1 += KING_VAL;
                else if (piece == UI.P2) p2 += MAN_VAL;
                else if (piece == UI.P2_KING) p2 += KING_VAL;
            }
        }
        return p2 - p1;
    }

    private static int evaluatePosition(UI.GameState state) {
        int p1 = 0, p2 = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.EMPTY) continue;
                int w = POSITION_WEIGHTS[r][c];
                if (state.isP1(piece)) p1 += w;
                else p2 += w;
            }
        }
        return p2 - p1;
    }

    private static int evaluateAdvancement(UI.GameState state) {
        int p1 = 0, p2 = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.P1) p1 += r * 7;
                else if (piece == UI.P2) p2 += (7 - r) * 7;
            }
        }
        return p2 - p1;
    }

    private static int evaluateMobility(UI.GameState state) {
        int p2M = state.getLegalMovesForPlayer(UI.P2).size();
        int p1M = state.getLegalMovesForPlayer(UI.P1).size();
        return (p2M - p1M) * 6;
    }

    private static int evaluateSafety(UI.GameState state) {
        int p1V = 0, p2V = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.EMPTY) continue;
                int v = computeVulnerability(state, r, c, piece);
                if (state.isP1(piece)) p1V += v;
                else p2V += v;
            }
        }
        return p1V - p2V;
    }

    private static int computeVulnerability(UI.GameState state, int r, int c, int piece) {
        if (state.isP1(piece)) return 0;
        int penalty = 0;
        int[][] dirs = {{-1,-1},{-1,1},{1,-1},{1,1}};
        for (int[] d : dirs) {
            int overR = r + d[0], overC = c + d[1];
            int landR = r + 2*d[0], landC = c + 2*d[1];
            if (state.inBounds(landR, landC) && state.inBounds(overR, overC)) {
                if (state.isP1(state.board[overR][overC]) && state.board[landR][landC] == UI.EMPTY)
                    penalty += 20;
            }
        }
        return penalty;
    }

    private static int evaluateBackRow(UI.GameState state) {
        int p1 = 0, p2 = 0;
        for (int c = 0; c < UI.BOARD_SIZE; c++) {
            if (state.board[7][c] == UI.P1) p1 += 22;
            if (state.board[0][c] == UI.P2) p2 += 22;
        }
        return p2 - p1;
    }

    private static int evaluateCenterControl(UI.GameState state) {
        int p1 = 0, p2 = 0;
        for (int r = 3; r <= 4; r++) {
            for (int c = 3; c <= 4; c++) {
                int piece = state.board[r][c];
                if (state.isP1(piece)) p1 += 18;
                else if (state.isP2(piece)) p2 += 18;
            }
        }
        return p2 - p1;
    }

    private static int evaluateVulnerability(UI.GameState state) {
        return -evaluateSafety(state) / 2;
    }

    /**
     * Binary-search style divide: partition moves into "promising" vs "less promising".
     * Used for move ordering - explore promising branch first.
     * Time Complexity: O(m)
     */
    private static boolean isMovePromising(UI.Move m, UI.GameState state) {
        if (m.type.equals("jump")) return true;
        int captures = m.captured != null ? m.captured.size() : 0;
        if (captures > 0) return true;
        UI.GameState next = state.applyMove(m);
        return evaluate(next) > 0;
    }

    /**
     * Recursive board evaluation - combine sub-scores from different zones.
     * Divide: evaluate by zone (center, wings, back). Conquer: sum.
     * Time Complexity: O(n^2)
     */
    private static int evaluateByZones(UI.GameState state) {
        int center = evaluateZone(state, 2, 5, 2, 5);
        int leftWing = evaluateZone(state, 0, 7, 0, 3);
        int rightWing = evaluateZone(state, 0, 7, 4, 7);
        return center * 2 + leftWing + rightWing;
    }

    private static int evaluateZone(UI.GameState state, int r1, int r2, int c1, int c2) {
        int p1 = 0, p2 = 0;
        for (int r = r1; r <= r2; r++) {
            for (int c = c1; c <= c2; c++) {
                int piece = state.board[r][c];
                if (state.isP1(piece)) p1++;
                else if (state.isP2(piece)) p2++;
            }
        }
        return (p2 - p1) * 10;
    }

    /**
     * Subproblem decomposition: evaluate piece types separately then combine.
     * Time Complexity: O(n^2)
     */
    private static int evaluateByPieceType(UI.GameState state) {
        int menScore = evaluateMen(state);
        int kingsScore = evaluateKings(state);
        return menScore + kingsScore;
    }

    private static int evaluateMen(UI.GameState state) {
        int p1 = 0, p2 = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.P1) p1 += MAN_VAL + (7 - r) * 2;
                else if (piece == UI.P2) p2 += MAN_VAL + r * 2;
            }
        }
        return p2 - p1;
    }

    private static int evaluateKings(UI.GameState state) {
        int p1 = 0, p2 = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.P1_KING) p1 += KING_VAL;
                else if (piece == UI.P2_KING) p2 += KING_VAL;
            }
        }
        return p2 - p1;
    }

    /**
     * Conquer phase: combine multiple evaluation dimensions.
     * Time Complexity: O(n^2)
     */
    private static int conquerEvaluations(int material, int position, int mobility) {
        return material * 2 + position + mobility / 2;
    }

    /**
     * Divide phase: split board into quadrants for parallel-style evaluation.
     * Time Complexity: O(n^2)
     */
    private static int evaluateQuadrants(UI.GameState state) {
        int q1 = evaluateZone(state, 0, 3, 0, 3);
        int q2 = evaluateZone(state, 0, 3, 4, 7);
        int q3 = evaluateZone(state, 4, 7, 0, 3);
        int q4 = evaluateZone(state, 4, 7, 4, 7);
        return q1 + q2 + q3 + q4;
    }

    /**
     * Recursive structure evaluation: base case + recursive case.
     * Base: terminal/leaf. Recursive: combine children.
     * Time Complexity: O(n^2)
     */
    private static int evaluateRecursive(UI.GameState state, int depth) {
        if (depth <= 0) return evaluateMaterial(state) + evaluatePosition(state);
        return evaluateRecursive(state, depth - 1) + evaluateMobility(state) / (depth + 1);
    }

    /**
     * Merge-style conquer: merge two sorted move lists by score.
     * Time Complexity: O(m log m)
     */
    private static List<UI.Move> mergeMovesByScore(List<UI.Move> moves, UI.GameState state) {
        List<UI.Move> result = new ArrayList<>(moves);
        result.sort(Comparator.comparingInt(m -> -quickEval(state, m)));
        return result;
    }

    private static int quickEval(UI.GameState state, UI.Move m) {
        UI.GameState next = state.applyMove(m);
        return evaluateMaterial(next) + (m.type.equals("jump") ? 300 : 0);
    }

    /**
     * Divide by game phase: opening, middlegame, endgame.
     * Time Complexity: O(n^2)
     */
    private static int evaluateByPhase(UI.GameState state) {
        int totalPieces = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                if (state.board[r][c] != UI.EMPTY) totalPieces++;
            }
        }
        if (totalPieces > 20) return evaluateMaterial(state) + evaluatePosition(state);
        if (totalPieces > 10) return evaluate(state);
        return evaluateMaterial(state) + evaluateMobility(state) * 2;
    }

    /**
     * Conquer with weighted combination of sub-evaluations.
     * Time Complexity: O(1)
     */
    private static int weightedConquer(int[] scores, int[] weights) {
        int total = 0;
        for (int i = 0; i < Math.min(scores.length, weights.length); i++) {
            total += scores[i] * weights[i];
        }
        return total;
    }

    /**
     * Divide board into ranks (rows) and evaluate each.
     * Time Complexity: O(n^2)
     */
    private static int evaluateByRanks(UI.GameState state) {
        int total = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            int rowScore = 0;
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (state.isP1(piece)) rowScore -= (r + 1);
                else if (state.isP2(piece)) rowScore += (8 - r);
            }
            total += rowScore * (4 - (int)Math.abs(r - 3.5));
        }
        return total;
    }

    /**
     * Divide by piece color/side - evaluate each side's strength.
     * Time Complexity: O(n^2)
     */
    private static int evaluateSideStrength(UI.GameState state) {
        int p1Strength = 0, p2Strength = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.EMPTY) continue;
                int strength = POSITION_WEIGHTS[r][c] + (state.isKing(piece) ? 50 : 0);
                if (state.isP1(piece)) p1Strength += strength;
                else p2Strength += strength;
            }
        }
        return p2Strength - p1Strength;
    }

    /**
     * Divide by file (column) - evaluate each file's control.
     * Time Complexity: O(n^2)
     */
    private static int evaluateByFiles(UI.GameState state) {
        int total = 0;
        for (int c = 0; c < UI.BOARD_SIZE; c++) {
            int fileScore = 0;
            for (int r = 0; r < UI.BOARD_SIZE; r++) {
                int piece = state.board[r][c];
                if (state.isP1(piece)) fileScore -= (7 - r);
                else if (state.isP2(piece)) fileScore += r;
            }
            total += fileScore;
        }
        return total;
    }

    /**
     * Merge sort style conquer: combine two sorted move lists.
     * Time Complexity: O(m)
     */
    private static List<UI.Move> mergeMoveLists(List<UI.Move> a, List<UI.Move> b, UI.GameState state) {
        List<UI.Move> result = new ArrayList<>();
        int i = 0, j = 0;
        while (i < a.size() && j < b.size()) {
            if (evaluate(state.applyMove(a.get(i))) >= evaluate(state.applyMove(b.get(j)))) {
                result.add(a.get(i++));
            } else {
                result.add(b.get(j++));
            }
        }
        while (i < a.size()) result.add(a.get(i++));
        while (j < b.size()) result.add(b.get(j++));
        return result;
    }

    /**
     * Divide: split moves into captures vs non-captures.
     * Time Complexity: O(m)
     */
    private static void partitionMoves(List<UI.Move> moves, List<UI.Move> captures, List<UI.Move> nonCaptures) {
        for (UI.Move m : moves) {
            if (m.type.equals("jump")) captures.add(m);
            else nonCaptures.add(m);
        }
    }

    /**
     * Conquer: select best from divided subproblem results.
     * Time Complexity: O(1)
     */
    private static int selectBest(int a, int b, boolean maximizing) {
        return maximizing ? Math.max(a, b) : Math.min(a, b);
    }

    /**
     * Base case for divide: terminal state evaluation.
     * Time Complexity: O(n^2)
     */
    private static int baseCaseEval(UI.GameState state) {
        return evaluateMaterial(state);
    }

    /**
     * Recursive case: combine child evaluations.
     * Time Complexity: O(1)
     */
    private static int recursiveCombine(int[] childEvals, boolean maximizing) {
        int result = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (int e : childEvals) {
            result = maximizing ? Math.max(result, e) : Math.min(result, e);
        }
        return result;
    }

    /**
     * Divide depth: when to stop dividing.
     * Time Complexity: O(1)
     */
    private static boolean shouldStopDividing(int depth) {
        return depth <= 0;
    }

    /**
     * Subproblem size estimation.
     * Time Complexity: O(1)
     */
    private static int estimateSubproblemSize(int moves, int depth) {
        return moves * depth;
    }
}
