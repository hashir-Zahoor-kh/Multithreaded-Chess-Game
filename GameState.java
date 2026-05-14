//Hashir Zahoor Ur Rahman
//Game State 

import java.util.Optional;

//Class that deals with the complete game logic and game state
public class GameState {
    private static final int SIZE = 8;
    private char[][] board = new char[SIZE][SIZE];
    
    //This will be true if it is white's turn
    private boolean whiteToMove;
    private int whiteMoveCount = 0;
    private int blackMoveCount = 0;
    public volatile boolean gameOver = false; // Made public volatile for thread-safe access
    private String result = "";

    // Full FEN state (P0 #4)
    private boolean whiteCanCastleKingside  = true;
    private boolean whiteCanCastleQueenside = true;
    private boolean blackCanCastleKingside  = true;
    private boolean blackCanCastleQueenside = true;
    private String  enPassantTarget = "-";   // e.g. "e3" right after 1.e4, else "-"
    private int     halfmoveClock   = 0;     // resets on pawn move or capture
    private int     fullmoveNumber  = 1;     // increments after Black's move

    public GameState(String startFen) {
        String[] parts = startFen.split(" ");
        loadFromFen(parts[0]);
        whiteToMove = parts.length>1 && parts[1].equalsIgnoreCase("w");

        // Parse remaining FEN fields if present; otherwise leave defaults
        // (defaults assume game start: full castling rights, no EP, clocks 0/1)
        if (parts.length > 2) parseCastlingRights(parts[2]);
        if (parts.length > 3) enPassantTarget = parts[3].isEmpty() ? "-" : parts[3];
        if (parts.length > 4) {
            try { halfmoveClock = Integer.parseInt(parts[4]); } catch (NumberFormatException ignored) {}
        }
        if (parts.length > 5) {
            try { fullmoveNumber = Integer.parseInt(parts[5]); } catch (NumberFormatException ignored) {}
        }
    }

    private void parseCastlingRights(String rights) {
        whiteCanCastleKingside  = rights.indexOf('K') >= 0;
        whiteCanCastleQueenside = rights.indexOf('Q') >= 0;
        blackCanCastleKingside  = rights.indexOf('k') >= 0;
        blackCanCastleQueenside = rights.indexOf('q') >= 0;
    }

    private String castlingRightsString() {
        StringBuilder sb = new StringBuilder();
        if (whiteCanCastleKingside)  sb.append('K');
        if (whiteCanCastleQueenside) sb.append('Q');
        if (blackCanCastleKingside)  sb.append('k');
        if (blackCanCastleQueenside) sb.append('q');
        return sb.length() == 0 ? "-" : sb.toString();
    }
    
    //Converts the FEN seperates it into 8 parts and fills the internal board accordingly.
    private void loadFromFen(String fenPlacement) {
        String[] rows = fenPlacement.split("/");
        for (int r = 0; r < SIZE; r++) {
            int c = 0;
            for (char ch : rows[r].toCharArray()) {
                if (Character.isDigit(ch)) {
                    c += ch - '0';
                } else {
                    board[r][c++] = ch;
                }
            }
        }
    }

