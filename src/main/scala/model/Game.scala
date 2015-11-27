package model

import scala.collection.mutable.Publisher
import scala.collection.mutable.Stack
import controller.Player

class Game(white: Player, black: Player) extends Runnable with Publisher[GameState]{
  /* List of all moves, for resetting the board at the end. */
  val moves: Stack[Move] = Stack()
  var whitesTurn: Boolean = true
  val chessGame: GNUChessWrapper = ???

  override def run() {
    while (!chessGame.isOver) {
      moves.push(if (whitesTurn) white.move(chessGame)
      else black.move(chessGame))
      whitesTurn = !whitesTurn
      //TODO allow time for Robot movement
    }
  }
}

class GNUChessWrapper {
  //TODO
  def isOver: Boolean = ???
  def isValidMove(move: Move): Boolean = ???
}