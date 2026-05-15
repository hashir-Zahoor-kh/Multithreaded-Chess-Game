// Plain-Java test runner for GameState. No JUnit, no JavaFX.
// Run: javac GameState.java GameStateTest.java && java GameStateTest

public class GameStateTest {
    static int passed = 0;
    static int failed = 0;

    public static void main(String[] args) {
        testInitialFenIsSixFields();
        testFenAfter_e4_setsEnPassantTarget();
        testFenAfter_e4_e5_clearsEnPassantAndIncrementsFullmove();
        testFenAfter_Nf3_incrementsHalfmoveClock();
        testCaptureResetsHalfmoveClock();
        testKingMoveRevokesBothCastlingRights();
        testRookMoveFromH1RevokesKingsideOnly();
        testRookMoveFromA1RevokesQueensideOnly();
        testCapturedRookOnA8RevokesBlackQueenside();
        testRoundTripSixFieldFen();

        // P0 #3: promotion suffix
        testPromotionDefaultsToQueenWhenNoSuffix();
        testWhitePromotionToKnight();
        testWhitePromotionToRook();
        testWhitePromotionToBishop();
        testBlackPromotionToKnight();
        testInvalidPromotionSuffixDefaultsToQueen();
        testPromotionSuffixIgnoredOnNonPromotingMove();

        // P0 #6: king capture is now an unreachable state
        testKingCaptureThrows();

        // P0 #5: draw detection
        testFiftyMoveRuleDraw();
        testThreefoldRepetitionDraw();
        testInsufficientMaterial_KvK();
        testInsufficientMaterial_KBvK();
        testInsufficientMaterial_KNvK();
        testInsufficientMaterial_KBvKB_sameColor();
        testNotInsufficient_KBvKB_differentColor();
        testNotInsufficient_KRvK();
        testRepetitionHistoryClearsOnPawnMove();

        // P0 #2: en passant
        testWhiteEnPassantCapture();
        testBlackEnPassantCapture();
        testCannotEnPassantWithoutTarget();
        testEnPassantRemovesCapturedPawn();
        testEnPassantExposingOwnKingIsRejected();

        // P0 #1: castling
        testWhiteKingsideCastle();
        testWhiteQueensideCastle();
        testBlackKingsideCastle();
        testBlackQueensideCastle();
        testCannotCastleWithoutRight();
        testCannotCastleThroughOwnPiece();
        testCannotCastleWhileInCheck();
        testCannotCastleThroughAttackedSquare();
        testCannotCastleOntoAttackedSquare();
        testCastleQueensideStillBlockedByB1Knight();
        testCastleRevokesBothRightsForThatColor();
        testCastleIncrementsHalfmoveClock();

        System.out.println();
        System.out.println("Passed: " + passed + "  Failed: " + failed);
        if (failed > 0) System.exit(1);
    }

    // --- Tests ---

    static void testInitialFenIsSixFields() {
        GameState g = new GameState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w");
        assertEq("initial FEN",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            g.getFen());
    }

    static void testFenAfter_e4_setsEnPassantTarget() {
        GameState g = new GameState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w");
        require("1.e4 legal", g.applyMove("e2e4"));
        assertEq("FEN after 1.e4",
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
            g.getFen());
    }

    static void testFenAfter_e4_e5_clearsEnPassantAndIncrementsFullmove() {
        GameState g = new GameState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w");
        require("1.e4", g.applyMove("e2e4"));
        require("1...e5", g.applyMove("e7e5"));
        assertEq("FEN after 1.e4 e5",
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2",
            g.getFen());
    }

    static void testFenAfter_Nf3_incrementsHalfmoveClock() {
        GameState g = new GameState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w");
        require("1.Nf3", g.applyMove("g1f3"));
        assertEq("FEN after 1.Nf3",
            "rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R b KQkq - 1 1",
            g.getFen());
    }

