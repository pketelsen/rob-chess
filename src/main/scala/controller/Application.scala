package controller

import controller._
import gui.GUI
import scala.concurrent.Channel
import robot._
import scala.annotation.tailrec
import controller.logic.CECP
import controller.logic.CECPLogic
import model.BoardPos
import model.NormalMove
import controller.logic.CECPPlayer

object Application {
  private trait State

  private case class StateStart() extends State
  private case class StateCalibrated(robotController: RobotController) extends State
  private case class StateRunning(robotController: RobotController, game: Game) extends State

  private val eventBus = new Channel[ApplicationEvent]
  private val gui = new GUI

  private def mkPlayer(info: PlayerInfo, white: Boolean): Player =
    if (info.ai)
      new CECPPlayer(white)
    else
      new HumanPlayer(white, gui)

  def queueEvent(event: ApplicationEvent) = eventBus.write(event)

  def showMessage(message: String) = gui.showMessage(message)

  private def handleEvent(state: State, event: ApplicationEvent): Option[State] = {
    (state, event) match {
      case (StateStart(), StartCalibrationEvent(robotHost, trackingHost)) =>
        val robotController = null // new RobotController(robotHost, trackingHost)

        Some(StateCalibrated(robotController))

      case (StateCalibrated(robotController), StartGameEvent(whiteInfo, blackInfo)) =>
        val white = mkPlayer(whiteInfo, true)
        val black = mkPlayer(blackInfo, false)

        val game = new Game(white, black)

        game.subscribe(gui)
        //game.subscribe(robotController)

        game.run()

        Some(StateRunning(robotController, game))

      case (StateRunning(robotController, game), NextTurnEvent) =>
        game.run()

        Some(state)

      case (StateRunning(_, game), EndGameEvent(_)) =>
        queueEvent(QuitEvent)
        Some(state)

      case (StateRunning(_, game), QuitEvent) =>
        game.destroy()
        None

      case (_, QuitEvent) =>
        None

      case (_, event) =>
        println(s"Unexpected event ${event} in state ${state}")
        Some(state)
    }
  }

  @tailrec
  def handleEvents(state: State): Unit = {
    handleEvent(state, eventBus.read) match {
      case Some(newState) =>
        handleEvents(newState)

      case None =>
      // Quit
    }
  }

  def main(args: Array[String]) = handleEvents(StateStart())
}