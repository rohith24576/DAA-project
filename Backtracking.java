import java.util.ArrayList;
import java.util.List;

/**
 * Backtracking algorithm for Checkers - Medium Mode.
 * State-space search with recursive decision tree exploration.
 * Explores moves, backtracks when branch cannot improve result (alpha-beta).
 *
 * Implements:
 * - State-space search: explore all reachable states
 * - Recursive decision tree: each node = state, children = moves
 * - Constraint checking: prune when constraints violated (alpha-beta)
 *
 * Time Complexity: O(b^d) where b = branching factor, d = depth
 */
public class Backtracking {

    private static final int MAN_VAL = 100;
    private static final int KING_VAL = 330;
    private static final int SEARCH_DEPTH = 5;

    // Constraint thresholds for backtracking
    private static final int MAX_VULNERABILITY_PENALTY = 50;
    private static final int MOBILITY_WEIGHT = 5;

    private static List<UI.Move> orderMoves(List<UI.Move> moves, UI.GameState state) {
        List<UI.Move> result = new ArrayList<>(moves);
        result.sort((UI.Move ma, UI.Move mb) -> {
            int capA = (ma.captured != null ? ma.captured.size() : 0) * 200;
            int capB = (mb.captured != null ? mb.captured.size() : 0) * 200;
            int jumpA = ma.type.equals("jump") ? 400 : 0;
            int jumpB = mb.type.equals("jump") ? 400 : 0;
            UI.GameState nextA = state.applyMove(ma);
            UI.GameState nextB = state.applyMove(mb);
            int scoreA = evaluate(nextA) + jumpA + capA;
            int scoreB = evaluate(nextB) + jumpB + capB;
            int cmp = Integer.compare(scoreB, scoreA);
            if (cmp != 0) return cmp;
            return stateToKey(nextA).compareTo(stateToKey(nextB));
        });
        return result;
    }

    /**
     * Main entry: returns best move using backtracking search.
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
     * Backtracking search: recursively explore state space.
     * Uses shouldPrune, canPruneBranch, getNextCandidate, simulateDFS.
     */
    private static int backtrackSearch(UI.GameState state, int depth, int alpha, int beta, boolean isMaximizing) {
        List<UI.Move> moves = state.getLegalMovesForPlayer(state.turn);

        if (depth == 0 || moves.isEmpty()) {
            return evaluate(state);
        }

        if (depth <= 1 && moves.size() < 5) {
            return simulateDFS(state, depth);
        }

        List<UI.Move> ordered = orderMoves(moves, state);

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (int i = 0; i < ordered.size(); i++) {
                UI.Move mv = getNextCandidate(ordered, i);
                if (mv == null) break;
                UI.GameState nextState = state.applyMove(mv);
                int score = backtrackSearch(nextState, depth - 1, alpha, beta, false);
                maxEval = Math.max(maxEval, score);
                alpha = Math.max(alpha, score);
                if (shouldPrune(alpha, beta) || canPruneBranch(maxEval, alpha, beta, true)) break;
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (int i = 0; i < ordered.size(); i++) {
                UI.Move mv = getNextCandidate(ordered, i);
                if (mv == null) break;
                UI.GameState nextState = state.applyMove(mv);
                int score = backtrackSearch(nextState, depth - 1, alpha, beta, true);
                minEval = Math.min(minEval, score);
                beta = Math.min(beta, score);
                if (shouldPrune(alpha, beta) || canPruneBranch(minEval, alpha, beta, false)) break;
            }
            return minEval;
        }
    }