    static void testCaptureResetsHalfmoveClock() {
        // Position with knights ready to capture each other; halfmove is at 5 in this FEN
        // Setup: white knight on d4, black knight on e6, white to move, halfmove=5
        GameState g = new GameState(
            "8/8/4n3/8/3N4/8/8/4K2k w - - 5 10");
        // Sanity: halfmove clock came in as 5
        require("Nxe6 capture legal", g.applyMove("d4e6"));
        // After capture halfmove should reset to 0
        String[] parts = g.getFen().split(" ");
        assertEq("halfmove resets on capture", "0", parts[4]);
    }

    static void testKingMoveRevokesBothCastlingRights() {
        // White king on e1, free to step to e2; rooks still on a1/h1
        GameState g = new GameState("4k3/8/8/8/8/8/8/R3K2R w KQ - 0 1");
        require("Ke2 legal", g.applyMove("e1e2"));
        String[] parts = g.getFen().split(" ");
        assertEq("white loses both castling rights after king move", "-", parts[2]);
    }

    static void testRookMoveFromH1RevokesKingsideOnly() {
        GameState g = new GameState("4k3/8/8/8/8/8/8/R3K2R w KQ - 0 1");
        require("Rh1-h2 legal", g.applyMove("h1h2"));
        String[] parts = g.getFen().split(" ");
        assertEq("white loses K only when h1 rook moves", "Q", parts[2]);
    }

    static void testRookMoveFromA1RevokesQueensideOnly() {
        GameState g = new GameState("4k3/8/8/8/8/8/8/R3K2R w KQ - 0 1");
        require("Ra1-a2 legal", g.applyMove("a1a2"));
        String[] parts = g.getFen().split(" ");
        assertEq("white loses Q only when a1 rook moves", "K", parts[2]);
    }

    static void testCapturedRookOnA8RevokesBlackQueenside() {
        // White queen on a4 captures black rook on a8 — white's own rights untouched.
        // Black FEN has only "q" rights (h8 rook absent), so the only thing that should
        // change is that "q" disappears.
        GameState g = new GameState("r3k3/8/8/8/Q7/8/8/R3K2R w KQq - 0 1");
        require("Qxa8 legal", g.applyMove("a4a8"));
        String[] parts = g.getFen().split(" ");
        assertEq("captured a8 rook revokes only black q", "KQ", parts[2]);
    }

    static void testRoundTripSixFieldFen() {
        String fen = "r3k2r/pppppppp/8/8/8/8/PPPPPPPP/R3K2R w KQkq - 7 12";
        GameState g = new GameState(fen);
        assertEq("six-field FEN round-trips", fen, g.getFen());
    }

    // --- P0 #3: promotion-suffix tests ---

    // Standard promotion test position: white pawn on e7, kings on a1/h8 (far apart),
    // empty board otherwise — single-step promotion to e8 is legal.
    static GameState whitePromotionPosition() {
        return new GameState("7k/4P3/8/8/8/8/8/K7 w - - 0 1");
    }

    static GameState blackPromotionPosition() {
        return new GameState("K7/8/8/8/8/8/4p3/7k b - - 0 1");
    }

    static char pieceAt(GameState g, String square) {
        int col = square.charAt(0) - 'a';
        int row = 8 - (square.charAt(1) - '0');
        String[] rows = g.getFen().split(" ")[0].split("/");
        int c = 0;
        for (char ch : rows[row].toCharArray()) {
            if (Character.isDigit(ch)) {
                c += Character.getNumericValue(ch);
            } else {
                if (c == col) return ch;
                c++;
            }
        }
        return '\0';
    }

    static void testPromotionDefaultsToQueenWhenNoSuffix() {
        GameState g = whitePromotionPosition();
        require("e7e8 (no suffix) legal", g.applyMove("e7e8"));
        assertEq("no suffix → queen", "Q", String.valueOf(pieceAt(g, "e8")));
    }

