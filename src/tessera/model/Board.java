package tessera.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The grid of tiles. The board owns the deal: it takes one face per pair from
 * the chosen theme, duplicates each face, shuffles the whole stack, and lays it
 * out row by row. Every face appears exactly twice and the layout is uniformly
 * random, which the old letter-and-coordinate shuffle did not guarantee.
 *
 * <p>The deal is deterministic when constructed with a seeded {@link Random},
 * which is what the headless logic tests rely on.
 */
public final class Board {

    private final int rows;
    private final int cols;
    private final Tile[][] grid;

    public Board(BoardSize size, TileTheme theme) {
        this(size, theme, new Random());
    }

    public Board(BoardSize size, TileTheme theme, Random random) {
        this.rows = size.rows();
        this.cols = size.cols();
        this.grid = new Tile[rows][cols];
        deal(size, theme, random);
    }

    private void deal(BoardSize size, TileTheme theme, Random random) {
        int pairs = size.pairCount();
        List<String> faces = theme.faces(pairs);

        List<Tile> stack = new ArrayList<>(size.cellCount());
        for (String face : faces) {
            stack.add(new Tile(face));
            stack.add(new Tile(face));
        }
        Collections.shuffle(stack, random);

        int index = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = stack.get(index++);
            }
        }
    }

    public int rows() {
        return rows;
    }

    public int cols() {
        return cols;
    }

    public Tile tileAt(int row, int col) {
        return grid[row][col];
    }

    public int totalPairs() {
        return (rows * cols) / 2;
    }

    /** True once every tile has been matched. */
    public boolean isSolved() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!grid[r][c].isMatched()) {
                    return false;
                }
            }
        }
        return true;
    }
}
