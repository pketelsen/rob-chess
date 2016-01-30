package view

import controller.logic.ResultDraw
import controller.logic.ResultWhiteWins
import model.White
import controller.GameEvent
import controller.logic.ResultBlackWins
import scala.concurrent.Future
import model.Color

object ConsoleView extends BoardView {
  var turn: Color = White

  override def handle(event: GameEvent): Future[Unit] = {
    println(s"${turn} made move ${event.move}")

    turn = turn.other

    super.handle(event)
  }

  def showMessage(message: String): Unit = {
    println(message)
  }

  def AIMove(color: Color) = ()

  def handleActions(l: List[Action]): Future[Unit] = {
    println(board)
    Future.successful(())
  }
}