package controller

import java.util.concurrent.Executors
import scala.annotation.tailrec
import scala.collection.mutable.MutableList
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import logic._
import model.Move
import model.White
import model.Color
import model.Black

case class GameEvent(move: Move, result: Option[Result])

trait GameSubscriber {
  def handle(event: GameEvent): Future[Unit]
}

class Game(white: Player, black: Player) {
  private val subscribers = MutableList[GameSubscriber]()

  private var turn: Color = White

  private val logic: ChessLogic = new CECPLogic

  private val executionContext = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

  private def publishAndWait(move: Move, result: Option[Result]) = {
    val futures = subscribers.map(_.handle(GameEvent(move, result)))

    futures.foreach { Await.ready(_, Duration.Inf) }
  }

  def subscribe(sub: GameSubscriber) = subscribers += sub

  @tailrec
  private def makeTurn(player: Player, wasInvalid: Boolean = false): Move = {
    val move = player.getMove(wasInvalid)

    if (logic.attemptMove(move))
      move
    else
      makeTurn(player, true)
  }

  def run(): Unit = Future {
    val (player, opponent) = turn match {
      case White =>
        (white, black)
      case Black =>
        (black, white)
    }

    val move = makeTurn(player)

    logic.getResult match {
      case None =>
        opponent.opponentMove(move)
        publishAndWait(move, None)

        turn = turn.other
        Application.queueEvent(NextTurnEvent)

      case Some(result) =>
        publishAndWait(move, Some(result))
        Application.queueEvent(EndGameEvent(result))
    }
  }(executionContext)

  def destroy(): Unit = {
    white.destroy()
    black.destroy()
    logic.destroy()
    executionContext.shutdown()
  }
}