    static void testWhitePromotionToKnight() {
        GameState g = whitePromotionPosition();
        require("e7e8n legal", g.applyMove("e7e8n"));
        assertEq("white promotes to knight", "N", String.valueOf(pieceAt(g, "e8")));
    }

    static void testWhitePromotionToRook() {
        GameState g = whitePromotionPosition();
        require("e7e8r legal", g.applyMove("e7e8r"));
        assertEq("white promotes to rook", "R", String.valueOf(pieceAt(g, "e8")));
    }

    static void testWhitePromotionToBishop() {
        GameState g = whitePromotionPosition();
        require("e7e8b legal", g.applyMove("e7e8b"));
        assertEq("white promotes to bishop", "B", String.valueOf(pieceAt(g, "e8")));
    }

    static void testBlackPromotionToKnight() {
        GameState g = blackPromotionPosition();
        require("e2e1n legal", g.applyMove("e2e1n"));
        assertEq("black promotes to knight (lowercase)", "n", String.valueOf(pieceAt(g, "e1")));
    }

    static void testInvalidPromotionSuffixDefaultsToQueen() {
        GameState g = whitePromotionPosition();
        require("e7e8x (invalid suffix) still applies", g.applyMove("e7e8x"));
        assertEq("invalid suffix → queen", "Q", String.valueOf(pieceAt(g, "e8")));
    }

    static void testPromotionSuffixIgnoredOnNonPromotingMove() {
        // Suffix on a normal pawn push must not promote anything.
        GameState g = new GameState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w");
        require("e2e4q (bogus suffix on non-promo) legal", g.applyMove("e2e4q"));
        assertEq("non-promo suffix ignored, pawn stays a pawn", "P", String.valueOf(pieceAt(g, "e4")));
    }

    // --- P0 #1: castling tests ---

    static void testWhiteKingsideCastle() {
        GameState g = new GameState("4k3/8/8/8/8/8/8/4K2R w K - 0 1");
        require("O-O legal", g.applyMove("e1g1"));
        assertEq("white kingside castle FEN",
            "4k3/8/8/8/8/8/8/5RK1 b - - 1 1",
            g.getFen());
    }

    static void testWhiteQueensideCastle() {
        GameState g = new GameState("4k3/8/8/8/8/8/8/R3K3 w Q - 0 1");
        require("O-O-O legal", g.applyMove("e1c1"));
        assertEq("white queenside castle FEN",
            "4k3/8/8/8/8/8/8/2KR4 b - - 1 1",
            g.getFen());
    }

    static void testBlackKingsideCastle() {
        GameState g = new GameState("4k2r/8/8/8/8/8/8/4K3 b k - 0 1");
        require("...O-O legal", g.applyMove("e8g8"));
        assertEq("black kingside castle FEN",
            "5rk1/8/8/8/8/8/8/4K3 w - - 1 2",
            g.getFen());
    }

    static void testBlackQueensideCastle() {
        GameState g = new GameState("r3k3/8/8/8/8/8/8/4K3 b q - 0 1");
        require("...O-O-O legal", g.applyMove("e8c8"));
        assertEq("black queenside castle FEN",
            "2kr4/8/8/8/8/8/8/4K3 w - - 1 2",
            g.getFen());
    }

    static void testCannotCastleWithoutRight() {
        GameState g = new GameState("4k3/8/8/8/8/8/8/4K2R w - - 0 1");
        boolean ok = g.applyMove("e1g1");
        if (ok) {
            failed++;
            System.out.println("FAIL  cannot castle without right (move was accepted)");
        } else {
            passed++;
            System.out.println("PASS  cannot castle without right");
        }
    }

    static void testCannotCastleThroughOwnPiece() {
        // Knight on f1 blocks kingside castle
        GameState g = new GameState("4k3/8/8/8/8/8/8/4KN1R w K - 0 1");
        boolean ok = g.applyMove("e1g1");
        if (ok) { failed++; System.out.println("FAIL  cannot castle through own piece (accepted)"); }
        else    { passed++; System.out.println("PASS  cannot castle through own piece"); }
    }

