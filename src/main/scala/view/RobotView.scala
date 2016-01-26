package view

import scala.language.implicitConversions
import scala.concurrent.Future
import robot.RobotController
import scala.annotation.tailrec
import model._
import scala.collection.mutable.ListBuffer
import robot.RobotControl

class RobotView(rc: RobotControl) extends BoardView {

  @tailrec
  final def handleActions(l: List[Action]): Future[Unit] = {
    l match {
      case head :: tail =>
        doAction(head)
        handleActions(tail)
      case Nil => Future.successful(())
    }
  }
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
  val captureCounters: Map[Color, CaptureCounter] = Map(Black -> { new CaptureCounter }, White -> { new CaptureCounter })

  private def doAction(a: Action) = {
    a match {
      case SimpleMove(from, to, piece) => {
        rc.movePiece(from, to, piece)
      }
      case CaptureMove(from, piece) => {
        val (_, color) = board(from).get
        val idx = captureCounters(color).get
        rc.capturePiece(from, idx, color, piece)
        capturedPieces((piece, color)) += idx
        captureCounters(color).inc()
      }
      case PromoteMove(to, piece, color) => {
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