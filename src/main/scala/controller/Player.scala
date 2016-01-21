package controller

import model._
import gui.GUI

abstract class Player(white: Boolean) {
  def getMove(wasInvalid: Boolean): Move

  def playerString =
    if (white) "White"
    else "Black"
}

class AIPlayer(white: Boolean) extends Player(white) {
  def getMove(wasInvalid: Boolean): Move = {
    Application.showMessage("AI is thinking...")
    ???
  }
}
class HumanPlayer(white: Boolean, gui: GUI) extends Player(white) {
  def getMove(wasInvalid: Boolean): Move = {
    if (wasInvalid)
      Application.showMessage(s"Invalid move! Try again, $playerString.")
    else
      Application.showMessage(s"Make your move, $playerString.")

    gui.getMove()
  }
}