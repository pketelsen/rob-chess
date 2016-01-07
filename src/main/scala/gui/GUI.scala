package gui

import model.Move
import model.Game
import model.GameSubscriber
import scala.concurrent.Future
import controller.StartCalibrationEvent
import controller.Application
import controller.Host
import controller.QuitEvent

class GUI extends GameSubscriber {
  def showMessage(message: String) = println(message)
  def getMove(): Move = ???

  def handle(move: Move): Future[Unit] = Future.successful(())

  val robotHost = Host("localhost", 5005)
  val trackingHost = Host("141.83.19.44", 5000)

  Application.queueEvent(StartCalibrationEvent(robotHost, trackingHost))
  Application.queueEvent(QuitEvent)
}