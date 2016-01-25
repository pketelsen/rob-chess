package model

abstract sealed class Piece

case object Pawn extends Piece {
  override def toString() = ""
}

case object Rook extends Piece {
  override def toString() = "r"
}

case object Knight extends Piece {
  override def toString() = "n"
}

case object Bishop extends Piece {
  override def toString() = "b"
}

case object King extends Piece {
  override def toString() = "k"
}

case object Queen extends Piece {
  override def toString() = "q"
}

case class BoardPos(file: Int, rank: Int) {
  override def toString() = s"${"abcdefgh"(file)}${rank + 1}"
}

sealed abstract class Move {
  override def toString(): String
}

case class NormalMove(src: BoardPos, dest: BoardPos) extends Move {
  override def toString() = src.toString + dest.toString
}
case class PromotionMove(src: BoardPos, dest: BoardPos, promotion: Piece) extends Move {
  override def toString() = src.toString + dest.toString + promotion.toString
}

case class GameState(moves: Seq[Move])