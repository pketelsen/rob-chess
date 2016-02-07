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
import model.BoardPos
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
    def reset(): Unit = { ctr = 0 }
    def get = ctr
  }
  private val captureCounters: Map[Color, CaptureCounter] = Map(Black -> { new CaptureCounter }, White -> { new CaptureCounter })

  private var boardState: BoardState = BoardState()

  def showMessage(message: String): Unit = ()
  def AIMove(color: Color): Unit = ()

  def handleMove(l: List[Action], color: Color, move: String, board: BoardState): Future[Unit] = Future {
    l.foreach(doAction(_))
  }

  private def setBoard(newState: BoardState) {
    val allPositions: Set[BoardPos] = Set(
      (0 until 8) flatMap { file =>
        (0 until 8) map { rank =>
          BoardPos(file, rank)
        }
      }: _*)

    while (boardState != newState) {
      // Map from empty board positions to pieces supposed to stand there
      val needed = newState.state -- boardState.state.keys

      // Map from positions to wrongly positioned pieces
      val wrong = boardState.state filter {
        case (pos, piece) =>
          newState(pos) != Some(piece)
      }

      val action = needed.headOption match {
        case Some((to, piece)) =>
          wrong.filter(_._2 == piece).headOption match {
            case Some((from, _)) => // There's a wrong piece to fill the empty position
              MoveAction(from, to)

            case None =>
              PromoteAction(to, piece._1, piece._2)
          }
        case None => // All positions are occupied, but there's still something wrong
          val anyEmpty = (allPositions -- boardState.state.keys).head
          MoveAction(wrong.head._1, anyEmpty)
      }

      doAction(action)
    }
  }

  def reset(): Future[Unit] = Future {
    setBoard(BoardState())
    capturedPieces.values.foreach(_.clear())
    captureCounters.values.foreach(_.reset())
  }

  private def doAction(action: Action) = {
    boardState = boardState + (action match {
      case MoveAction(from, to) =>
        val Some((piece, _)) = boardState(from)
        rc.movePiece(from, to, piece)
        MoveAction(from, to)

      case CaptureAction(from) =>
        val Some((piece, color)) = boardState(from)
        val idx = captureCounters(color).get
        rc.capturePiece(from, idx, color, piece)
        capturedPieces((piece, color)) += idx
        captureCounters(color).inc()
        CaptureAction(from)

      case PromoteAction(to, promotionPiece, color) => {
        val (piece, pcs) = capturedPieces((promotionPiece, color)) match {
          case ListBuffer() => (Pawn, capturedPieces((Pawn, color))) // Fall back to pawn if no matching piece exists
          case pieces => (promotionPiece, pieces)
        }

        val idx = pcs.head
        pcs -= idx

        rc.promotePiece(idx, color, to, piece)
        PromoteAction(to, piece, color)
      }
    })
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