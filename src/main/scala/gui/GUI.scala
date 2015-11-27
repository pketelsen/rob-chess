package gui

import model._
import controller.Application
import scala.collection.mutable.Subscriber
import scala.collection.mutable.Publisher

class GUI(app: Application) extends Subscriber[GameState, Publisher[GameState]] {
  def getMove(message: String): Move = ???

  override def notify(pub: Publisher[GameState], event: GameState) = ???
}