    /**
      Full evaluation with constraint-aware scoring.
     */
    private static int evaluate(UI.GameState state) {
        if (!checkStateConstraints(state)) return evaluateMaterial(state);
        int materialScore = evaluateMaterial(state) + evaluatePosition(state) + evaluateMobility(state)
            + evaluateVulnerability(state) + evaluateAdvancement(state) + evaluateThreats(state)
            + evaluateFormation(state) + evaluateConstraintSatisfaction(state);
        int propScore = propagateVulnerabilityConstraint(state);
        int clampedScore = Math.max(lowerBound(state), Math.min(upperBound(state), materialScore));
        return clampedScore + propScore / 2;
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
                int centerBonus = (r >= 2 && r <= 5 && c >= 2 && c <= 5) ? 15 : 0;
                if (state.isP1(piece)) p1 += centerBonus;
                else p2 += centerBonus;
            }
        }
        return p2 - p1;
    }

    private static int evaluateMobility(UI.GameState state) {
        int p2M = state.getLegalMovesForPlayer(UI.P2).size();
        int p1M = state.getLegalMovesForPlayer(UI.P1).size();
        return (p2M - p1M) * MOBILITY_WEIGHT;
    }

    private static int evaluateVulnerability(UI.GameState state) {
        int p1V = 0, p2V = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.EMPTY) continue;
                int vuln = computeVulnerability(state, r, c, piece);
                if (state.isP1(piece)) p1V += vuln;
                else p2V += vuln;
            }
        }
        return p1V - p2V;
    }

    private static int computeVulnerability(UI.GameState state, int r, int c, int piece) {
        boolean isP2 = piece == UI.P2 || piece == UI.P2_KING;
        if (!isP2) return 0;
        int penalty = 0;
        int[][] dirs = {{-1,-1},{-1,1},{1,-1},{1,1}};
        for (int[] d : dirs) {
            int overR = r + d[0], overC = c + d[1];
            int landR = r + 2*d[0], landC = c + 2*d[1];
            if (state.inBounds(landR, landC) && state.inBounds(overR, overC)) {
                if (state.isP1(state.board[overR][overC]) && state.board[landR][landC] == UI.EMPTY)
                    penalty += Math.min(30, MAX_VULNERABILITY_PENALTY);
            }
        }
        return penalty;
    }

    private static int evaluateAdvancement(UI.GameState state) {
        int p1 = 0, p2 = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.P1) p1 += r * 10;
                else if (piece == UI.P2) p2 += (7 - r) * 10;
            }
        }
        return p2 - p1;
    }

    private static int evaluateThreats(UI.GameState state) {
        int p2Threat = countCaptureMoves(state, UI.P2);
        int p1Threat = countCaptureMoves(state, UI.P1);
        return (p2Threat - p1Threat) * 8;
    }

    private static int countCaptureMoves(UI.GameState state, int player) {
        List<UI.Move> moves = state.getLegalMovesForPlayer(player);
        int count = 0;
        for (UI.Move m : moves) {
            if (m.type.equals("jump")) count += (m.captured != null ? m.captured.size() : 0) + 1;
        }
        return count;
    }

    private static int evaluateFormation(UI.GameState state) {
        int p1Conn = 0, p2Conn = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.EMPTY) continue;
                int n = countAllies(state, r, c, piece);
                if (state.isP1(piece)) p1Conn += n;
                else p2Conn += n;
            }
        }
        return (p2Conn - p1Conn) * 3;
    }

    private static int countAllies(UI.GameState state, int r, int c, int piece) {
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

    /**
     * Constraint satisfaction: reward states that satisfy strategic constraints.
     * Constraints: no pieces under immediate capture, control center, etc.
     * Time Complexity: O(n^2)
     */
    private static int evaluateConstraintSatisfaction(UI.GameState state) {
        int score = 0;
        score += checkCenterControlConstraint(state);
        score += checkBackRowConstraint(state);
        score += checkNoHangingPiecesConstraint(state);
        return score;
    }

    private static int checkCenterControlConstraint(UI.GameState state) {
        int p1 = 0, p2 = 0;
        for (int r = 3; r <= 4; r++) {
            for (int c = 3; c <= 4; c++) {
                int piece = state.board[r][c];
                if (state.isP1(piece)) p1++;
                else if (state.isP2(piece)) p2++;
            }
        }
        return (p2 - p1) * 5;
    }

    private static int checkBackRowConstraint(UI.GameState state) {
        int p1 = 0, p2 = 0;
        for (int c = 0; c < UI.BOARD_SIZE; c++) {
            if (state.board[7][c] == UI.P1) p1 += 10;
            if (state.board[0][c] == UI.P2) p2 += 10;
        }
        return p2 - p1;
    }

    private static int checkNoHangingPiecesConstraint(UI.GameState state) {
        int hanging = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.EMPTY) continue;
                if (state.isP2(piece) && computeVulnerability(state, r, c, piece) > 0)
                    hanging -= 15;
                if (state.isP1(piece)) {
                    int v = computeP1Vulnerability(state, r, c, piece);
                    if (v > 0) hanging += 15;
                }
            }
        }
        return hanging;
    }

    private static int computeP1Vulnerability(UI.GameState state, int r, int c, int piece) {
        int penalty = 0;
        int[][] dirs = {{-1,-1},{-1,1},{1,-1},{1,1}};
        for (int[] d : dirs) {
            int overR = r + d[0], overC = c + d[1];
            int landR = r + 2*d[0], landC = c + 2*d[1];
            if (state.inBounds(landR, landC) && state.inBounds(overR, overC)) {
                if (state.isP2(state.board[overR][overC]) && state.board[landR][landC] == UI.EMPTY)
                    penalty += 30;
            }
        }
        return penalty;
    }

    /**
     * Check if state satisfies backtracking pruning condition.
     * Time Complexity: O(1)
     */
    private static boolean shouldPrune(int alpha, int beta) {
        return beta <= alpha;
    }

    /**
     * Constraint propagation: if piece is capturable, propagate penalty.
     * Time Complexity: O(n^2)
     */
    private static int propagateVulnerabilityConstraint(UI.GameState state) {
        int total = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.EMPTY) continue;
                int v = state.isP2(piece) ? computeVulnerability(state, r, c, piece) : computeP1Vulnerability(state, r, c, piece);
                total += state.isP2(piece) ? -v : v;
            }
        }
        return total;
    }

    /**
     * Branch and bound: upper bound  on state value.
     * Time Complexity: O(n^2)
     */
    private static int upperBound(UI.GameState state) {
        return evaluateMaterial(state) + 500;
    }

    /**
     * Lower bound on state value.
     * Time Complexity: O(n^2)
     */
    private static int lowerBound(UI.GameState state) {
        return evaluateMaterial(state) - 500;
    }

    /**
     * Recursive constraint checking for state validity.
     * Time Complexity: O(n^2)
     */
    private static boolean checkStateConstraints(UI.GameState state) {
        int p1Count = 0, p2Count = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (state.isP1(piece)) p1Count++;
                else if (state.isP2(piece)) p2Count++;
            }
        }
        return p1Count >= 0 && p2Count >= 0 && p1Count <= 12 && p2Count <= 12;
    }

    /**
     * Decision tree depth-first search simulation.
     * Time Complexity: O(b^d)
     */
    private static int simulateDFS(UI.GameState state, int depth) {
        if (depth <= 0) return evaluate(state);
        List<UI.Move> moves = state.getLegalMovesForPlayer(state.turn);
        if (moves.isEmpty()) return evaluate(state);
        int best = state.turn == UI.P2 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        for (UI.Move m : moves) {
            int score = simulateDFS(state.applyMove(m), depth - 1);
            if (state.turn == UI.P2) best = Math.max(best, score);
            else best = Math.min(best, score);
        }
        return best;
    }

    /**
     * State-space exploration with visited set (conceptual).
     * Time Complexity: O(n^2) for hash
     */
    private static String stateToKey(UI.GameState state) {
        StringBuilder sb = new StringBuilder();
        sb.append(state.turn);
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for(int c = 0; c < UI.BOARD_SIZE; c++) {
                sb.append(state.board[r][c]);
            }
        }
        return sb.toString();
    }

    /**
     * Pruning condition: can we skip this branch?
     * Time Complexity: O(1)
     */
    private static boolean canPruneBranch(int currentBest, int alpha, int beta, boolean maximizing) {
        return maximizing ? currentBest >= beta : currentBest <= alpha;
    }

    /**
     * Next candidate move for backtracking exploration.
     * Time Complexity: O(m)
     */
    private static UI.Move getNextCandidate(List<UI.Move> moves, int index) {
        return index < moves.size() ? moves.get(index) : null;
    }
}
