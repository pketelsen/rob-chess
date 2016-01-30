package controller

import scala.collection.mutable.ArraySeq
import scala.collection.mutable.MutableList
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration

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
case class SimpleMove(from: BoardPos, to: BoardPos, piece: Piece, color: Color) extends Action
case class CaptureMove(from: BoardPos, piece: Piece, color: Color) extends Action
case class PromoteMove(to: BoardPos, piece: Piece, color: Color) extends Action

case class BoardState(state: Map[BoardPos, (Piece, Color)]) {
  override def toString = {
    (0 until 8).reverse.map { rank =>
      (0 until 8).map { file =>
        state.get(BoardPos(file, rank)) match {
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
          case None => " " // "　"
        }
      }.mkString("")
    }.mkString("\n")
  }

  def apply(pos: BoardPos): Option[(Piece, Color)] = state.get(pos)
}

trait BoardSubscriber {
  def showMessage(message: String): Unit
  def AIMove(color: Color): Unit
  def resetBoard(board: BoardState): Unit
  def handleActions(actions: List[Action], board: BoardState): Future[Unit]
}

class Board {
  private val subscribers = MutableList[BoardSubscriber]()

  private val state: ArraySeq[ArraySeq[Option[(Piece, Color)]]] =
    ArraySeq(
      lastRow(White),
      pawnRow(White),
      emptyRow,
      emptyRow,
      emptyRow,
      emptyRow,
      pawnRow(Black),
      lastRow(Black))

  private def lastRow(c: Color): ArraySeq[Option[(Piece, Color)]] =
    ArraySeq(
      Some((Rook, c)),
      Some((Knight, c)),
      Some((Bishop, c)),
      Some((Queen, c)),
      Some((King, c)),
      Some((Bishop, c)),
      Some((Knight, c)),
      Some((Rook, c)))

  private def pawnRow(c: Color): ArraySeq[Option[(Piece, Color)]] =
    ArraySeq.fill(8)(Some((Pawn, c)))

  private def emptyRow: ArraySeq[Option[(Piece, Color)]] = ArraySeq.fill(8)(None)

  private def apply(pos: BoardPos): Option[(Piece, Color)] = state(pos.rank)(pos.file)

  private def update(pos: BoardPos, pc: Option[(Piece, Color)]): Unit = {
    state(pos.rank)(pos.file) = pc
  }

  private def modifyBoard(actions: List[Action]): Unit = actions.foreach {
    _ match {
      case SimpleMove(src, dest, _, _) => {
        this(dest) = this(src)
        this(src) = None
      }
      case CaptureMove(src, _, _) => {
        this(src) = None
      }
      case PromoteMove(dest, piece, color) =>
        this(dest) = Some(piece, color)
    }
  }

  def subscribe(sub: BoardSubscriber) = {
    subscribers += sub
    sub.resetBoard(toBoardState)
  }

  def showMessage(message: String): Unit = {
    subscribers.foreach(_.showMessage(message))
  }

  def AIMove(color: Color): Unit = {
    subscribers.foreach(_.AIMove(color))
  }

  private def toBoardState: BoardState = BoardState((0 until 8).flatMap { file =>
    (0 until 8).map { rank =>
      state(rank)(file) match {
        case Some((piece, color)) =>
          Some(BoardPos(file, rank), (piece, color))

        case None =>
          None
      }
    }
  }.flatten.toMap)

  private def handleActionsAndWait(actions: List[Action]): Future[Unit] = {
    val boardState = toBoardState

    val futures = subscribers.map(_.handleActions(actions, boardState))

    Future {
      futures.foreach { Await.ready(_, Duration.Inf) }
    }
  }

  def move(move: Move): Future[Unit] = {
    val Move(src, dest, promotion) = move

    val Some((srcPiece, srcColor)) = this(src)

    val baseActions: List[Action] = {
      val (srcPiece, srcColor) = this(src).get
      if (this(dest) != None) { // NORMAL CAPTURE
        val Some((destPiece, destColor)) = this(dest)
        List(
          CaptureMove(dest, destPiece, destColor),
          SimpleMove(src, dest, srcPiece, srcColor))

      } else if (srcPiece == King && 1 < Math.abs(src.file - dest.file)) { // CASTLING
        val direction = src.file - dest.file
        val rank = src.rank
        val (rookSrc, rookDest) =
          if (direction < 0)
            (new BoardPos(7, rank), new BoardPos(5, rank))
          else
            (new BoardPos(0, rank), new BoardPos(3, rank))

        List(SimpleMove(src, dest, King, srcColor),
          SimpleMove(rookSrc, rookDest, Rook, srcColor))

      } else if (srcPiece == Pawn && src.file != dest.file) { // EN PASSANT
        List(
          CaptureMove(BoardPos(dest.file, src.rank), Pawn, srcColor.other),
          SimpleMove(src, dest, Pawn, srcColor))

      } else { // NORMAL MOVE
        List(SimpleMove(src, dest, srcPiece, srcColor))
      }
    }

    val promotionActions: List[Action] = promotion match {
      case Some(piece) =>
        List(
          CaptureMove(dest, Pawn, srcColor),
          PromoteMove(dest, piece, srcColor))

      case None =>
        List()
    }

    val actions = baseActions ++ promotionActions

    modifyBoard(actions)
    handleActionsAndWait(actions)
  }
}