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
