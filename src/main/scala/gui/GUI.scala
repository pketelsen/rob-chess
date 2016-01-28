package gui

import scala.concurrent.Future
import scala.io.StdIn

import controller.Application
import controller.GameEvent
import controller.GameSubscriber
import controller.Host
import controller.PlayerInfo
import controller.StartCalibrationEvent
import controller.StartGameEvent
import controller.logic.CECP
import controller.logic.ResultBlackWins
import controller.logic.ResultDraw
import controller.logic.ResultWhiteWins
import model.Move

class GUI extends GameSubscriber {
  var whitesTurn = true;

  def showMessage(message: String) = println(message)
  def getMove(): Move = {
    println("please enter move: ")
    val in = StdIn.readLine()
    CECP.parseMove(in)
  }

  def handle(event: GameEvent): Future[Unit] = {
    println(s"${if (whitesTurn) "White" else "Black"} made move ${event.move}")

    event.result match {
      case Some(ResultWhiteWins(message)) => println(s"White wins: $message")
      case Some(ResultBlackWins(message)) => println(s"Black wins: $message")
      case Some(ResultDraw(message))      => println(s"Draw: $message")
      case _                              =>
    }

    whitesTurn = !whitesTurn;

    Future.successful(())
  }

  val robotHost = Host("localhost", 5005)
  val trackingHost = Host("141.83.19.44", 5000)

  Application.queueEvent(StartCalibrationEvent(robotHost, trackingHost))
  Application.queueEvent(StartGameEvent(PlayerInfo(true), PlayerInfo(true)))
}