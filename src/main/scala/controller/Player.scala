package controller

import model._
import gui.GameGUI


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

  def getMove(wasInvalid: Boolean): Move

  def destroy(): Unit
}

class HumanPlayer(val color: Color, gui: GameGUI) extends Player {
  def opponentMove(move: Move): Unit = ()
  def destroy(): Unit = ()

  def getMove(wasInvalid: Boolean): Move = {
    if (wasInvalid)
      Application.showMessage(s"Invalid move! Try again, ${color}.")
    else
      Application.showMessage(s"Make your move, ${color}.")

    gui.getMove()
  }
}