package controller

import scala.annotation.tailrec
import scala.concurrent.Channel

import controller.logic.CECPPlayer
import gui.GUI
import robot.RobotControl
import view.RobotView

object Application {
  private trait State

  private case class StateStart() extends State
  private case class StateCalibrated(robotController: Option[RobotControl]) extends State
  private case class StateRunning(robotController: Option[RobotControl], robotView: Option[RobotView], game: Game) extends State

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
        val robotController = None //Some(new RobotControllerStub) // Some(new RobotController(robotHost, trackingHost))

        Some(StateCalibrated(robotController))

      case (StateCalibrated(robotController), StartGameEvent(whiteInfo, blackInfo)) =>
        val white = mkPlayer(whiteInfo, true)
        val black = mkPlayer(blackInfo, false)

        val game = new Game(white, black)
        val robotView = robotController.map(new RobotView(_))

        game.subscribe(gui)
        robotView.foreach(game.subscribe(_))

        game.run()

        Some(StateRunning(robotController, robotView, game))

      case (StateRunning(_, _, game), NextTurnEvent) =>
        game.run()

        Some(state)

      case (StateRunning(_, robotView, game), EndGameEvent(_)) =>
        queueEvent(QuitEvent)
        //TODO tell robot view to reset the board
        Some(state)

      case (StateRunning(_, _, game), QuitEvent) =>
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