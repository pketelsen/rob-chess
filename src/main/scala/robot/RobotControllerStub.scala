package robot

import model.BoardPos
import robot.piece.Piece
import model.Color

trait RobotControl {
  def movePiece(fromPosition: BoardPos, toPosition: BoardPos, piece: Piece)
  def promotePiece(fromIndex: Int, fromColor: Color, toPosition: BoardPos, piece: Piece)
  def capturePiece(fromPosition: BoardPos, toIndex: Int, toColor: Color, piece: Piece)
}

class RobotControllerStub extends RobotControl {
  def movePiece(fromPosition: BoardPos, toPosition: BoardPos, piece: Piece) = {
    println(s"Robot stub moving a $piece from $fromPosition to $toPosition")
  }
  def promotePiece(fromIndex: Int, fromColor: Color, toPosition: BoardPos, piece: Piece) = {
    println(s"Robot stub promoting a $fromColor $piece from index $fromIndex to $toPosition")
  }
  def capturePiece(fromPosition: BoardPos, toIndex: Int, toColor: Color, piece: Piece) = {
    println(s"Robot stub capturing a $toColor $piece from $fromPosition to index $toIndex")
  }
}

