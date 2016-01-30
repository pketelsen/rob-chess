package controller

import java.util.concurrent.Executors

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import logic.CECPLogic
import logic.ChessLogic
import logic.ResultDraw
import logic.ResultWin
import model.Black
import model.Color
import model.Move
import model.White

class Game(white: Player, black: Player) {
  private var turn: Color = White

  private val logic: ChessLogic = new CECPLogic

  private val executionContext = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

  val board = new Board()

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
        player.acceptMove()
        move
      case None =>
        makeTurn(player, true)
    }
  }

  def AIMove(): Unit = board.AIMove(turn)

  private def moveBoardAndWait(move: Move): Unit = {
    Await.ready(board.move(move), Duration.Inf)
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
        moveBoardAndWait(move)

        turn = turn.other
        Application.queueEvent(NextTurnEvent)

      case Some(result) =>
        result match {
          case ResultWin(color, message) => Application.showMessage(s"$color wins: $message")
          case ResultDraw(message) => Application.showMessage(s"Draw: $message")
        }

        moveBoardAndWait(move)
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
