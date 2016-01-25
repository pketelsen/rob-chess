package view

import model.Move
import controller.GameSubscriber
import scala.concurrent.Future
import model._
import scala.collection.mutable.ArraySeq

sealed abstract class Color
case object Black extends Color
case object White extends Color

sealed abstract class Action
case class SimpleMove(from: BoardPos, to: BoardPos, piece: Piece) extends Action
case class CaptureMove(from: BoardPos, piece: Piece) extends Action
case class PromoteMove(to: BoardPos, piece: Piece, color: Color) extends Action

trait BoardView extends GameSubscriber {

  val board = new Board()

  def handle(move: Move): Future[Unit] = {
    val actions: List[Action] = move match {
      case PromotionMove(src, dest, piece) =>
        List(SimpleMove(src, dest, Pawn),
          CaptureMove(dest, Pawn),
          PromoteMove(dest, piece, board(src).get._2))
      case NormalMove(src, dest) => {
        val srcPiece = board(src).get._1

        //NORMAL CAPTURE
        if (board(dest) != None) {
          val destPiece = board(dest).get._1
          List(CaptureMove(dest, destPiece),
            SimpleMove(src, dest, srcPiece))

          //CASTLING
        } else if (srcPiece == King && 1 < Math.abs(src.rank - dest.rank)) {
          val direction = src.rank - dest.rank
          val file = src.file
          val (rookSrc, rookDest) =
            if (direction > 0)
              (new BoardPos(file, 7), new BoardPos(file, 5))
            else
              (new BoardPos(file, 0), new BoardPos(file, 2))

          List(SimpleMove(src, dest, King),
            SimpleMove(rookSrc, rookDest, Rook))

          // EN PASSANT
        } else if (srcPiece == Pawn && src.file != dest.file) {
          List(CaptureMove(BoardPos(src.rank, dest.file), Pawn),
            SimpleMove(src, dest, Pawn))

          // NORMAL MOVE
        } else {
          List(SimpleMove(src, dest, srcPiece))
        }
      }
    }
    modifyBoard(actions)
    handleActions(actions)
  }
  protected def modifyBoard(actions: List[Action]) = actions.foreach {
    _ match {
      case SimpleMove(src, dest, piece) => {
        board(dest) = board(src)
        board(src) = None
      }
      case CaptureMove(src, piece) => {
        board(src) = None
      }
      case PromoteMove(dest, piece, color) =>
        board(dest) = Some(piece, color)
    }
  }
  /** This method has to be implemented by non abstract subclasses. */
  def handleActions(l: List[Action]): Future[Unit]
}

class Board {
  val boardState: ArraySeq[ArraySeq[Option[(Piece, Color)]]] =
    ArraySeq(lastRow(White), pawnRow(White), emptyRow, emptyRow,
      emptyRow, emptyRow, pawnRow(Black), lastRow(Black))

  private def lastRow(c: Color): ArraySeq[Option[(Piece, Color)]] =
    ArraySeq(Some((Rook, c)), Some((Knight, c)), Some((Bishop, c)), Some((Queen, c)),
      Some((King, c)), Some((Bishop, c)), Some((Knight, c)), Some((Rook, c)))

  private def pawnRow(c: Color): ArraySeq[Option[(Piece, Color)]] =
    ArraySeq.fill(8)(Some((Pawn, c)))

  private def emptyRow: ArraySeq[Option[(Piece, Color)]] = ArraySeq.fill(8)(None)

  def update(pos: BoardPos, pc: Option[(Piece, Color)]): Unit = {
    boardState(pos.file)(pos.rank) = pc
  }
  def apply(pos: BoardPos): Option[(Piece, Color)] = boardState(pos.file)(pos.rank)
}