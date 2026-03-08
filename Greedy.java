import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Greedy algorithm for Checkers - Easy Mode.
 * Uses greedy strategy with enhanced heuristics, multi-ply lookahead, and
 * activity selection style decision making.
 *
 * Implements:
 * - Greedy strategy selection: pick locally optimal choice at each step
 * - Activity selection: prioritize moves by multiple criteria
 * - Coin change style: prioritize high-value captures first
 * - Multi-criteria evaluation with weighted heuristics
 *
 * Time Complexity: O(m * m' * k) where m = our moves, m' = opponent moves, k = evaluation cost
 */
public class Greedy {

    // Piece valuation constants - greedy prioritization
    private static final int MAN_VAL = 100;
    private static final int KING_VAL = 320;
    private static final int CAPTURE_BONUS = 600;
    private static final int MULTI_CAPTURE_BONUS = 150;
    private static final int LOOKAHEAD_PLY = 1;
    private static final int MAX_ACTIVITY_CRITERIA = 8;

    // Positional weights for greedy center control
    private static final int[][] CENTER_WEIGHTS = {
        {0, 0, 0, 2, 2, 0, 0, 0},
        {0, 0, 3, 4, 4, 3, 0, 0},
        {0, 3, 5, 6, 6, 5, 3, 0},
        {2, 4, 6, 8, 8, 6, 4, 2},
        {2, 4, 6, 8, 8, 6, 4, 2},
        {0, 3, 5, 6, 6, 5, 3, 0},
        {0, 0, 3, 4, 4, 3, 0, 0},
        {0, 0, 0, 2, 2, 0, 0, 0}
    };

    // Advancement bonus per row for P2 (rows 0-7)
    private static final int[] P2_ROW_BONUS = {25, 20, 15, 10, 8, 5, 2, 0};
    private static final int[] P1_ROW_BONUS = {0, 2, 5, 8, 10, 15, 20, 25};

    /**
     * Main entry point: returns the best move using greedy strategy.
     * Combines activity selection, coin-change prioritization, and lookahead.
     *
     * Time Complexity: O(m * m') where m = our moves, m' = opponent moves per position
     *
     * @param state current game state
     * @return best move according to greedy heuristic, or null if no moves
     */
    public static UI.Move getBestMove(UI.GameState state) {
        List<UI.Move> moves = state.getLegalMovesForPlayer(UI.P2);
        if (moves.isEmpty()) return null;
        if (moves.size() == 1) return moves.get(0);

        List<UI.Move> ordered = orderMovesByActivitySelection(moves, state);
        UI.Move bestMove = ordered.get(0);
        int bestScore = Integer.MIN_VALUE;

        for (UI.Move move : ordered) {
            UI.GameState nextState = state.applyMove(move);
            int score = evaluateWithLookahead(nextState, LOOKAHEAD_PLY);
            score += computeMoveBonus(move);
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    /**
     * Compute bonus for a move based on greedy coin-change style prioritization.
     * Captures are worth more; multi-captures scale.
     * Time Complexity: O(1)
     */
    private static int computeMoveBonus(UI.Move move) {
        int bonus = 0;
        if (move.type.equals("jump")) {
            bonus += CAPTURE_BONUS;
            int captures = move.captured != null ? move.captured.size() : 0;
            bonus += captures * MULTI_CAPTURE_BONUS;
        }
        return bonus;
    }

    /**
     * Greedy lookahead: assume opponent plays best reply (minimax at depth 1).
     * Activity selection principle: consider opponent's best response before committing.
     *
     * Time Complexity: O(m) where m = opponent's legal moves
     */
    private static int evaluateWithLookahead(UI.GameState state, int ply) {
        if (ply <= 0) return evaluateFull(state);

        List<UI.Move> oppMoves = state.getLegalMovesForPlayer(state.turn);
        if (oppMoves.isEmpty()) return evaluateFull(state);

        int worstForUs = Integer.MAX_VALUE;
        for (UI.Move m : oppMoves) {
            UI.GameState next = state.applyMove(m);
            int score = evaluateFull(next);
            worstForUs = Math.min(worstForUs, score);
        }
        return worstForUs;
    }

    /**
     * Activity selection: order moves by multiple greedy criteria.
     * Similar to scheduling - pick the "most active" (best) move first.
     * Criteria: captures > advancement > center > safety
     *
     * Time Complexity: O(m log m)
     */
    private static List<UI.Move> orderMovesByActivitySelection(List<UI.Move> moves, UI.GameState state) {
        List<UI.Move> result = new ArrayList<>(moves);
        result.sort(new Comparator<UI.Move>() {
            @Override
            public int compare(UI.Move a, UI.Move b) {
                int scoreA = computeActivityScore(a, state);
                int scoreB = computeActivityScore(b, state);
                if (scoreA != scoreB) return Integer.compare(scoreB, scoreA);
                UI.GameState nextA = state.applyMove(a);
                UI.GameState nextB = state.applyMove(b);
                return Integer.compare(evaluateFull(nextB), evaluateFull(nextA));
            }
        });
        return result;
    }

    /**
     * Activity score: composite of multiple greedy criteria.
     * Time Complexity: O(1)
     */
    private static int computeActivityScore(UI.Move m, UI.GameState state) {
        int score = 0;
        if (m.type.equals("jump")) score += 1000;
        score += (m.captured != null ? m.captured.size() : 0) * 150;
        score += getAdvancementBonus(m.to.r, UI.P2);
        score += CENTER_WEIGHTS[m.to.r][m.to.c];
        return score;
    }

    private static int getAdvancementBonus(int row, int player) {
        if (player == UI.P2) return P2_ROW_BONUS[row];
        return P1_ROW_BONUS[row];
    }

    private static int captureCount(UI.Move m) {
        return m.captured != null ? m.captured.size() : 0;
    }

    /**
     * Full evaluation combining all greedy heuristics.
     * Positive = good for P2 (CPU), negative = good for P1 (Human).
     *
     * Time Complexity: O(n^2) for fixed 8x8 board
     */
    private static int evaluateFull(UI.GameState state) {
        int material = evaluateMaterial(state);
        int position = evaluatePosition(state);
        int mobility = countMobility(state);
        int safety = evaluateSafety(state);
        int advancement = evaluateAdvancement(state);
        int kingSafety = evaluateKingSafety(state);
        int threat = evaluateThreatBalance(state);
        int formation = evaluateFormation(state);
        int backRow = evaluateBackRowProtection(state);
        int diagonal = evaluateDiagonalControl(state);
        int tempo = evaluateTempo(state);
        int density = evaluatePieceDensity(state);
        int edge = evaluateEdgePieces(state);
        int promotion = evaluatePromotionThreat(state);
        int capturePot = evaluateCapturePotential(state);
        int kingMob = evaluateKingMobility(state);
        int pawnStruct = evaluatePawnStructure(state);

        return material + position + mobility + safety + advancement + kingSafety + threat + formation
            + backRow + diagonal + tempo + density + edge + promotion + capturePot + kingMob + pawnStruct;
    }

    /**
     * Material count: piece values with king premium.
     * Time Complexity: O(n^2)
     */
    private static int evaluateMaterial(UI.GameState state) {
        int p1Score = 0, p2Score = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.P1) p1Score += MAN_VAL;
                else if (piece == UI.P1_KING) p1Score += KING_VAL;
                else if (piece == UI.P2) p2Score += MAN_VAL;
                else if (piece == UI.P2_KING) p2Score += KING_VAL;
            }
        }
        return p2Score - p1Score;
    }

    /**
     * Positional evaluation: center control, double corners.
     * Time Complexity: O(n^2)
     */
    private static int evaluatePosition(UI.GameState state) {
        int p1Score = 0, p2Score = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.EMPTY) continue;
                int posVal = CENTER_WEIGHTS[r][c];
                int cornerVal = ((r == 0 || r == 7) && (c == 0 || c == 7)) ? 10 : 0;
                if (state.isP1(piece)) p1Score += posVal + cornerVal;
                else p2Score += posVal + cornerVal;
            }
        }
        return p2Score - p1Score;
    }

    /**
     * Mobility: difference in number of legal moves.
     * Time Complexity: O(m1 + m2) for move generation
     */
    private static int evaluateMobility(UI.GameState state) {
        int p2Moves = state.getLegalMovesForPlayer(UI.P2).size();
        int p1Moves = state.getLegalMovesForPlayer(UI.P1).size();
        return (p2Moves - p1Moves) * 5;
    }

    /**
     * Safety: penalize vulnerable pieces (can be captured).
     * Time Complexity: O(n^2)
     */
    private static int evaluateSafety(UI.GameState state) {
        int p1Vuln = 0, p2Vuln = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.EMPTY) continue;
                int vuln = countVulnerability(state, r, c, piece);
                if (state.isP1(piece)) p1Vuln += vuln;
                else p2Vuln += vuln;
            }
        }
        return p1Vuln - p2Vuln;
    }

    private static int countVulnerability(UI.GameState state, int r, int c, int piece) {
        boolean isP2 = piece == UI.P2 || piece == UI.P2_KING;
        if (!isP2) return 0;
        int penalty = 0;
        int[][] dirs = {{-1,-1},{-1,1},{1,-1},{1,1}};
        for (int[] d : dirs) {
            int overR = r + d[0], overC = c + d[1];
            int landR = r + 2*d[0], landC = c + 2*d[1];
            if (state.inBounds(landR, landC) && state.inBounds(overR, overC)) {
                if (state.isP1(state.board[overR][overC]) && state.board[landR][landC] == UI.EMPTY)
                    penalty += 25;
            }
        }
        return penalty;
    }

    /**
     * Advancement: reward pieces moving toward promotion.
     * Time Complexity: O(n^2)
     */
    private static int evaluateAdvancement(UI.GameState state) {
        int p1Score = 0, p2Score = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.P1) p1Score += P1_ROW_BONUS[r];
                else if (piece == UI.P2) p2Score += P2_ROW_BONUS[r];
            }
        }
        return p2Score - p1Score;
    }

    /**
     * King safety: keep kings away from edges when threatened.
     * Time Complexity: O(n^2)
     */
    private static int evaluateKingSafety(UI.GameState state) {
        int bonus = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.P2_KING) {
                    int distFromEdge = Math.min(Math.min(r, 7-r), Math.min(c, 7-c));
                    bonus += distFromEdge * 2;
                } else if (piece == UI.P1_KING) {
                    int distFromEdge = Math.min(Math.min(r, 7-r), Math.min(c, 7-c));
                    bonus -= distFromEdge * 2;
                }
            }
        }
        return bonus;
    }

    /**
     * Threat balance: pieces that can capture vs pieces under threat.
     * Time Complexity: O(n^2)
     */
    private static int evaluateThreatBalance(UI.GameState state) {
        int p2Threats = countCaptureThreats(state, UI.P2);
        int p1Threats = countCaptureThreats(state, UI.P1);
        return (p2Threats - p1Threats) * 8;
    }

    private static int countCaptureThreats(UI.GameState state, int player) {
        List<UI.Move> moves = state.getLegalMovesForPlayer(player);
        int captures = 0;
        for (UI.Move m : moves) {
            if (m.type.equals("jump")) captures += captureCount(m) + 1;
        }
        return captures;
    }

    /**
     * Formation: reward connected pieces, penalize isolated.
     * Time Complexity: O(n^2)
     */
    private static int evaluateFormation(UI.GameState state) {
        int p1Conn = 0, p2Conn = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.EMPTY) continue;
                int neighbors = countFriendlyNeighbors(state, r, c, piece);
                if (state.isP1(piece)) p1Conn += neighbors;
                else p2Conn += neighbors;
            }
        }
        return (p2Conn - p1Conn) * 3;
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

    /**
     * Legacy evaluate for compatibility - delegates to full evaluation.
     * Time Complexity: O(n^2)
     */
    private static int evaluate(UI.GameState state) {
        return evaluateFull(state);
    }

    private static int countMobility(UI.GameState state) {
        return evaluateMobility(state);
    }

    /**
     * Back row protection: reward keeping pieces on home row.
     * Time Complexity: O(n)
     */
    private static int evaluateBackRowProtection(UI.GameState state) {
        int p1Back = 0, p2Back = 0;
        for (int c = 0; c < UI.BOARD_SIZE; c++) {
            if (state.board[7][c] == UI.P1) p1Back += 15;
            if (state.board[0][c] == UI.P2) p2Back += 15;
        }
        return p2Back - p1Back;
    }

    /**
     * Diagonal control: reward controlling main diagonals.
     * Time Complexity: O(n)
     */
    private static int evaluateDiagonalControl(UI.GameState state) {
        int p1Diag = 0, p2Diag = 0;
        for (int i = 0; i < UI.BOARD_SIZE; i++) {
            int piece1 = state.board[i][i];
            int piece2 = state.board[i][7-i];
            if (state.isP1(piece1)) p1Diag += 2;
            else if (state.isP2(piece1)) p2Diag += 2;
            if (state.isP1(piece2)) p1Diag += 2;
            else if (state.isP2(piece2)) p2Diag += 2;
        }
        return p2Diag - p1Diag;
    }

    /**
     * Tempo: reward having the move (initiative).
     * Time Complexity: O(1)
     */
    private static int evaluateTempo(UI.GameState state) {
        return state.turn == UI.P2 ? 5 : -5;
    }

    /**
     * Piece density: reward clustering in center.
     * Time Complexity: O(n^2)
     */
    private static int evaluatePieceDensity(UI.GameState state) {
        int p1Center = 0, p2Center = 0;
        for (int r = 2; r <= 5; r++) {
            for (int c = 2; c <= 5; c++) {
                int piece = state.board[r][c];
                if (state.isP1(piece)) p1Center++;
                else if (state.isP2(piece)) p2Center++;
            }
        }
        return (p2Center - p1Center) * 4;
    }

    /**
     * Edge piece penalty: pieces on edge are often weak.
     * Time Complexity: O(n)
     */
    private static int evaluateEdgePieces(UI.GameState state) {
        int p1Edge = 0, p2Edge = 0;
        for (int i = 0; i < UI.BOARD_SIZE; i++) {
            if (state.isP1(state.board[i][0]) || state.isP1(state.board[i][7])) p1Edge++;
            if (state.isP2(state.board[i][0]) || state.isP2(state.board[i][7])) p2Edge++;
            if (state.isP1(state.board[0][i]) || state.isP1(state.board[7][i])) p1Edge++;
            if (state.isP2(state.board[0][i]) || state.isP2(state.board[7][i])) p2Edge++;
        }
        return (p1Edge - p2Edge) * 3;
    }

    /**
     * Promotion threat: pieces close to promotion row.
     * Time Complexity: O(n^2)
     */
    private static int evaluatePromotionThreat(UI.GameState state) {
        int p1Threat = 0, p2Threat = 0;
        for (int c = 0; c < UI.BOARD_SIZE; c++) {
            if (state.board[1][c] == UI.P1) p1Threat += 20;
            if (state.board[6][c] == UI.P2) p2Threat += 20;
        }
        return p2Threat - p1Threat;
    }

    /**
     * Capture chain potential: positions that enable multi-jumps.
     * Time Complexity: O(n^2)
     */
    private static int evaluateCapturePotential(UI.GameState state) {
        List<UI.Move> p2Moves = state.getLegalMovesForPlayer(UI.P2);
        List<UI.Move> p1Moves = state.getLegalMovesForPlayer(UI.P1);
        int p2Cap = 0, p1Cap = 0;
        for (UI.Move m : p2Moves) {
            if (m.type.equals("jump")) p2Cap += 50 + captureCount(m) * 30;
        }
        for (UI.Move m : p1Moves) {
            if (m.type.equals("jump")) p1Cap += 50 + captureCount(m) * 30;
        }
        return p2Cap - p1Cap;
    }

    /**
     * King mobility: kings should have more movement options.
     * Time Complexity: O(n^2)
     */
    private static int evaluateKingMobility(UI.GameState state) {
        int p1KingMob = 0, p2KingMob = 0;
        for (int r = 0; r < UI.BOARD_SIZE; r++) {
            for (int c = 0; c < UI.BOARD_SIZE; c++) {
                int piece = state.board[r][c];
                if (piece == UI.P1_KING) {
                    p1KingMob += countKingMoves(state, r, c);
                } else if (piece == UI.P2_KING) {
                    p2KingMob += countKingMoves(state, r, c);
                }
            }
        }
        return (p2KingMob - p1KingMob) * 2;
    }

    private static int countKingMoves(UI.GameState state, int r, int c) {
        int count = 0;
        int[][] dirs = {{-1,-1},{-1,1},{1,-1},{1,1}};
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            if (state.inBounds(nr, nc) && state.board[nr][nc] == UI.EMPTY) count++;
        }
        return count;
    }

    /**
     * Pawn structure: avoid doubled pawns, reward passed pawns.
     * Time Complexity: O(n^2)
     */
    private static int evaluatePawnStructure(UI.GameState state) {
        int p1Doubled = 0, p2Doubled = 0;
        for (int c = 0; c < UI.BOARD_SIZE; c++) {
            int p1InCol = 0, p2InCol = 0;
            for (int r = 0; r < UI.BOARD_SIZE; r++) {
                if (state.board[r][c] == UI.P1) p1InCol++;
                else if (state.board[r][c] == UI.P2) p2InCol++;
            }
            if (p1InCol > 1) p1Doubled += (p1InCol - 1) * 5;
            if (p2InCol > 1) p2Doubled += (p2InCol - 1) * 5;
        }
        return p1Doubled - p2Doubled;
    }
}
