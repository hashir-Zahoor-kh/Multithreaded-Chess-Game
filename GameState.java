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
    private boolean gameOver = false;
    private String result = "";

    public GameState(String startFen) {
        String[] parts = startFen.split(" ");
        loadFromFen(parts[0]);
        whiteToMove = parts.length>1 && parts[1].equalsIgnoreCase("w");
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

        char p = board[sr][sc];
        if (p == '\0') return false;
        boolean isWhiteP = Character.isUpperCase(p);
        if (isWhiteP != whiteToMove) return false;

        // target occupant
        char dest = board[dr][dc];
        boolean captureKing = (dest=='k' && whiteToMove) || (dest=='K' && !whiteToMove);

        // must be empty or opponent
        if (dest!='\0' && (Character.isUpperCase(dest)==isWhiteP)) return false;

        // basic move-validation by piece type
        if (!isValidPieceMove(p, sr, sc, dr, dc)) return false;

        // perform move
        board[dr][dc] = p;
        board[sr][sc] = '\0';

        // auto-promotion: pawn reaching last rank → queen
        if ((p=='P' && dr==0) || (p=='p' && dr==7)) {
            board[dr][dc] = (p=='P' ? 'Q' : 'q');
        }

        // increment move count
        if (whiteToMove) whiteMoveCount++;
        else            blackMoveCount++;

        // switch turn
        whiteToMove = !whiteToMove;

        // if king was captured, end game
        if (captureKing) {
            gameOver = true;
            boolean WhiteMove = Character.isUpperCase(p);
            String Winner;
            if(WhiteMove){
                Winner = "White";
            } else {
                Winner = "Black";
            }
            this.result = Winner + "Wins!";
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
                if (drc==0 || dcc==0 || Math.abs(drc)==Math.abs(dcc))
                    return clearPath(sr,sc,dr,dc);
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
        return gameOver;
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