    static void testCannotCastleWhileInCheck() {
        // Black rook on e2 gives check
        GameState g = new GameState("4k3/8/8/8/8/8/4r3/4K2R w K - 0 1");
        boolean ok = g.applyMove("e1g1");
        if (ok) { failed++; System.out.println("FAIL  cannot castle while in check (accepted)"); }
        else    { passed++; System.out.println("PASS  cannot castle while in check"); }
    }

    static void testCannotCastleThroughAttackedSquare() {
        // Black rook on f2 attacks f1 — king would transit through check
        GameState g = new GameState("4k3/8/8/8/8/8/5r2/4K2R w K - 0 1");
        boolean ok = g.applyMove("e1g1");
        if (ok) { failed++; System.out.println("FAIL  cannot castle through attacked square (accepted)"); }
        else    { passed++; System.out.println("PASS  cannot castle through attacked square"); }
    }

    static void testCannotCastleOntoAttackedSquare() {
        // Black rook on g2 attacks g1 — king would land in check
        GameState g = new GameState("4k3/8/8/8/8/8/6r1/4K2R w K - 0 1");
        boolean ok = g.applyMove("e1g1");
        if (ok) { failed++; System.out.println("FAIL  cannot castle onto attacked square (accepted)"); }
        else    { passed++; System.out.println("PASS  cannot castle onto attacked square"); }
    }

    static void testCastleQueensideStillBlockedByB1Knight() {
        // Knight on b1 blocks queenside castle (rook would have to traverse b1)
        GameState g = new GameState("4k3/8/8/8/8/8/8/RN2K3 w Q - 0 1");
        boolean ok = g.applyMove("e1c1");
        if (ok) { failed++; System.out.println("FAIL  queenside castle blocked by b1 knight (accepted)"); }
        else    { passed++; System.out.println("PASS  queenside castle blocked by b1 knight"); }
    }

    static void testCastleRevokesBothRightsForThatColor() {
        // White had both KQ; after kingside castle white should have neither
        GameState g = new GameState("4k3/8/8/8/8/8/8/R3K2R w KQ - 0 1");
        require("O-O legal with KQ rights", g.applyMove("e1g1"));
        String[] parts = g.getFen().split(" ");
        assertEq("white loses both rights after castling", "-", parts[2]);
    }

    static void testCastleIncrementsHalfmoveClock() {
        GameState g = new GameState("4k3/8/8/8/8/8/8/4K2R w K - 4 1");
        require("O-O legal", g.applyMove("e1g1"));
        String[] parts = g.getFen().split(" ");
        assertEq("castling is not pawn move/capture, halfmove++", "5", parts[4]);
    }

    // --- P0 #2: en passant tests ---

    static void testWhiteEnPassantCapture() {
        // Position after 1.e4 d5 2.e5 f5 — Black just played f7-f5, EP target f6
        GameState g = new GameState(
            "rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3");
        require("exf6 e.p. legal", g.applyMove("e5f6"));
        assertEq("white EP capture FEN",
            "rnbqkbnr/ppp1p1pp/5P2/3p4/8/8/PPPP1PPP/RNBQKBNR b KQkq - 0 3",
            g.getFen());
    }

    static void testBlackEnPassantCapture() {
        // Synthetic: Black pawn on e4, White just played d2-d4, EP target d3
        GameState g = new GameState("4k3/8/8/8/3Pp3/8/8/4K3 b - d3 0 1");
        require("...exd3 e.p. legal", g.applyMove("e4d3"));
        assertEq("black EP capture FEN",
            "4k3/8/8/8/8/3p4/8/4K3 w - - 0 2",
            g.getFen());
    }

