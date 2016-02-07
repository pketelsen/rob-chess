package view

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.implicitConversions

import controller.Action
import controller.BoardState
import controller.BoardSubscriber
import controller.CaptureAction
import controller.MoveAction
import controller.PromoteAction
import model.Black
import model.BoardPos
import model.Color
import model.Pawn
import model.Piece
import model.White
import robot.RobotControl

class RobotView(rc: RobotControl) extends BoardSubscriber {
  private val capturedPieces: Map[Color, mutable.Map[Int, Piece]] = Map(White -> mutable.Map(), Black -> mutable.Map())

  private def nextCaptureIndex(color: Color): Int = {
    val map = capturedPieces(color)

    @tailrec
    def index(i: Int): Int = {
      if (map.contains(i))
        return index(i + 1)
      else
        return i
    }

    index(0)
  }

  private def findCapturedPiece(piece: Piece, color: Color): Option[Int] =
    capturedPieces(color) find (_._2 == piece) map (_._1)

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
          val (pos, _) = wrong.head
          val nearestEmpty = (allPositions -- boardState.state.keys)
            .minBy(other => Math.pow(pos.file - other.file, 2) + Math.pow(pos.rank - other.rank, 2))
          MoveAction(pos, nearestEmpty)
      }

      doAction(action)
    }
  }

  def reset(): Future[Unit] = Future {
    setBoard(BoardState())
    assert(capturedPieces.values.forall(_.isEmpty))
  }

  private def doAction(action: Action) = {
    boardState = boardState + (action match {
      case MoveAction(from, to) =>
        val Some((piece, _)) = boardState(from)
        rc.movePiece(from, to, piece)
        MoveAction(from, to)

      case CaptureAction(from) =>
        val Some((piece, color)) = boardState(from)
        val idx = nextCaptureIndex(color)

        rc.capturePiece(from, idx, color, piece)
        capturedPieces(color)(idx) = piece

        CaptureAction(from)

      case PromoteAction(to, promotionPiece, color) => {
        val (piece, idx) = findCapturedPiece(promotionPiece, color) match {
          case None => (Pawn, findCapturedPiece(Pawn, color).get) // Fall back to pawn if no matching piece exists
          case Some(i) => (promotionPiece, i)
        }

        capturedPieces(color) -= idx

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