package model

sealed abstract class Color {
  def other: Color
}

case object White extends Color {
  def other = Black
}
case object Black extends Color {
  def other = White
}

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

case class Move(src: BoardPos, dest: BoardPos, promotion: Option[Piece]) {
  override def toString(): String =
    src.toString + dest.toString + promotion.map(_.toString).getOrElse("")
}

case class GameState(moves: Seq[Move])