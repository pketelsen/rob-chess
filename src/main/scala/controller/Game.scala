package controller

import scala.annotation.tailrec
import scala.collection.mutable.MutableList
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import model.Move
import logic._

case class GameEvent(move: Move, result: Option[Result])

trait GameSubscriber {
  def handle(event: GameEvent): Future[Unit]
}

class Game(white: Player, black: Player) {
  private val subscribers = MutableList[GameSubscriber]()

  private var whitesTurn = true

  private val logic: ChessLogic = new CECPLogic

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

  def run(): Unit = {
    val (player, opponent) =
      if (whitesTurn)
        (white, black)
      else
        (black, white)

    val move = makeTurn(player)

    logic.getResult match {
      case None =>
        opponent.opponentMove(move)
        publishAndWait(move, None)

        whitesTurn = !whitesTurn
        Application.queueEvent(NextTurnEvent)

      case Some(result) =>
        publishAndWait(move, Some(result))
        Application.queueEvent(EndGameEvent(result))
    }
  }

  def destroy(): Unit = {
    white.destroy()
    black.destroy()
    logic.destroy()
  }
}