    static void testCannotEnPassantWithoutTarget() {
        // Same geometry as above but EP target cleared — move must be rejected
        GameState g = new GameState("4k3/8/8/8/3Pp3/8/8/4K3 b - - 0 1");
        boolean ok = g.applyMove("e4d3");
        if (ok) { failed++; System.out.println("FAIL  EP rejected without target (accepted)"); }
        else    { passed++; System.out.println("PASS  EP rejected without target"); }
    }

    static void testEnPassantRemovesCapturedPawn() {
        // After EP, the captured pawn must be gone from its original square
        GameState g = new GameState("4k3/8/8/8/3Pp3/8/8/4K3 b - d3 0 1");
        require("...exd3 e.p.", g.applyMove("e4d3"));
        // d4 (the captured white pawn's square) must be empty
        char atD4 = pieceAt(g, "d4");
        if (atD4 == '\0') { passed++; System.out.println("PASS  EP removes captured pawn from d4"); }
        else { failed++; System.out.println("FAIL  EP did not remove captured pawn (d4=" + atD4 + ")"); }
    }

    static void testEnPassantExposingOwnKingIsRejected() {
        // White king on h5, white pawn on e5, black pawn just played d7-d5,
        // black rook on a5. Both pawns currently block the rook from attacking
        // the white king. If white plays exd6 e.p., both blockers vanish along
        // rank 5 and the king is in check. The move must be rejected.
        GameState g = new GameState("7k/8/8/r2pP2K/8/8/8/8 w - d6 0 1");
        boolean ok = g.applyMove("e5d6");
        if (ok) { failed++; System.out.println("FAIL  EP that exposes king accepted"); }
        else    { passed++; System.out.println("PASS  EP that exposes own king rejected"); }
    }

    // --- P0 #5: draw-detection tests ---

    static void testFiftyMoveRuleDraw() {
        // Halfmove at 99; one more non-pawn / non-capture move pushes it to 100.
        // K+R vs K is sufficient material, so this isolates the 50-move rule.
        GameState g = new GameState("4k3/8/8/8/8/8/8/R3K3 w - - 99 50");
        require("Kf1 (50th move)", g.applyMove("e1f1"));
        require("game over", g.isGameOver());
        assertEq("50-move rule fires", "Draw by 50-move rule!", g.getResult());
    }

    static void testThreefoldRepetitionDraw() {
        // Knights bouncing — back to start every two full moves.
        GameState g = new GameState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w");
        String[] cycle = {"g1f3", "g8f6", "f3g1", "f6g8"};
        // Two full cycles return to the starting position twice more (3 occurrences total).
        for (int i = 0; i < cycle.length * 2; i++) {
            require("move " + cycle[i % 4], g.applyMove(cycle[i % 4]));
        }
        require("game over after 3rd repetition", g.isGameOver());
        assertEq("threefold repetition fires",
            "Draw by Threefold Repetition!", g.getResult());
    }

    static void testInsufficientMaterial_KvK() {
        // After Kxd2 only the two kings remain.
        GameState g = new GameState("4k3/8/8/8/8/8/3p4/4K3 w - - 0 1");
        require("Kxd2", g.applyMove("e1d2"));
        require("game over", g.isGameOver());
        assertEq("K vs K is a draw",
            "Draw by Insufficient Material!", g.getResult());
    }

    static void testInsufficientMaterial_KBvK() {
        // After Bxc2 white has K + B vs lone black king.
        GameState g = new GameState("4k3/8/8/8/8/8/2p5/3BK3 w - - 0 1");
        require("Bxc2", g.applyMove("d1c2"));
        require("game over", g.isGameOver());
        assertEq("K+B vs K insufficient",
            "Draw by Insufficient Material!", g.getResult());
    }

    static void testInsufficientMaterial_KNvK() {
        // After Nxc3 white has K + N vs lone black king.
        GameState g = new GameState("4k3/8/8/8/8/2p5/8/3NK3 w - - 0 1");
        require("Nxc3", g.applyMove("d1c3"));
        require("game over", g.isGameOver());
        assertEq("K+N vs K insufficient",
            "Draw by Insufficient Material!", g.getResult());
    }

