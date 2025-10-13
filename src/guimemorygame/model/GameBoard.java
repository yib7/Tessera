package guimemorygame.model;

import java.util.Random;


public class GameBoard {
	private final GamePiece[][] board;
	private char[] alphabet;
	
	public GameBoard(int rows, int cols, Alphabet a) {
		this.alphabet = a.toCharArray();
		this.board = new CharacterGamePiece[rows][cols];
	}
	
	public void setUp(int row, int col) {
		/*
		 * Sets a character to each spot on the game board
		 * of the specified alphabet character
		 */
		Random r = new Random();
		int[] letter = new int[(row*col)/2];
		int[] rows = new int[row];
		int[] cols = new int[col];
		
		while(letter[letter.length - 1] == 0) {
			int random = r.nextInt(this.alphabet.length) + 1;
			for(int i = 0; i < letter.length; i ++) {
				if(letter[i] == random) {
					break;
				}
				else if(letter[i] == 0) {
					letter[i] = random;
					break;
				}
			}
		}
		
		for (int i = 0; i < letter.length; i++) {
			letter[i] = letter[i] - 1;
		}
		
		while(rows[rows.length - 1] == 0) {
			int random = r.nextInt(row) + 1;
			for(int i = 0; i < rows.length; i++) {
				if(rows[i] == random) {
					break;
				}
				else if(rows[i] == 0) {
					rows[i] = random;
					break;
				}
			}
		}
		
		for (int i = 0; i < rows.length; i++) {
			rows[i] = rows[i] - 1;
		}
		
		while(cols[cols.length - 1] == 0) {
			int random = r.nextInt(col) + 1;
			for(int i = 0; i < cols.length; i++) {
				if(cols[i] == random) {
					break;
				}
				else if(cols[i] == 0) {
					cols[i] = random;
					break;
				}
			}
		}
		
		for (int i = 0; i < cols.length; i++) {
			cols[i] = cols[i] - 1;
		}

		for(int x = 0; x < (row*col); x++) {
			for(int i = (x % board.length); i < (x % board.length) + 1; i++) {
				for(int j = (x % board[0].length); j < (x % board[0].length) + 1; j++) {
						GamePiece symbol = new CharacterGamePiece(alphabet[letter[x/2]]);
						board[rows[i]][cols[j]] = symbol;
				}
			}
		}
	}
		
	public void setInvisible() {
		/*
		 * Makes the entire game board invisible to the player so
		 * they can see a board of just question marks
		 */
		for(int i = 0; i < board.length; i++) {
			for(int j = 0; j < board[0].length; j++) {
				board[i][j].setVisible(false);	
			}
		}
	}
			
	public Character getPiece(int row, int col) {
		/*
		 * returns the symbol of a specific spot on the game board
		 */
		return this.board[row][col].getSymbol();
	}
	
	public void revealPiece(int row, int col) {
		/*
		 * reveals the symbol of a specific spot on the game board to the player
		 */
		this.board[row][col].setVisible(true);		
	}
	
	public void hidePiece(int row, int col) {
		/*
		 * hides the symbol of a specific spot on the game board from the player
		 */
		this.board[row][col].setVisible(false);;
	}
	
	public Boolean visiblePiece(int row, int col) {
		/*
		 * checks if the inputed location on the game board is already
		 * visible to the player to prevent any exploitations
		 */
		return this.board[row][col].isVisible();
	}
	
}