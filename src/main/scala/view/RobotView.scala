package view

import scala.concurrent.Future
import robot.RobotController
import scala.annotation.tailrec

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

  private def doAction(a: Action) = {
    a match {
      case SimpleMove(from, to, piece)   => rc.movePiece(from.file, from.rank, to.file, to.rank, piece)
      case CaptureMove(from, piece)      => ???
      case PromoteMove(to, piece, color) => ???
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