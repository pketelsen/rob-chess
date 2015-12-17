package gui

import model.Move
import model.Game
import model.GameSubscriber
import scala.concurrent.Future

class GUI extends GameSubscriber {
  def showMessage(message: String) = println(message)
  def getMove(): Move = ???
  
  def handle(move: Move): Future[Unit] = Future.successful(())
}