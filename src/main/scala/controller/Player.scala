package controller

import model.Color
import model.Move
import view.gui.GameGUI

abstract sealed class PlayerType

case object PlayerTypeHuman extends PlayerType {
  override def toString = "Human"
}

case object PlayerTypeAI extends PlayerType {
  override def toString = "AI"
}

abstract trait Player {
  def color: Color

  def opponentMove(move: Move): Unit

  /**
   * Gets a move from the player
   *
   * Returns None when the game is aborted
   */
  def getMove(wasInvalid: Boolean): Move

  def destroy(): Unit
}

class HumanPlayer(val color: Color, gui: GameGUI) extends Player {
  def opponentMove(move: Move): Unit = ()
  def getMove(wasInvalid: Boolean): Move = {
    if (wasInvalid)
      Application.showMessage(s"Invalid move! Try again, ${color}.")
    else
      Application.showMessage(s"${color} to move.")

    gui.getMove(color)
  }

  def destroy(): Unit = {
    gui.abortMove()
  }

}