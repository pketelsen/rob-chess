package controller

import model._
import gui.GUI

abstract trait Player {
  def white: Boolean

  def opponentMove(move: Move): Unit

  def getMove(wasInvalid: Boolean): Move

  def playerString =
    if (white)
      "White"
    else
      "Black"
}

class HumanPlayer(val white: Boolean, gui: GUI) extends Player {
  def opponentMove(move: Move): Unit = ()

  def getMove(wasInvalid: Boolean): Move = {
    if (wasInvalid)
      Application.showMessage(s"Invalid move! Try again, $playerString.")
    else
      Application.showMessage(s"Make your move, $playerString.")

    gui.getMove()
  }
}