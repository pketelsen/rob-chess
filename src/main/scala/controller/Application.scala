package controller

import model._
import controller._
import gui.GUI
import scala.concurrent.Channel
import robot._
import scala.annotation.tailrec

object Application {
  private trait State

  private case class StateStart() extends State
  private case class StateCalibrated(robotController: RobotController) extends State
  private case class StateRunning(robotController: RobotController, game: Game) extends State

  private val marker = "PolarisActive_1"

  private val eventBus = new Channel[ApplicationEvent]
  private val gui = new GUI

  private def mkPlayer(info: PlayerInfo, white: Boolean): Player =
    if (info.ai)
      new AIPlayer(white)
    else
      new HumanPlayer(white, gui)

  def raiseEvent(event: ApplicationEvent) = eventBus.write(event)

  def showMessage(message: String) = raiseEvent(MessageEvent(message))

  @tailrec
  def handleEvents(state: State): Unit = {
    (state, eventBus.read) match {
      case (StateStart(), StartCalibrationEvent(robotHost, trackingHost)) =>
        val robot = new Robot(robotHost)
        val tracking = new Tracking(robotHost)

        robot.setSpeed(10)

        val robotController = new RobotController(robot, tracking)

        handleEvents(StateCalibrated(robotController))

      case (StateCalibrated(robotController), StartGameEvent(whiteInfo, blackInfo)) =>
        val white = mkPlayer(whiteInfo, true)
        val black = mkPlayer(blackInfo, false)

        val game = new Game(white, black)

        game.subscribe(gui)
        game.subscribe(robotController)

        handleEvents(StateRunning(robotController, game))

      case (_, MessageEvent(message)) =>
        gui.showMessage(message)
        handleEvents(state)

      case (_, QuitEvent) =>
        return
        
      case (_, event) =>
        println(s"Unexpected event ${event} in state ${state}")
        handleEvents(state)
    }
  }

  def main(args: Array[String]) = handleEvents(StateStart())
}