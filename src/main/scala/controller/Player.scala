package controller

import model._
import gui.GUI

abstract class Player(white: Boolean) {
  def getMove(chessLogic: ChessLogic): Move

  def playerString =
    if (white) "White"
    else "Black"
}

class AIPlayer(white: Boolean) extends Player(white) {
  def getMove(chessLogic: ChessLogic): Move = {
    Application.showMessage("AI is thinking...")
    chessLogic.suggestMove()
  }
}
class HumanPlayer(white: Boolean, gui: GUI) extends Player(white) {
  private def getMove(chessLogic: ChessLogic, message: String): Move = {
    Application.showMessage(message)
    
    val move = gui.getMove()
    if (chessLogic.isValidMove(move))
      return move
    
    getMove(chessLogic, "Invalid move! Try again, " + playerString + ".")
  }
  
  def getMove(chessLogic: ChessLogic): Move =
    getMove(chessLogic, "Make your move, " + playerString + "!")
}