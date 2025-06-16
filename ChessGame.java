//Hashir Zahoor ur Rahman
//ChessNetworkGame

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;

//ChessGame class 
public class ChessGame extends Application {
    static final String START_FEN =
        "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w";

    //Declaring all private variables and constants
    private GameState game;
    private FENBoardRenderer renderer;
    private DataInputStream in;
    private DataOutputStream out;

    private boolean isWhite;             
    private Label   statusLabel = new Label("Status: starting…");
    private TextField moveInput = new TextField();
    private Button   sendBtn    = new Button("Send");
    private static final String state = "STATE ";
    private static final String moveFinal = "MOVE ";
    private static final String moveResult = "RESULT ";

    //The method contains GUI code for the game
    //Logic for starting the game and initial state of the game
    public void start(Stage stage) {
        //Alert Boxes & Buttons used, took from GUI programming class project
        moveInput.setPromptText("e.g. e2e4");
        moveInput.setDisable(true);
        sendBtn.setDisable(true);
        
        //Creating an instance of GridPane to create a chess board
        GridPane boardGrid = new GridPane();
        renderer = new FENBoardRenderer(boardGrid);

        //Alert boxes (Horizontal and Vertical boxes)
        HBox controls = new HBox(10, moveInput, sendBtn);
        controls.setPadding(new Insets(10));
        VBox root = new VBox(10, statusLabel, boardGrid, controls);
        root.setPadding(new Insets(10));

        stage.setScene(new Scene(root));
        stage.setTitle("Chess Network Game");
        stage.show();
        String serverHost = askString("Server IP/hostname:", "localhost");
        int    serverPort = askPort(  "Server port:",      8021);

        //  Networking on background thread
        new Thread(() -> {
            
            try {
                updateStatus("Connecting to " + serverHost + ":" + serverPort + "…");
                Socket socket = new Socket(serverHost, serverPort);
                updateStatus("Connected!");


                in  = new DataInputStream(socket.getInputStream());//Input DataStream
                out = new DataOutputStream(socket.getOutputStream());//Output DataStream
                String msg = in.readUTF();
                if (!msg.startsWith(state)) {
                    throw new IOException("Expected STATE, got: " + msg);
                }
                String fen = msg.substring(state.length());
                game = new GameState(fen);
                Platform.runLater(() -> renderer.setupBoard(fen));

                // 2) Read color/turn announcement
                String colorMsg = in.readUTF();
                isWhite = colorMsg.startsWith("You are White");
                Platform.runLater(() -> {
                    statusLabel.setText(colorMsg);
                    onTurnSwitched(colorMsg);
                });
                   
                listener(0);// Listener function to ensure that the game is running
                
            } catch (Exception ex) {
                ex.printStackTrace();
                updateStatus("Network error: " + ex.getMessage());
            }
        }).start();
        
        sendBtn.setOnAction(e -> {
            String uci = moveInput.getText().trim();// Takes the UCI move from user
            if (!game.applyMove(uci)) {//Confirms move validity from GameState Class
                alert("Illegal move");
            } else {
                renderer.setupBoard(game.getFen());//Redraws the updated Board once the move is confirmed to be valid.
                onTurnSwitched("You: " + uci);//Displays your move for reference
                
                if(game.isGameOver()) {//Confirms if the king has been captured from GameState class.
                    String Result = game.getResult();
                    sendMsg( moveResult + Result);
                    onGameOver("You Win!");
                } else{
                    sendMsg(moveFinal + uci);// Sends the move UCI to the peer
                }
            }
            moveInput.clear();
        });

    }
    //Ensures that the game turn status is updated on the window.
    private void renderInitial(String statusText) {
        Platform.runLater(() -> {
            renderer.setupBoard(game.getFen());
            onTurnSwitched(statusText);
        });
    } 
    //This is an important function as it ensures that the game is kept running until king capture.
    private void listener(int i) throws IOException {
        for (int j = i; i < 10; i++){
            String msg = in.readUTF();//This line ensures that it is continously reading the incoming UCI commands
            // from either SERVER or the CLIENT.
            if (msg.startsWith(moveFinal)){
            String NewMsg = msg.substring(5);
            Platform.runLater(() -> {
                game.applyMove(NewMsg);
                renderer.setupBoard(game.getFen());
                onTurnSwitched("Them: " + NewMsg);
            }); }  } 
        }
    //Ensures that the turns are changed accordingly.
    private void onTurnSwitched(String statusText) {
        statusLabel.setText(statusText);
        boolean whiteToMove = game.getTurn().equals("White");
        boolean myTurn = (isWhite && whiteToMove) || (!isWhite && !whiteToMove);

        moveInput.setDisable(!myTurn);
        sendBtn.setDisable(!myTurn);
    }
    //This function brings the alert for game end when the king is captured.
    private void onGameOver(String res) {
        Platform.runLater(() -> {
            Alert end = new Alert(Alert.AlertType.INFORMATION);
            end.setTitle("Game Over");
            end.setHeaderText(null);
            end.setContentText(res + "!");
            end.showAndWait();

            moveInput.setDisable(true);
            sendBtn.setDisable(true);
        });
    }
    //This function ensures that the new moves are communicated to the connection
    private void sendMsg(String txt) {
        try { out.writeUTF(txt); }
        catch (IOException e) { e.printStackTrace(); }
    }
    //Helper function that provides status updates, took from GUI programming class project
    private void updateStatus(String txt) {
        Platform.runLater(() -> statusLabel.setText(txt));
    }
    //Helps in obtaining Input from user,  took from GUI programming class project
    private String askString(String prompt, String defaultVal) {
        TextInputDialog dlg = new TextInputDialog(defaultVal);
        dlg.setHeaderText(prompt);
        Optional<String> r = dlg.showAndWait();
        if (r.isEmpty()) Platform.exit();
        return r.orElse(defaultVal);
    }
    //Helps in obtaining the port number from user,  took from GUI programming class project
    private int askPort(String prompt, int defaultPort) {
        TextInputDialog dlg = new TextInputDialog(String.valueOf(defaultPort));
        dlg.setHeaderText(prompt);
        Optional<String> r = dlg.showAndWait();
        if (r.isEmpty()) Platform.exit();
        try { return Integer.parseInt(r.get()); }
        catch (NumberFormatException e) { return defaultPort; }
    }
    //Creats Alert boxes,  took from GUI programming class project
    private void alert(String txt) {
        new Alert(Alert.AlertType.INFORMATION, txt, ButtonType.OK)
            .showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
