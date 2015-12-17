package model

import scala.collection.mutable.MutableList
import controller.Player
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class GameState
class Move

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

  private def doTurn(whitesTurn: Boolean): Unit = {
    val move =
      if (whitesTurn)
        white.getMove(chessGame)
      else
        black.getMove(chessGame)

    publishAndWait(move)

    doTurn(!whitesTurn);
  }

  def run() = doTurn(true)
}

trait ChessLogic {
  def isValidMove(move: Move): Boolean
  def suggestMove(): Move
}