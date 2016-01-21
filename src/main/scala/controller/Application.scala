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

  def showMessage(message: String) = queueEvent(MessageEvent(message))

  @tailrec
  def handleEvents(state: State): Unit = {
    (state, eventBus.read) match {
      case (StateStart(), StartCalibrationEvent(robotHost, trackingHost)) =>
        val robotController = null // new RobotController(robotHost, trackingHost)

        handleEvents(StateCalibrated(robotController))

      case (StateCalibrated(robotController), StartGameEvent(whiteInfo, blackInfo)) =>
        val white = mkPlayer(whiteInfo, true)
        val black = mkPlayer(blackInfo, false)

        val game = new Game(white, black)

        game.subscribe(gui)
        //game.subscribe(robotController)

        game.run()

        handleEvents(StateRunning(robotController, game))

      case (_, MessageEvent(message)) =>
        gui.showMessage(message)
        handleEvents(state)

      case (StateRunning(_, game), QuitEvent) =>
        game.logic.destroy()
        return

      case (_, QuitEvent) =>
        return

      case (_, event) =>
        println(s"Unexpected event ${event} in state ${state}")
        handleEvents(state)
    }
  }

  def main(args: Array[String]) = handleEvents(StateStart())
}