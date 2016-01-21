package controller.logic

import model.Move

trait ChessLogic {
  def attemptMove(move: Move): Boolean

  def destroy(): Unit
}