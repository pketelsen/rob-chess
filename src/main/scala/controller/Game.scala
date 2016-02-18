package controller

import java.util.concurrent.Executors
import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
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
import java.util.concurrent.TimeUnit

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
        move
      case None =>
        makeTurn(player, true)
    }
  }

  def AIMove(): Unit = board.AIMove(turn)

  /** Makes the next step in the game, and handles the outcomes. */
  def run(): Unit = Future {
    val (player, opponent) = turn match {
      case White =>
        (white, black)
      case Black =>
        (black, white)
    }

    val move = makeTurn(player)
    opponent.opponentMove(move)

    val result = logic.getResult

    player.acceptMove(result)

    val boardMove = board.move(move, result)

    result.foreach {
      _ match {
        case ResultWin(color, message) => Application.showMessage(s"$color wins: $message")
        case ResultDraw(message)       => Application.showMessage(s"Draw: $message")
      }
    }

    Await.result(boardMove, Duration.Inf)

    if (result.isEmpty) {
      turn = turn.other
      Application.queueEvent(NextTurnEvent)
    }
  }(executionContext).onFailure {
    case _: MoveAbortedException =>
      println("Game aborted.")

    case e: Exception =>
      e.printStackTrace()
  }

  def destroy(): Unit = {
    white.destroy()
    black.destroy()
    logic.destroy()
    executionContext.shutdown()

    while (!executionContext.awaitTermination(1, TimeUnit.DAYS)) {}

    // TODO Reset board
  }
}
