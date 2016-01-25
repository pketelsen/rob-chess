package view

import scala.concurrent.Future
import robot.RobotController
import scala.annotation.tailrec
import scala.collection.mutable.HashMap
import model._

class RobotView(rc: RobotController) extends BoardView {

  @tailrec
  final def handleActions(l: List[Action]): Future[Unit] = {
    l match {
      case head :: tail =>
        doAction(head)
        handleActions(tail)
      case Nil => Future.successful()
    }
  }
  private val capturedPieces = new HashMap[(Piece, Color), Int]()

  class CaptureCounter {
    private var ctr = 0
    def inc() = { ctr = ctr + 1; ctr }
    def get = ctr
  }
  val captureCounters: Map[Color, CaptureCounter] = Map(Black -> { new CaptureCounter }, White -> { new CaptureCounter })

  private def doAction(a: Action) = {
    a match {
      case SimpleMove(from, to, piece) => {
        rc.movePiece(rc.getBoardPosition(from), rc.getBoardPosition(to), piece)
      }
      case CaptureMove(from, piece) => {
        val (_, color) = board(from).get
        val idx = captureCounters(color).get
        rc.movePiece(rc.getBoardPosition(from), rc.getCapturedPosition(idx, color), piece)
        capturedPieces += (((piece, color), idx))
        captureCounters(color).inc()
      }
      case PromoteMove(to, piece, color) => {
        val idx = capturedPieces.lift((piece, color)) match {
          case Some(i) => i
          case None => {
            println(s"No $color $piece found, assuming there is one at capture position 15")
            15
          }
        }
        rc.movePiece(rc.getCapturedPosition(idx, color), rc.getBoardPosition(to), piece)
      }
    }
  }

  implicit def toRobPiece(mP: model.Piece): robot.piece.Piece = {
    mP match {
      case model.Pawn   => robot.piece.Pawn
      case model.Rook   => robot.piece.Rook
      case model.Knight => robot.piece.Knight
      case model.Bishop => robot.piece.Bishop
      case model.King   => robot.piece.King
      case model.Queen  => robot.piece.Queen
    }
  }
}