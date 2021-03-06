package com.github.jubalh.jessy;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import com.fluxchess.jcpi.models.GenericBoard;
import com.fluxchess.jcpi.models.GenericMove;
import com.fluxchess.jcpi.models.GenericRank;
import com.fluxchess.jcpi.models.GenericChessman;
import com.fluxchess.jcpi.utils.MoveGenerator;
import com.fluxchess.flux.board.Hex88Board;
import com.fluxchess.flux.move.IntMove;
import com.github.jubalh.jessy.pieces.*;

public class Game extends Observable {

	private EngineHandler engineHandler = null;
	private final List<GenericMove> moves = new ArrayList<GenericMove>();
	private int castlingInt = 0;
	private Board board;
	private boolean running;
	private boolean moveWasValid;
	private boolean isComputerGame;
	private Color currentPlayer;

	/**
	 * Constructor
	 * @param board to use
	 */
	public Game(Board board) {
		this.board = board;
	}

	public void init() {
		board.reset();
		board.init();

		moves.clear();
		castlingInt = getHexBoard().castling;

		engineHandler = new EngineHandler();
		engineHandler.start();
		engineHandler.newGame();

		this.running = false;
		this.moveWasValid = false;
		this.isComputerGame = false;
		this.currentPlayer = Color.WHITE;
	}

	/**
	 * Gets game status.
	 * @return true if runs
	 */
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Sets game status
	 * @param status
	 */
	public void setRunning(boolean status) {
		this.running = status;
		if(status == false && engineHandler != null) {
			engineHandler.stop();//TODO: rather in destructor?
		}
	}

	/**
	 * Get if last move was set to be okay.
	 * @return true if was okay
	 */
	public boolean wasValidMove() {
		return this.moveWasValid;
	}

	/**
	 * TODO: needed? take a look at isValid()...
	 * Set if last move was okay
	 * @param status
	 */
	public void setValidMove(boolean status) {
		this.moveWasValid = status;
	}

	/**
	 * Get Color of player whose turn it is
	 * @return Color of current player
	 */
	public Color getCurrentPlayer() {
		return currentPlayer;
	}

	/**
	 * Define whose turn it is
	 * @param currentPlayer color of player whose turn it is
	 */
	public void setCurrentPlayer(Color currentPlayer) {
		this.currentPlayer = currentPlayer;
	}

	/**
	 * Next players turn.
	 */
	public void nextPlayer() {
		if(this.currentPlayer == Color.WHITE)
			this.currentPlayer = Color.BLACK;
		else
			this.currentPlayer = Color.WHITE;
	}

	/**
	 * Returns if game is played against computer
	 * @return true if against computer
	 */
	public boolean isComputerGame() {
		return isComputerGame;
	}

	/**
	 * Sets if game is played against computer
	 * @return true if against computer
	 */
	public void isComputerGame(boolean status) {
		this.isComputerGame = status;
	}

	/**
	 * Returns board
	 * @return board
	 */
	public Board getBoard() {
		return board;
	}

	public void statusUpdate(String message) {
		GameNotification notification = new GameNotification(message);
		setChanged();
		notifyObservers(notification);
	}

	public void process(GenericMove move) {

		try {
			Figure figureToMove = board.getFigure(move.from);
			if (figureToMove == null) {
				statusUpdate("Wrong coordinates");
				return;
			} else {
				if (!figureToMove.isOpponent(this.getCurrentPlayer()) ) {
					GenericChessman promotion = null;
					if ( figureToMove instanceof Pawn && (move.to.rank == GenericRank.R8 || move.to.rank == GenericRank.R1) )
						promotion = GenericChessman.QUEEN;
					GenericMove genMove = new GenericMove( move.from, move.to, promotion);
					this.setValidMove(this.isValidMove(genMove));
					if(this.wasValidMove()) {
						/*TODO: Find a decent way to ask for the promotion piece
						 * right now it defaults to a queen
						 */
						// System.out.println("What piece would you like to promote to?");
						board.moveFigure(move);
						if (genMove.promotion != null) {
							board.setFigure(move.to, new Queen(getCurrentPlayer()));
						}
						this.makeMove(genMove);
						if (this.isMate()) {
							statusUpdate("Checkmate!\n");
							return;
						} else {
							if ( this.isCastle() ) {
								board.moveCastlingRook();
							}

							this.nextPlayer();
							if (this.isComputerGame()) {
								/*TODO: this should be done in a gameloop.
								 * for sure after drawing the board so the user sees his last move first.
								 * best would be in another thread so jessy doesnt freeze. 
								 */
								engineHandler.compute(this, board);
							}
						}
					} else {
						statusUpdate("Move not allowed\n");
						return;
					}
				} else {
					this.setValidMove(false);
						statusUpdate("It's not your turn\n");
						return;
				}
			}
		} catch (NotAField e) {
			// should not occur, since it gets already checked in parseFigurePos
			System.err.println("Illegal field");
			e.printStackTrace();
		}
	}

	private Hex88Board getHexBoard() {
		Hex88Board hex88Board = new Hex88Board(new GenericBoard(GenericBoard.STANDARDSETUP));
		for (GenericMove genericMove : moves) {
			int move = IntMove.convertMove(genericMove, hex88Board);
			hex88Board.makeMove(move);
		}
		return hex88Board;
	}

	private GenericBoard getCurrentBoard() {
		Hex88Board hex88Board = getHexBoard();

		return hex88Board.getBoard();
	}

	private boolean isValid(GenericBoard board, GenericMove move) {
		for (GenericMove validMove : MoveGenerator.getGenericMoves(board)) {
			if (move.equals(validMove)) {
				return true;
			}
		}

		return false;
	}

	public boolean isValidMove(GenericMove move) {
		return isValid(getCurrentBoard(), move);
	}

	public void makeMove(GenericMove move) {
		if (isValidMove(move)) {
			moves.add(move);
		} else {
			throw new IllegalArgumentException();
		}
	}

	public void undoMove() {
		moves.remove(moves.size() - 1);
	}

	public boolean isMate() {
		return MoveGenerator.getGenericMoves(getCurrentBoard()).length == 0;
	}

	public boolean isCastle() {
		int newCastling = getHexBoard().castling;
		if (getHexBoard().castling != castlingInt) {
			castlingInt =  newCastling;
			return true;
		}

		return false;
	}
	
	public List<GenericMove> getMoves() {
		return this.moves;
	}
	
}
