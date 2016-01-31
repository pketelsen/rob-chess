package controller

import scala.collection.mutable.MutableList
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import controller.logic.Result
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
import controller.logic.ResultWin

sealed abstract class Action
case class MoveAction(from: BoardPos, to: BoardPos) extends Action
case class CaptureAction(from: BoardPos) extends Action
case class PromoteAction(to: BoardPos, piece: Piece, color: Color) extends Action

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

  def apply(action: Action): BoardState = BoardState(action match {
    case MoveAction(src, dest) =>
      state + (dest -> state(src)) - src

    case CaptureAction(src) =>
      state - src

    case PromoteAction(dest, piece, color) =>
      state + (dest -> (piece, color))
  })

  def apply(actions: List[Action]): BoardState = (actions foldLeft this)((board, action) => board(action))

  def apply(pos: BoardPos): Option[(Piece, Color)] = state.get(pos)
}

object BoardState {
  private def figures(rank: Int, c: Color): Seq[(BoardPos, (Piece, Color))] =
    Seq(
      (BoardPos(0, rank) -> (Rook, c)),
      (BoardPos(1, rank) -> (Knight, c)),
      (BoardPos(2, rank) -> (Bishop, c)),
      (BoardPos(3, rank) -> (Queen, c)),
      (BoardPos(4, rank) -> (King, c)),
      (BoardPos(5, rank) -> (Bishop, c)),
      (BoardPos(6, rank) -> (Knight, c)),
      (BoardPos(7, rank) -> (Rook, c)))

  private def pawns(rank: Int, c: Color): Seq[(BoardPos, (Piece, Color))] =
    (0 until 8).map { file =>
      (BoardPos(file, rank) -> (Pawn, c))
    }

  private val initialState =
    BoardState((
      figures(0, White) ++
      pawns(1, White) ++
      pawns(6, Black) ++
      figures(7, Black)).toMap)

  def apply(): BoardState = initialState
}

trait BoardSubscriber {
  def showMessage(message: String): Unit
  def AIMove(color: Color): Unit
  def reset(): Future[Unit]
  def handleMove(actions: List[Action], color: Color, move: String, board: BoardState): Future[Unit]
}

class Board {
  private val subscribers = MutableList[BoardSubscriber]()

  private var boardState: BoardState = BoardState()

  def subscribe(sub: BoardSubscriber) = {
    subscribers += sub
  }

  def showMessage(message: String): Unit = {
    subscribers.foreach(_.showMessage(message))
  }

  def AIMove(color: Color): Unit = {
    subscribers.foreach(_.AIMove(color))
  }

  private def handleMove(actions: List[Action], color: Color, move: String): Future[Unit] = {
    val futures = subscribers.map(_.handleMove(actions, color, move, boardState))

    Future.sequence(futures).map(_ => ())
  }

  def reset(): Future[Unit] = {
    val futures = subscribers.map(_.reset())

    Future.sequence(futures).map(_ => ())
  }

  def move(move: Move, result: Option[Result]): Future[Unit] = {
    val Move(src, dest, promotion) = move

    val Some((piece, color)) = boardState(src)

    val pieceName = piece.toString.toUpperCase

    val (baseActions: List[Action], baseString: String) = {
      if (boardState(dest) != None) { // NORMAL CAPTURE
        assert(boardState(dest).isDefined)

        (List(
          CaptureAction(dest),
          MoveAction(src, dest)),
          s"${pieceName}${src}x${dest}")

      } else if (piece == King && 1 < Math.abs(src.file - dest.file)) { // CASTLING
        val direction = src.file - dest.file
        val rank = src.rank
        val (rookSrc, rookDest, s) =
          if (direction < 0)
            (new BoardPos(7, rank), new BoardPos(5, rank), "0-0")
          else
            (new BoardPos(0, rank), new BoardPos(3, rank), "0-0-0")

        (List(
          MoveAction(src, dest),
          MoveAction(rookSrc, rookDest)), s)

      } else if (piece == Pawn && src.file != dest.file) { // EN PASSANT
        (List(
          CaptureAction(BoardPos(dest.file, src.rank)),
          MoveAction(src, dest)),
          s"${pieceName}${src}x${dest}")

      } else { // NORMAL MOVE
        (List(MoveAction(src, dest)),
          s"${pieceName}${src}-${dest}")
      }
    }

    val (promotionActions: List[Action], promotionString: String) = promotion match {
      case Some(promotionPiece) =>
        (List(
          CaptureAction(dest),
          PromoteAction(dest, promotionPiece, color)),
          promotionPiece.toString.toUpperCase)

      case None =>
        (List(), "")
    }

    val checkmateString = result match {
      case Some(ResultWin(_, _)) => "#"
      case _ => ""
    }

    val actions = baseActions ++ promotionActions
    val string = baseString + promotionString + checkmateString

    boardState = boardState(actions)
    handleMove(actions, color, string)
  }
}