    static void testInsufficientMaterial_KBvKB_sameColor() {
        // Both bishops on dark squares (e3 and d6 are both dark).
        GameState g = new GameState("4k3/8/3b4/8/8/4B3/8/4K3 w - - 0 1");
        require("Kf1 (no material change)", g.applyMove("e1f1"));
        require("game over", g.isGameOver());
        assertEq("K+B vs K+B same color insufficient",
            "Draw by Insufficient Material!", g.getResult());
    }

    static void testNotInsufficient_KBvKB_differentColor() {
        // White bishop on e3 (dark), black bishop on c6 (light) — opposite colors.
        GameState g = new GameState("4k3/8/2b5/8/8/4B3/8/4K3 w - - 0 1");
        require("Kf1", g.applyMove("e1f1"));
        if (g.isGameOver()) {
            failed++;
            System.out.println("FAIL  K+B vs K+B different colors should NOT be a draw");
        } else {
            passed++;
            System.out.println("PASS  K+B vs K+B different colors not a draw");
        }
    }

    static void testNotInsufficient_KRvK() {
        // K + R vs K is winnable; not a draw by material.
        GameState g = new GameState("4k3/8/8/8/8/8/3p4/3RK3 w - - 0 1");
        require("Rxd2", g.applyMove("d1d2"));
        if (g.isGameOver()) {
            failed++;
            System.out.println("FAIL  K+R vs K should NOT be a draw");
        } else {
            passed++;
            System.out.println("PASS  K+R vs K not a draw");
        }
    }

    static void testRepetitionHistoryClearsOnPawnMove() {
        // Repeat-the-same-knight-cycle once, then push a pawn (irreversible),
        // then repeat the cycle again — should NOT trigger threefold because
        // the pawn move severed the history.
        GameState g = new GameState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w");
        // Two cycles → starting position has appeared twice
        String[] cycle = {"g1f3", "g8f6", "f3g1", "f6g8"};
        for (int i = 0; i < cycle.length; i++) require("c1." + cycle[i], g.applyMove(cycle[i]));
        // Pawn move (white)
        require("a2a3", g.applyMove("a2a3"));
        // Black response that doesn't recreate any prior position
        require("a7a6", g.applyMove("a7a6"));
        // Another full cycle — but the previously-seen pre-pawn positions are gone
        for (int i = 0; i < cycle.length; i++) require("c2." + cycle[i], g.applyMove(cycle[i]));
        if (g.isGameOver()) {
            failed++;
            System.out.println("FAIL  history should have been cleared on pawn move ("
                + g.getResult() + ")");
        } else {
            passed++;
            System.out.println("PASS  pawn move clears repetition history");
        }
    }

    // --- P0 #6: king-capture safety net ---

    static void testKingCaptureThrows() {
        // Malformed position: white to move, but the black king is in check
        // (queen on a4 attacks b3). A legal game can never reach this state
        // because the prior black move would have been rejected. If we ever
        // do reach it, the new applyMove guard must throw rather than silently
        // "win" by capturing the king.
        GameState g = new GameState("8/8/8/8/Q7/1k6/8/4K3 w - - 0 1");
        try {
            g.applyMove("a4b3");
            failed++;
            System.out.println("FAIL  king capture should have thrown IllegalStateException");
        } catch (IllegalStateException e) {
            passed++;
            System.out.println("PASS  king capture throws IllegalStateException");
        }
    }

    // --- Tiny assertion helpers ---

    static void assertEq(String label, String expected, String actual) {
        if (expected.equals(actual)) {
            passed++;
            System.out.println("PASS  " + label);
        } else {
            failed++;
            System.out.println("FAIL  " + label);
            System.out.println("        expected: " + expected);
            System.out.println("        actual:   " + actual);
        }
    }

    static void require(String label, boolean cond) {
        if (!cond) {
            failed++;
            System.out.println("FAIL  precondition: " + label);
        }
    }
}
