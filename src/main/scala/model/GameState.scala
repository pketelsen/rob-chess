package model

sealed class Piece

case object Pawn extends Piece
case object Rook extends Piece
case object Knight extends Piece
case object Bishop extends Piece
case object King extends Piece
case object Queen extends Piece


sealed abstract class Move

case class BoardPos(file: Int, rank: Int)

case class NormalMove(src: BoardPos, dest: BoardPos) extends Move
case class PromotionMove(src: BoardPos, dest: BoardPos, promotion: Piece) extends Move


case class GameState(moves: Seq[Move])
