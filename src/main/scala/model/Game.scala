package model

import scala.collection.mutable.MutableList
import controller.Player
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class Game(white: Player, black: Player) {
  type Subscriber = (Move => Future[Unit])
  val subscribers = MutableList[Subscriber]()

  /* List of all moves, for resetting the board at the end. */
  val chessGame: ChessLogic = ???

  private def publishAndWait(move: Move) = {
    val futures = subscribers.map(_(move))

    futures.foreach { Await.ready(_, Duration.Inf) }
  }

  def subscribe(sub: Subscriber) = subscribers += sub

  def doTurn(whitesTurn: Boolean): Unit = {
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