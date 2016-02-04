package robot.piece

sealed abstract class Piece {
  def gripHeight: Double
  def gripWidth: Double
}

object Pawn extends Piece {
  def gripHeight = 22
  def gripWidth = 14
  override def toString = "Pawn"
}

object Rook extends Piece {
  def gripHeight = 31
  def gripWidth = 20.5
  override def toString = "Rook"
}

object Knight extends Piece {
  def gripHeight = 9.5
  def gripWidth = 23
  override def toString = "Knight"
}

object Bishop extends Piece {
  def gripHeight = 40
  def gripWidth = 13
  override def toString = "Bishop"
}

object Queen extends Piece {
  def gripHeight = 38.5
  def gripWidth = 17
  override def toString = "Queen"
}

object King extends Piece {
  def gripHeight = 36.5
  def gripWidth = 16.4
  override def toString = "King"
}
