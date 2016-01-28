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

    event.result match {
      case Some(ResultWhiteWins(message)) => println(s"White wins: $message")
      case Some(ResultBlackWins(message)) => println(s"Black wins: $message")
      case Some(ResultDraw(message)) => println(s"Draw: $message")
      case _ =>
    }

    turn = turn.other

    super.handle(event)
  }

  def handleActions(l: List[Action]): Future[Unit] = {
    println(board)
    Future.successful(())
  }
}