//Hashir Zahoor ur Rahman
//ChessNetworkGame

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

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
    private boolean gameOverShown = false; // Flag to prevent multiple popups
    private String dragFromSquare = null; // Track drag source for drag-and-drop
    private int dragFromCol = -1;
    private int dragFromRow = -1;
    private static final String state = "STATE ";
    private static final String moveFinal = "MOVE ";
    private static final String moveResult = "RESULT ";

    //The method contains GUI code for the game
    //Logic for starting the game and initial state of the game
    public void start(Stage stage) {
        //Creating an instance of GridPane to create a chess board
        GridPane boardGrid = new GridPane();
        renderer = new FENBoardRenderer(boardGrid);
        
        // Set up drag-and-drop press handler
        renderer.setOnSquarePressed((col, row) -> {
            renderer.clearLegalHighlights();           // Clear old highlights first
            renderer.highlightSquare(dragFromCol, dragFromRow, false); // Clear old highlight
            
            // Only allow dragging on player's turn
            boolean whiteToMove = game != null && game.getTurn().equals("White");
            boolean myTurn = (isWhite && whiteToMove) || (!isWhite && !whiteToMove);
            
            if (!myTurn || game == null) return;
            
            dragFromSquare = toUCI(col, row);
            dragFromCol = col;
            dragFromRow = row;
            renderer.highlightSquare(col, row, true);
            
            // Compute and highlight legal moves for this piece
            List<String> legalMoves = new ArrayList<>();
            int sr = 8 - (dragFromSquare.charAt(1) - '0');
            int sc = dragFromSquare.charAt(0) - 'a';
            
            for (char f = 'a'; f <= 'h'; f++) {
                for (int r = 1; r <= 8; r++) {
                    int dr = 8 - r;
                    int dc = f - 'a';
                    
                    // Check if this move is legal using GameState's validation
                    if (game.isLegalMove(sr, sc, dr, dc)) {
                        legalMoves.add("" + f + r);
                    }
                }
            }
            
            renderer.highlightLegalMoves(legalMoves);
        });

        //Layout: Status label and board only (no text input)
        VBox root = new VBox(10, statusLabel, boardGrid);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Chess Network Game");
        stage.show();
        
        // CRITICAL FIX: Use scene-level mouse release to detect drop target
        scene.setOnMouseReleased(e -> {
            // Clear all highlights
            renderer.clearLegalHighlights();
            renderer.highlightSquare(dragFromCol, dragFromRow, false);
            
            if (dragFromSquare == null) return;
            
            // Find which square the mouse is over using scene coordinates
            for (Node node : boardGrid.getChildren()) {
                Bounds bounds = node.localToScene(node.getBoundsInLocal());
                if (bounds.contains(e.getSceneX(), e.getSceneY())) {
                    Integer col = GridPane.getColumnIndex(node);
                    Integer row = GridPane.getRowIndex(node);
                    
                    if (col != null && row != null) {
                        String dragToSquare = toUCI(col, row);
                        
                        if (!dragFromSquare.equals(dragToSquare)) {
                            String move = dragFromSquare + dragToSquare;
                            
                            // Check for pawn promotion
                            if (isPawnPromotion(dragFromSquare, dragToSquare)) {
                                String piece = showPromotionDialog();
                                if (piece != null) {
                                    move = move + piece;
                                }
                            }
                            
                            submitMove(move);
                        }
                        break;
                    }
                }
            }
            
            dragFromSquare = null;
            dragFromCol = -1;
            dragFromRow = -1;
        });
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
        while (!game.gameOver) { // Use volatile flag instead of isGameOver() to avoid race condition
            String msg = in.readUTF();//This line ensures that it is continously reading the incoming UCI commands
            // from either SERVER or the CLIENT.
            if (msg.startsWith(moveFinal)){
                String NewMsg = msg.substring(5);
                Platform.runLater(() -> {
                    game.applyMove(NewMsg);
                    renderer.setupBoard(game.getFen());
                    onTurnSwitched("Them: " + NewMsg);
                    
                    // Check if game is over after opponent's move
                    if (game.isGameOver() && !gameOverShown) {
                        String result = game.getResult();
                        onGameOver("You Lost! " + result);
                    }
                });
            } else if (msg.startsWith(moveResult)) {
                // Handle game over messages from server or move confirmations
                String result = msg.substring(moveResult.length());
                
                if (result.equals("OK")) {
                    // Server confirmed our move - update our board
                    Platform.runLater(() -> {
                        renderer.setupBoard(game.getFen());
                        onTurnSwitched("You: " + "move confirmed");
                    });
                } else if (result.contains("Wins") || result.contains("Draw")) {
                    Platform.runLater(() -> {
                        if (!gameOverShown) {
                            onGameOver(result);
                        }
                    });
                    break;
                }
            }
        }
    }
    //Ensures that the turns are changed accordingly.
    private void onTurnSwitched(String statusText) {
        statusLabel.setText(statusText);
        // Turn management now handled entirely by drag-and-drop validation
    }
    //This function brings the alert for game end when the king is captured.
    private void onGameOver(String res) {
        if (gameOverShown) return; // Prevent multiple popups
        gameOverShown = true;
        
        Platform.runLater(() -> {
            Alert end = new Alert(Alert.AlertType.INFORMATION);
            end.setTitle("Game Over");
            end.setHeaderText(null);
            end.setContentText(res);
            end.showAndWait();
            // No need to disable UI elements - drag-and-drop is already disabled by turn validation
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
    
    // Convert board coordinates to UCI notation
    private String toUCI(int col, int row) {
        char file = (char)('a' + col);
        int rank = 8 - row; // row 0 = rank 8 (black's side)
        return "" + file + rank;
    }
    
    // Check if move is a pawn promotion
    private boolean isPawnPromotion(String from, String to) {
        if (game == null) return false;
        
        // Extract coordinates
        int fromCol = from.charAt(0) - 'a';
        int fromRow = 8 - (from.charAt(1) - '0');
        int toRow = 8 - (to.charAt(1) - '0');
        
        // Check if it's a pawn moving to the back rank
        String fen = game.getFen();
        String[] rows = fen.split(" ")[0].split("/");
        
        if (fromRow >= 0 && fromRow < 8) {
            int c = 0;
            for (char ch : rows[fromRow].toCharArray()) {
                if (Character.isDigit(ch)) {
                    c += Character.getNumericValue(ch);
                } else {
                    if (c == fromCol) {
                        // Check if it's a pawn and moving to last rank
                        if ((ch == 'P' && toRow == 0) || (ch == 'p' && toRow == 7)) {
                            return true;
                        }
                        break;
                    }
                    c++;
                }
            }
        }
        return false;
    }
    
    // Show dialog for pawn promotion piece selection
    private String showPromotionDialog() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Queen", "Queen", "Rook", "Bishop", "Knight");
        dialog.setTitle("Pawn Promotion");
        dialog.setHeaderText("Choose promotion piece:");
        dialog.setContentText("Piece:");
        
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            return switch (result.get()) {
                case "Queen" -> "q";
                case "Rook" -> "r";
                case "Bishop" -> "b";
                case "Knight" -> "n";
                default -> "q";
            };
        }
        return "q"; // Default to queen
    }
    
    // Submit a move (used by drag-and-drop)
    private void submitMove(String uci) {
        // Guard: Only allow moves on player's turn
        boolean whiteToMove = game.getTurn().equals("White");
        boolean myTurn = (isWhite && whiteToMove) || (!isWhite && !whiteToMove);
        
        if (!myTurn) {
            return; // Silently ignore moves on opponent's turn
        }
        
        if (!game.applyMove(uci)) {
            Platform.runLater(() -> alert("Illegal move"));
            return;
        }
        
        // Don't update local board here - let server response handle it
        // This prevents desynchronization between clients
        sendMsg(moveFinal + uci);
        
        if(game.isGameOver()) {
            String Result = game.getResult();
            sendMsg(moveResult + Result);
            if (!gameOverShown) {
                onGameOver("You Win! " + Result);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
