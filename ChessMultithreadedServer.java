import java.net.ServerSocket;
import java.net.Socket;

public class ChessMultithreadedServer {
    public static final int DEFAULT_PORT = 8021;

    public static void main(String[] args) throws Exception {
        int port = (args.length > 0)
          ? Integer.parseInt(args[0])
          : DEFAULT_PORT;
          //https://docs.oracle.com/javase/tutorial/java/nutsandbolts/op2.html
          
        try (ServerSocket listener = new ServerSocket(port)) {
            System.out.println("Chess server listening on port " + port);
            while (true) {
                //Wait for White
                System.out.println("Waiting for White to connect…");
                Socket whiteSocket = listener.accept();
                System.out.println("White connected from " 
                                   + whiteSocket.getRemoteSocketAddress());

                //Wait for Black
                System.out.println("Waiting for Black to connect…");
                Socket blackSocket = listener.accept();
                System.out.println("Black connected from " 
                                   + blackSocket.getRemoteSocketAddress());

                //a new thread
                new ChessGameSession(whiteSocket, blackSocket)
                    .start();
            }
        }
    }
}
