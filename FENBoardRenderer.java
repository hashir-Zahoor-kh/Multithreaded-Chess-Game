//This whole java class is taken from my GUI prorgamming final project 

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import javafx.scene.input.MouseEvent;
import java.util.function.BiConsumer;
import java.util.List;
import java.util.ArrayList;

public class FENBoardRenderer {
    private static final int SIZE = 8, SQUARE = 50;
    private final GridPane grid;
    private final Image WP, WR, WN, WB, WQ, WK;
    private final Image BP, BR, BN, BB, BQ, BK;
    private final StackPane[][] squares = new StackPane[SIZE][SIZE];
    private BiConsumer<Integer, Integer> onSquarePressed;
    private BiConsumer<Integer, Integer> onSquareReleased;
    private List<Circle> dotOverlays = new ArrayList<>();
    private Color darkSquareColor = Color.rgb(82, 52, 26);
    private Color lightSquareColor = Color.WHITE;

    public FENBoardRenderer(GridPane grid) {
        this.grid = grid;
        WP = new Image("file:White_Pawn.png");
        WR = new Image("file:White_Rook.png");
        WN = new Image("file:White_Knight.png");
        WB = new Image("file:White_Bishop.png");
        WQ = new Image("file:White_Queen.png");
        WK = new Image("file:White_King.png");
        BP = new Image("file:Black_Pawn.png");
        BR = new Image("file:Black_Rook.png");
        BN = new Image("file:Black_Knight.png");
        BB = new Image("file:Black_Bishop.png");
        BQ = new Image("file:Black_Queen.png");
        BK = new Image("file:Black_King.png");
        drawEmpty();
    }

    private void drawEmpty() {
        grid.getChildren().clear();
        
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                Rectangle sq = new Rectangle(SQUARE, SQUARE);
                sq.setFill((r + c) % 2 == 0 ? lightSquareColor : darkSquareColor);
                
                // Create StackPane for each square to hold rectangle and piece
                StackPane stackPane = new StackPane();
                stackPane.getChildren().add(sq);
                squares[r][c] = stackPane;
                
                // Add mouse event handlers for drag and drop
                final int row = r;
                final int col = c;
                
                stackPane.setOnMousePressed(e -> {
                    if (onSquarePressed != null) {
                        onSquarePressed.accept(col, row);
                        stackPane.startFullDrag(); // CRITICAL: Enable drag events across nodes
                    }
                });
                
                grid.add(stackPane, c, r);
            }
        }
    }

    public void setupBoard(String fen) {
        // Clear pieces from all squares but keep the squares themselves
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                StackPane square = squares[r][c];
                // Remove all children except the rectangle (first child)
                if (square.getChildren().size() > 1) {
                    square.getChildren().remove(1, square.getChildren().size());
                }
            }
        }
        
        String[] rows = fen.split(" ")[0].split("/");
        for (int r = 0; r < SIZE; r++) {
            int c = 0;
            for (char ch : rows[r].toCharArray()) {
                if (Character.isDigit(ch)) {
                    c += Character.getNumericValue(ch);
                } else {
                    Image img = switch (ch) {
                        case 'P' -> WP; case 'R' -> WR; case 'N' -> WN;
                        case 'B' -> WB; case 'Q' -> WQ; case 'K' -> WK;
                        case 'p' -> BP; case 'r' -> BR; case 'n' -> BN;
                        case 'b' -> BB; case 'q' -> BQ; case 'k' -> BK;
                        default  -> null;
                    };
                    if (img != null) {
                        ImageView iv = new ImageView(img);
                        iv.setFitWidth(SQUARE);
                        iv.setFitHeight(SQUARE);
                        iv.setMouseTransparent(true); // Allow clicks to pass through to StackPane
                        squares[r][c].getChildren().add(iv);
                    }
                    c++;
                }
            }
        }
    }
    
    // Set handlers for drag and drop
    public void setOnSquarePressed(BiConsumer<Integer, Integer> handler) {
        this.onSquarePressed = handler;
    }
    
    public void setOnSquareReleased(BiConsumer<Integer, Integer> handler) {
        this.onSquareReleased = handler;
    }
    
    // Get the GridPane for scene-level event handling
    public GridPane getGrid() {
        return grid;
    }
    
    // Highlight a square (for visual feedback during drag)
    public void highlightSquare(int col, int row, boolean highlight) {
        if (row >= 0 && row < SIZE && col >= 0 && col < SIZE) {
            Rectangle rect = (Rectangle) squares[row][col].getChildren().get(0);
            if (highlight) {
                rect.setStroke(Color.YELLOW);
                rect.setStrokeWidth(3);
            } else {
                rect.setStroke(null);
            }
        }
    }
    
    // Highlight legal destination squares
    public void highlightLegalMoves(List<String> destinations) {
        clearLegalHighlights();
        
        for (String sq : destinations) {
            int col = sq.charAt(0) - 'a';
            int row = 8 - Character.getNumericValue(sq.charAt(1));
            
            if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) continue;
            
            StackPane square = squares[row][col];
            if (square == null) continue;
            
            boolean occupied = square.getChildren().size() > 1; // has a piece image
            
            if (occupied) {
                // Ring around capturable piece
                Rectangle rect = (Rectangle) square.getChildren().get(0);
                rect.setStroke(Color.color(0, 0, 0, 0.4));
                rect.setStrokeWidth(5);
            } else {
                // Dot on empty square
                Circle dot = new Circle(8, Color.color(0, 0, 0, 0.25));
                dot.setMouseTransparent(true); // don't block mouse events
                square.getChildren().add(dot);
                dotOverlays.add(dot);
            }
        }
    }
    
    // Clear all legal move highlights
    public void clearLegalHighlights() {
        // Remove dots
        for (Circle dot : dotOverlays) {
            StackPane parent = (StackPane) dot.getParent();
            if (parent != null) {
                parent.getChildren().remove(dot);
            }
        }
        dotOverlays.clear();
        
        // Clear border strokes on all squares
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                StackPane square = squares[r][c];
                if (square != null && square.getChildren().size() > 0) {
                    Rectangle rect = (Rectangle) square.getChildren().get(0);
                    rect.setStroke(null);
                }
            }
        }
    }
}
