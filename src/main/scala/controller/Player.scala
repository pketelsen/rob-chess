package controller

import model._

abstract class Player(name: String) {
  def move(chessGame: GNUChessWrapper): Move;
}

class AIPlayer(name: String) extends Player(name) {
  def move(chessGame: GNUChessWrapper): Move = ???
    // get move from GNUChessWrapper and return it
}
class HumanPlayer(name: String, getMoveFromGui: String => Move) extends Player(name) {
  def move(chessGame: GNUChessWrapper): Move = {
    var move: Move = getMoveFromGui("Please enter a move for Player " + name)
    while(!chessGame.isValidMove(move)){
      move = getMoveFromGui("That move was invalid! Please enter a move for Player " + name)
    }
    return move
  }
}

