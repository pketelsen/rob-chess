package controller

import scala.collection.mutable.MutableList
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.annotation.tailrec
import model._


trait GameSubscriber {
  def handle(move: Move): Future[Unit]
}

class Game(white: Player, black: Player) {

  val subscribers = MutableList[GameSubscriber]()

  /* List of all moves, for resetting the board at the end. */
  val chessGame: ChessLogic = ???

  private def publishAndWait(move: Move) = {
    val futures = subscribers.map(_.handle(move))

    futures.foreach { Await.ready(_, Duration.Inf) }
  }

  def subscribe(sub: GameSubscriber) = subscribers += sub

  @tailrec
  private def makeTurn(player: Player, wasInvalid: Boolean = false): Move = {
    val move = player.getMove(wasInvalid)

    if (chessGame.attemptMove(move))
      move
    else
      makeTurn(player, true)
  }

  @tailrec
  private def doTurn(whitesTurn: Boolean): Unit = {
    val move = makeTurn(if (whitesTurn) white else black)
    publishAndWait(move)

    doTurn(!whitesTurn);
  }

  def run() = doTurn(true)
}

trait ChessLogic {
  def attemptMove(move: Move): Boolean
}