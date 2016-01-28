package view

import scala.collection.mutable.ArraySeq
import scala.concurrent.Future

import controller.GameEvent
import controller.GameSubscriber
import model.Bishop
import model.Black
import model.BoardPos
import model.Color
import model.King
import model.Knight
import model.Move
import model.Pawn
import model.Piece
import model.Queen
import model.Rook
import model.White

sealed abstract class Action
case class SimpleMove(from: BoardPos, to: BoardPos, piece: Piece) extends Action
case class CaptureMove(from: BoardPos, piece: Piece) extends Action
case class PromoteMove(to: BoardPos, piece: Piece, color: Color) extends Action

trait BoardView extends GameSubscriber {

  val board = new Board()

  def handle(event: GameEvent): Future[Unit] = {
    val Move(src, dest, promotion) = event.move

    val baseActions: List[Action] = {
      val srcPiece = board(src).get._1
      //NORMAL CAPTURE
      if (board(dest) != None) {
        val destPiece = board(dest).get._1
        List(CaptureMove(dest, destPiece),
          SimpleMove(src, dest, srcPiece))

        //CASTLING
      } else if (srcPiece == King && 1 < Math.abs(src.file - dest.file)) {
        val direction = src.file - dest.file
        val rank = src.rank
        val (rookSrc, rookDest) =
          if (direction < 0)
            (new BoardPos(7, rank), new BoardPos(5, rank))
          else
            (new BoardPos(0, rank), new BoardPos(2, rank))

        List(SimpleMove(src, dest, King),
          SimpleMove(rookSrc, rookDest, Rook))

        // EN PASSANT
      } else if (srcPiece == Pawn && src.file != dest.file) {
        List(CaptureMove(BoardPos(dest.file, src.rank), Pawn),
          SimpleMove(src, dest, Pawn))

        // NORMAL MOVE
      } else {
        List(SimpleMove(src, dest, srcPiece))
      }
    }

    val promotionActions: List[Action] = promotion match {
      case Some(piece) =>
        List(CaptureMove(dest, Pawn),
          PromoteMove(dest, piece, board(src).get._2))

      case None =>
        List()
    }

    val actions = baseActions ++ promotionActions

    modifyBoard(actions)
    println(board)
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
    boardState(pos.rank)(pos.file) = pc
  }
  def apply(pos: BoardPos): Option[(Piece, Color)] = boardState(pos.rank)(pos.file)

  override def toString = {
    boardState.map {
      _.map {
        _ match {
          case Some((piece, color)) => {
            (piece, color) match {
              case (Pawn, White) => "♙"
              case (Rook, White) => "♖"
              case (Knight, White) => "♘"
              case (Bishop, White) => "♗"
              case (Queen, White) => "♕"
              case (King, White) => "♔"
              case (Pawn, Black) => "♟"
              case (Rook, Black) => "♜"
              case (Knight, Black) => "♞"
              case (Bishop, Black) => "♝"
              case (Queen, Black) => "♛"
              case (King, Black) => "♚"

            }
          }
          case None => " "
        }
      }.mkString("")
    }.reverse.mkString("\n")

  }
}