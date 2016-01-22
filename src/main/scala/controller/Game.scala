package controller

import scala.annotation.tailrec
import scala.collection.mutable.MutableList
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import model.Move
import logic._

trait GameSubscriber {
  def handle(move: Move): Future[Unit]
}

class Game(white: Player, black: Player) {
  val subscribers = MutableList[GameSubscriber]()

  var whitesTurn = true

  /* List of all moves, for resetting the board at the end. */
  val logic: ChessLogic = new CECPLogic

  private def publishAndWait(move: Move) = {
    val futures = subscribers.map(_.handle(move))

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

    opponent.opponentMove(move)
    publishAndWait(move)

    whitesTurn = !whitesTurn
  }
}
