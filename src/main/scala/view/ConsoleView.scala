package view

import scala.concurrent.Future

import controller.Action
import controller.BoardState
import controller.BoardSubscriber
import model.Black
import model.Color
import model.White

object ConsoleView extends BoardSubscriber {
  var counter = 1

  println(BoardState())

  def showMessage(message: String): Unit = {
    println(message)
  }

  def AIMove(color: Color) = ()

  def handleMoveString(move: String, color: Color): Unit = {
    color match {
      case White =>
        println(s"${counter}. ${move}")

      case Black =>
        println(s"${counter}... ${move}")
        counter = counter + 1
    }
  }

  def handleActions(actions: List[Action], board: BoardState): Future[Unit] = {
    println(board)
    Future.successful(())
  }
}