import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.LinkedList;

public class ChessGameSession extends Thread {
    private static final String STATE   = "STATE ";
    private static final String MOVE    = "MOVE ";
    private static final String RESULT  = "RESULT ";

    private final Socket whiteSock;
    private final Socket blackSock;


    public ChessGameSession(Socket whiteSock, Socket blackSock) {
        this.whiteSock = whiteSock;
        this.blackSock = blackSock;
    }

    public void run() {
        try (
            DataInputStream  inWhite  = new DataInputStream( whiteSock.getInputStream());
            DataOutputStream outWhite = new DataOutputStream(whiteSock.getOutputStream());
            DataInputStream  inBlack  = new DataInputStream( blackSock.getInputStream());
            DataOutputStream outBlack = new DataOutputStream(blackSock.getOutputStream());
        ) {
            //Initialize game state
            GameState game = new GameState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w");

            //Send initial FEN to both players
            String initFen = game.getFen();
            outWhite.writeUTF(STATE + initFen);
            outBlack.writeUTF(STATE + initFen);
            //Sends status of moves so that the client may understand.
            outWhite.writeUTF("You are White. Your move.");
            outBlack.writeUTF("You are Black. Waiting…");

            //Main game loop
            boolean whiteToMove = true;
            while (!game.isGameOver()) {
                DataInputStream  in  = whiteToMove ? inWhite  : inBlack;
                DataOutputStream out = whiteToMove ? outWhite : outBlack;
                DataOutputStream opp = whiteToMove ? outBlack : outWhite;

                //Read their move
                String msg = in.readUTF();
                if (!msg.startsWith(MOVE)) {
                    out.writeUTF(RESULT + "ERROR: expected MOVE");
                    continue;
                }
                String uci = msg.substring(MOVE.length());

                //Try to apply
                boolean ok = game.applyMove(uci);
                if (!ok) {
                    out.writeUTF(RESULT + "ILLEGAL");
                    continue;
                }

                //show move & new FEN
                String newFen = game.getFen();
                out.writeUTF(RESULT + "OK");
                opp.writeUTF(MOVE + uci);
                opp.writeUTF(STATE + newFen);

                //Swap turns
                whiteToMove = !whiteToMove;
            }

            //Game over
            String finalResult = game.getResult();
            outWhite.writeUTF(RESULT + finalResult);
            outBlack.writeUTF(RESULT + finalResult);

            System.out.println("Game finished: " + finalResult);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { whiteSock.close(); } catch(Exception ignored){}
            try { blackSock.close(); } catch(Exception ignored){}
        }
    }
}
