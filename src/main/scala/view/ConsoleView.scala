package view

import scala.concurrent.Future

import controller.Action
import controller.BoardState
import controller.BoardSubscriber
import model.Color

object ConsoleView extends BoardSubscriber {
  def showMessage(message: String): Unit = {
    println(message)
  }

  def AIMove(color: Color) = ()

  def resetBoard(board: BoardState): Unit = {
    println(board)
  }

  def handleActions(actions: List[Action], board: BoardState): Future[Unit] = {
    println(board)
    Future.successful(())
  }
}