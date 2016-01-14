package robot.piece

abstract class Piece {
  def gripHeight: Double
  def gripWidth: Double
}

object Pawn extends Piece {
  def gripHeight = 22
  def gripWidth = 14
}

object Rook extends Piece {
  def gripHeight = 31
  def gripWidth = 20.5
}

object Knight extends Piece {
  def gripHeight = 8
  def gripWidth = 23
}

object Bishop extends Piece {
  def gripHeight = 40
  def gripWidth = 13
}

object Queen extends Piece {
  def gripHeight = 38.5
  def gripWidth = 17
}

object King extends Piece {
  def gripHeight = 36.5
  def gripWidth = 16.4
}
