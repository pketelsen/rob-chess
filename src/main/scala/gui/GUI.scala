package gui

import model.Move
import controller.Game
import controller.GameSubscriber
import scala.concurrent.Future
import controller.StartCalibrationEvent
import controller.Application
import controller.Host
import controller.QuitEvent
import controller.StartGameEvent
import controller.PlayerInfo

class GUI extends GameSubscriber {
  var whitesTurn = true;

  def showMessage(message: String) = println(message)
  def getMove(): Move = ???

  def handle(move: Move): Future[Unit] = {
    println(s"${if (whitesTurn) "white" else "black"} made move $move")

    whitesTurn = !whitesTurn;

    Future.successful(())
  }

  val robotHost = Host("localhost", 5005)
  val trackingHost = Host("141.83.19.44", 5000)

  Application.queueEvent(StartCalibrationEvent(robotHost, trackingHost))
  Application.queueEvent(StartGameEvent(PlayerInfo(true), PlayerInfo(true)))
  Application.queueEvent(QuitEvent)
}