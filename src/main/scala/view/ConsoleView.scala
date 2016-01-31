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

  def reset(): Future[Unit] = {
    counter = 1

    println(BoardState())

    Future.successful(())
  }

  def showMessage(message: String): Unit = {
    println(message)
  }

  def AIMove(color: Color) = ()

  def handleMove(actions: List[Action], color: Color, move: String, board: BoardState): Future[Unit] = {
    color match {
      case White =>
        println(s"${counter}. ${move}")

      case Black =>
        println(s"${counter}... ${move}")
        counter = counter + 1
    }

    println(board)
    Future.successful(())
  }
}