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
   * The returned moves are tried one after another
   */
  def getMove(wasInvalid: Boolean): List[Move]

  def acceptMove(): Unit

  def destroy(): Unit
}

class HumanPlayer(val color: Color, gui: GameGUI) extends Player {
  def opponentMove(move: Move): Unit = ()

  def getMove(wasInvalid: Boolean): List[Move] = {
    if (!wasInvalid)
      Application.showMessage(s"${color} to move.")

    gui.getMove(color)
  }

  def acceptMove(): Unit = {
    gui.acceptMove()
  }

  def destroy(): Unit = {
    gui.abortMove()
  }

}