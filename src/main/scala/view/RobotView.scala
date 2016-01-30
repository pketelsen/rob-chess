package view

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

import controller.Action
import controller.BoardState
import controller.BoardSubscriber
import controller.CaptureAction
import controller.MoveAction
import controller.PromoteAction
import model.Bishop
import model.Black
import model.Color
import model.Knight
import model.Pawn
import model.Piece
import model.Queen
import model.Rook
import model.White
import robot.RobotControl

class RobotView(rc: RobotControl) extends BoardSubscriber {
  private val capturedPieces: Map[(Piece, Color), ListBuffer[Int]] =
    Map(((Pawn, White) -> new ListBuffer[Int]),
      ((Rook, White) -> new ListBuffer[Int]),
      ((Knight, White) -> new ListBuffer[Int]),
      ((Bishop, White) -> new ListBuffer[Int]),
      ((Queen, White) -> new ListBuffer[Int]),
      ((Pawn, Black) -> new ListBuffer[Int]),
      ((Rook, Black) -> new ListBuffer[Int]),
      ((Knight, Black) -> new ListBuffer[Int]),
      ((Bishop, Black) -> new ListBuffer[Int]),
      ((Queen, Black) -> new ListBuffer[Int]))

  class CaptureCounter {
    private var ctr = 0
    def inc() = { ctr = ctr + 1; ctr }
    def get = ctr
  }
  private val captureCounters: Map[Color, CaptureCounter] = Map(Black -> { new CaptureCounter }, White -> { new CaptureCounter })

  private var boardState: BoardState = BoardState()

  def showMessage(message: String): Unit = ()
  def AIMove(color: Color): Unit = ()

  def resetBoard(board: BoardState): Unit = {
    // TODO Implement
    // Make this a future?

    boardState = board
  }

  def handleMoveString(move: String, color: Color): Unit = ()

  def handleActions(l: List[Action], board: BoardState): Future[Unit] = Future {
    l.foreach(doAction(_))
  }

  private def doAction(action: Action) = {
    action match {
      case MoveAction(from, to) =>
        val Some((piece, _)) = boardState(from)
        rc.movePiece(from, to, piece)

      case CaptureAction(from) =>
        val Some((piece, color)) = boardState(from)
        val idx = captureCounters(color).get
        rc.capturePiece(from, idx, color, piece)
        capturedPieces((piece, color)) += idx
        captureCounters(color).inc()

      case PromoteAction(to, piece, color) => {
        val pcs = capturedPieces((piece, color))
        val idx = if (pcs.isEmpty) {
          println(s"No $color $piece found, this wasnt supposed to happen.")
          throw new RuntimeException("Promotion with nonexistent piece. Not supported by robot.")
        } else {
          val i = pcs.head
          pcs -= i
          i
        }
        rc.promotePiece(idx, color, to, piece)
      }
    }

    boardState = boardState(action)
  }

  implicit def toRobPiece(mP: model.Piece): robot.piece.Piece = {
    mP match {
      case model.Pawn => robot.piece.Pawn
      case model.Rook => robot.piece.Rook
      case model.Knight => robot.piece.Knight
      case model.Bishop => robot.piece.Bishop
      case model.King => robot.piece.King
      case model.Queen => robot.piece.Queen
    }
  }
}