    //Took from here, https://chatgpt.com/share/681a4bec-04e8-8007-be04-f62341fee7f8
    public String getFen() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < SIZE; r++) {
            int empty = 0;
            for (int c = 0; c < SIZE; c++) {
                char ch = board[r][c];
                if (ch == '\0') empty++;
                else {
                    if (empty>0) { sb.append(empty); empty=0; }
                    sb.append(ch);
                }
            }
            if (empty>0) sb.append(empty);
            if (r<7) sb.append('/');
        }
        sb.append(' ').append(whiteToMove?'w':'b');
        sb.append(' ').append(castlingRightsString());
        sb.append(' ').append(enPassantTarget);
        sb.append(' ').append(halfmoveClock);
        sb.append(' ').append(fullmoveNumber);
        return sb.toString();
    }

    public String getTurn() {
        return whiteToMove ? "White" : "Black";
    }
    
    // Taken from here, https://chatgpt.com/share/681a4bec-04e8-8007-be04-f62341fee7f8
    public synchronized boolean applyMove(String uci) {
        if (gameOver || uci.length()<4) return false;
        int sc = uci.charAt(0) - 'a';
        int sr = 8 - (uci.charAt(1)-'0');
        int dc = uci.charAt(2) - 'a';
        int dr = 8 - (uci.charAt(3)-'0');
        if (!inBounds(sr,sc) || !inBounds(dr,dc)) return false;

        // Use the new legal move validation
        if (!isLegalMove(sr, sc, dr, dc)) return false;

        char p = board[sr][sc];
        char dest = board[dr][dc];
        boolean captureKing = (dest=='k' && whiteToMove) || (dest=='K' && !whiteToMove);

        // perform move
        board[dr][dc] = p;
        board[sr][sc] = '\0';

        // auto-promotion: pawn reaching last rank → queen
        if ((p=='P' && dr==0) || (p=='p' && dr==7)) {
            board[dr][dc] = (p=='P' ? 'Q' : 'q');
        }

        // --- Full-FEN bookkeeping (P0 #4) ---
        boolean isPawnMove = (p == 'P' || p == 'p');
        boolean isCapture  = (dest != '\0');

        // Halfmove clock: reset on pawn move or capture, else increment
        if (isPawnMove || isCapture) halfmoveClock = 0;
        else                         halfmoveClock++;

        // En passant target: set if a pawn just made a two-square move
        if (isPawnMove && Math.abs(dr - sr) == 2) {
            int epRow = (sr + dr) / 2;
            char file = (char) ('a' + sc);
            int rank = 8 - epRow;
            enPassantTarget = "" + file + rank;
        } else {
            enPassantTarget = "-";
        }

        // Castling rights: lost when king moves, when a rook leaves its home
        // square, or when a rook is captured on its home square.
        if (p == 'K') { whiteCanCastleKingside = false; whiteCanCastleQueenside = false; }
        else if (p == 'k') { blackCanCastleKingside = false; blackCanCastleQueenside = false; }
        else if (p == 'R') {
            if (sr == 7 && sc == 0) whiteCanCastleQueenside = false;
            if (sr == 7 && sc == 7) whiteCanCastleKingside  = false;
        } else if (p == 'r') {
            if (sr == 0 && sc == 0) blackCanCastleQueenside = false;
            if (sr == 0 && sc == 7) blackCanCastleKingside  = false;
        }
        if (dest == 'R') {
            if (dr == 7 && dc == 0) whiteCanCastleQueenside = false;
            if (dr == 7 && dc == 7) whiteCanCastleKingside  = false;
        } else if (dest == 'r') {
            if (dr == 0 && dc == 0) blackCanCastleQueenside = false;
            if (dr == 0 && dc == 7) blackCanCastleKingside  = false;
        }

        // Fullmove number: increments after Black's move (i.e. when Black was the one moving)
        if (!whiteToMove) fullmoveNumber++;
        // --- end FEN bookkeeping ---

        // increment move count
        if (whiteToMove) whiteMoveCount++;
        else            blackMoveCount++;

        // switch turn
        whiteToMove = !whiteToMove;

        // Check for game end conditions
        if (captureKing) {
            gameOver = true;
            boolean WhiteMove = !whiteToMove; // The player who just moved
            String Winner = WhiteMove ? "White" : "Black";
            this.result = Winner + " Wins!";
        } else if (isCheckmate()) {
            gameOver = true;
            boolean WhiteMove = !whiteToMove; // The player who just moved
            String Winner = WhiteMove ? "White" : "Black";
            this.result = Winner + " Wins by Checkmate!";
        } else if (isStalemate()) {
            gameOver = true;
            this.result = "Draw by Stalemate!";
        }
        
        return true;
    }
    // Took from here, https://chatgpt.com/share/681a4bec-04e8-8007-be04-f62341fee7f8
    private boolean isValidPieceMove(char p, int sr, int sc, int dr, int dc) {
        int drc = dr - sr, dcc = dc - sc;
        switch (Character.toLowerCase(p)) {
            case 'p': // pawn
                int dir = (p=='P' ? -1 : +1);
                // single step
                if (dcc==0 && drc==dir && board[dr][dc]=='\0') return true;
                // double step from home rank
                int home = (p=='P'?6:1);
                if (sr==home && dcc==0 && drc==2*dir
                  && board[sr+dir][sc]=='\0' && board[dr][dc]=='\0')
                    return true;
                // capture
                if (Math.abs(dcc)==1 && drc==dir && board[dr][dc]!='\0')
                    return true;
                return false;
            case 'r': // rook
                if (drc!=0 && dcc!=0) return false;
                return clearPath(sr,sc,dr,dc);
            case 'b': // bishop
                if (Math.abs(drc)!=Math.abs(dcc)) return false;
                return clearPath(sr,sc,dr,dc);
            case 'q': // queen
                if (drc == 0 && dcc == 0) return false;          // same square
                if (drc == 0 || dcc == 0)                         // rook-like
                    return clearPath(sr, sc, dr, dc);
                if (Math.abs(drc) == Math.abs(dcc))               // bishop-like
                    return clearPath(sr, sc, dr, dc);
                return false;
            case 'n': // knight
                return (Math.abs(drc)==2 && Math.abs(dcc)==1)
                    || (Math.abs(drc)==1 && Math.abs(dcc)==2);
            case 'k': // king (no castling)
                return Math.max(Math.abs(drc),Math.abs(dcc))==1;
            default:
                return false;
        }
    }

    public boolean isGameOver() {
        return gameOver || isCheckmate() || isStalemate();
    }
    
    // Check if the current player is in checkmate
    private boolean isCheckmate() {
        if (!isInCheck()) return false;
        return !hasLegalMoves();
    }
    
    // Check if the current player is in stalemate
    private boolean isStalemate() {
        if (isInCheck()) return false;
        return !hasLegalMoves();
    }
    
    // Check if the current player's king is in check
    private boolean isInCheck() {
        // Find the king position
        char kingChar = whiteToMove ? 'K' : 'k';
        int kingRow = -1, kingCol = -1;
        
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == kingChar) {
                    kingRow = r;
                    kingCol = c;
                    break;
                }
            }
            if (kingRow != -1) break;
        }
        
        if (kingRow == -1) return false; // King not found (shouldn't happen)
        
        // Check if any opponent piece can attack the king
        return isSquareUnderAttack(kingRow, kingCol, !whiteToMove);
    }
    
    // Check if a square is under attack by the specified color
    private boolean isSquareUnderAttack(int row, int col, boolean byWhite) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                char piece = board[r][c];
                if (piece == '\0') continue;
                
                boolean isPieceWhite = Character.isUpperCase(piece);
                if (isPieceWhite != byWhite) continue;
                
                // Check if this piece can attack the target square
                if (canPieceAttack(piece, r, c, row, col)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    // Check if a piece can attack a specific square (similar to move validation but for attacks)
    private boolean canPieceAttack(char piece, int fromRow, int fromCol, int toRow, int toCol) {
        int drc = toRow - fromRow, dcc = toCol - fromCol;
        
        switch (Character.toLowerCase(piece)) {
            case 'p': // pawn attacks diagonally
                int dir = (piece == 'P' ? -1 : +1);
                return Math.abs(dcc) == 1 && drc == dir;
            case 'r': // rook
                if (drc != 0 && dcc != 0) return false;
                return clearPath(fromRow, fromCol, toRow, toCol);
            case 'b': // bishop
                if (Math.abs(drc) != Math.abs(dcc)) return false;
                return clearPath(fromRow, fromCol, toRow, toCol);
            case 'q': // queen
                if (drc == 0 || dcc == 0 || Math.abs(drc) == Math.abs(dcc))
                    return clearPath(fromRow, fromCol, toRow, toCol);
                return false;
            case 'n': // knight
                return (Math.abs(drc) == 2 && Math.abs(dcc) == 1)
                    || (Math.abs(drc) == 1 && Math.abs(dcc) == 2);
            case 'k': // king
                return Math.max(Math.abs(drc), Math.abs(dcc)) == 1;
            default:
                return false;
        }
    }
    
    // Check if the current player has any legal moves
    private boolean hasLegalMoves() {
        for (int sr = 0; sr < SIZE; sr++) {
            for (int sc = 0; sc < SIZE; sc++) {
                char piece = board[sr][sc];
                if (piece == '\0') continue;
                
                boolean isPieceWhite = Character.isUpperCase(piece);
                if (isPieceWhite != whiteToMove) continue;
                
                // Try all possible destination squares
                for (int dr = 0; dr < SIZE; dr++) {
                    for (int dc = 0; dc < SIZE; dc++) {
                        if (sr == dr && sc == dc) continue;
                        
                        // Test if this move is legal
                        if (isLegalMove(sr, sc, dr, dc)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    // Check if a move is legal (doesn't leave king in check) - PUBLIC and SYNCHRONIZED for thread safety
    public synchronized boolean isLegalMove(int sr, int sc, int dr, int dc) {
        char piece = board[sr][sc];
        char dest = board[dr][dc];
        
        // Basic validation
        if (piece == '\0') return false;
        boolean isPieceWhite = Character.isUpperCase(piece);
        if (isPieceWhite != whiteToMove) return false;
        if (dest != '\0' && (Character.isUpperCase(dest) == isPieceWhite)) return false;
        if (!isValidPieceMove(piece, sr, sc, dr, dc)) return false;
        
        // Make the move temporarily
        board[dr][dc] = piece;
        board[sr][sc] = '\0';
        
        // Check if king is in check after this move
        boolean kingInCheck = isInCheck();
        
        // Undo the move
        board[sr][sc] = piece;
        board[dr][dc] = dest;
        
        return !kingInCheck;
    }
    
    public String getResult() {
        return result;
    }
    
    
    private boolean clearPath(int sr, int sc, int dr, int dc) {
        int stepR = Integer.signum(dr - sr);
        int stepC = Integer.signum(dc - sc);
        int r = sr + stepR, c = sc + stepC;
        while (r!=dr || c!=dc) {
            if (board[r][c] != '\0') return false;
            r += stepR; c += stepC;
        }
        return true;
    }

    private boolean inBounds(int r, int c) {
        return r>=0 && r<SIZE && c>=0 && c<SIZE;
    }
}
