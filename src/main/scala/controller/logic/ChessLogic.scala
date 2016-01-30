package controller.logic

import model.Color
import model.Move

abstract sealed class Result {
  def message: String
}

case class ResultWin(color: Color, message: String) extends Result
case class ResultDraw(message: String) extends Result

trait ChessLogic {
  def attemptMove(move: Move): Boolean

  def getResult: Option[Result]

  def destroy(): Unit
}