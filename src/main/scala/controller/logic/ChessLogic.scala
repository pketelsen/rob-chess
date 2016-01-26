package controller.logic

import model.Move

abstract sealed class Result {
  def message: String
}

case class ResultWhiteWins(message: String) extends Result
case class ResultBlackWins(message: String) extends Result
case class ResultDraw(message: String) extends Result

trait ChessLogic {
  def attemptMove(move: Move): Boolean

  def getResult: Option[Result]

  def destroy(): Unit
}