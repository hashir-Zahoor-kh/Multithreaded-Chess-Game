//This whole java class is taken from my GUI prorgamming final project 

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class FENBoardRenderer {
    private static final int SIZE = 8, SQUARE = 50;
    private final GridPane grid;
    private final Image WP, WR, WN, WB, WQ, WK;
    private final Image BP, BR, BN, BB, BQ, BK;

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
        Color dark = Color.rgb(82, 52, 26);
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                Rectangle sq = new Rectangle(SQUARE, SQUARE);
                sq.setFill((r + c) % 2 == 0 ? Color.WHITE : dark);
                grid.add(sq, c, r);
            }
        }
    }

    public void setupBoard(String fen) {
        drawEmpty();
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
                        grid.add(iv, c, r);
                    }
                    c++;
                }
            }
        }
    }
}
