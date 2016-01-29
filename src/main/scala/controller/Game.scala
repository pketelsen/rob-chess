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
  def showMessage(message: String): Unit
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

  def showMessage(message: String): Unit = {
    subscribers.foreach(_.showMessage(message))
  }

  @tailrec
  private def attemptMoves(moves: List[Move]): Option[Move] = {
    moves match {
      case move :: moves =>
        if (logic.attemptMove(move))
          Some(move)
        else
          attemptMoves(moves)

      case Nil =>
        None
    }
  }

  @tailrec
  private def makeTurn(player: Player, wasInvalid: Boolean = false): Move = {
    val moves = player.getMove(wasInvalid)

    attemptMoves(moves) match {
      case Some(move) =>
        move
      case None =>
        makeTurn(player, true)
    }
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
        result match {
          case ResultWhiteWins(message) => Application.showMessage(s"White wins: $message")
          case ResultBlackWins(message) => Application.showMessage(s"Black wins: $message")
          case ResultDraw(message) => Application.showMessage(s"Draw: $message")
        }

        publishAndWait(move, Some(result))
        Application.queueEvent(EndGameEvent)
    }
  }(executionContext)

  def destroy(): Unit = {
    white.destroy()
    black.destroy()
    logic.destroy()
    executionContext.shutdown()
  }
}
