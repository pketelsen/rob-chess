package controller

import scala.annotation.tailrec
import scala.concurrent.Channel
import controller.logic.CECPPlayer
import gui.PlayerSetupGUI
import robot.RobotControl
import view.RobotView
import gui.RobotSetupGUI
import gui.GameGUI
import model.Color
import model.White
import model.Black

object Application {
  private trait State

  private case object StateStart extends State
  private case class StateRobotSetup(gui: RobotSetupGUI) extends State
  private case class StatePlayerSetup(gui: PlayerSetupGUI, robot: Option[RobotView]) extends State
  private case class StateRunning(gui: GameGUI, robot: Option[RobotView], game: Game) extends State

  private val eventBus = new Channel[ApplicationEvent]

  private def mkPlayer(info: PlayerInfo, color: Color, gui: GameGUI): Player =
    info.playerType match {
      case PlayerTypeHuman => new HumanPlayer(color, gui)
      case PlayerTypeAI => new CECPPlayer(color)
    }

  def queueEvent(event: ApplicationEvent) = eventBus.write(event)

  def showMessage(message: String) = queueEvent(MessageEvent(message))

  private def handleEvent(state: State, event: ApplicationEvent): Option[State] = {
    (state, event) match {
      case (StateStart, InitEvent) =>
        val robotSetupGUI = new RobotSetupGUI
        Some(StateRobotSetup(robotSetupGUI))

      case (StateRobotSetup(robotSetupGUI), RobotSetupEvent(robot)) =>
        val playerSetupGUI = new PlayerSetupGUI
        Some(StatePlayerSetup(playerSetupGUI, robot))

      case (StatePlayerSetup(playerSetupGUI, robot), StartGameEvent(whiteInfo, blackInfo)) =>
        val gameGUI = new GameGUI

        val white = mkPlayer(whiteInfo, White, gameGUI)
        val black = mkPlayer(blackInfo, Black, gameGUI)

        val game = new Game(white, black)
        
        //game.subscribe(gui)
        robot.foreach(game.subscribe(_))

        game.run()

        Some(StateRunning(gameGUI, robot, game))

      case (StateRunning(_, _, game), NextTurnEvent) =>
        game.run()

        Some(state)

      case (StateRunning(_, robot, game), EndGameEvent(_)) =>
        queueEvent(QuitEvent)
        //TODO tell robot view to reset the board
        Some(state)

      case (StateRunning(gameGUI, _, _), MessageEvent(message)) =>
        gameGUI.showMessage(message)
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
  private def handleEvents(state: State): Unit = {
    handleEvent(state, eventBus.read) match {
      case Some(newState) =>
        handleEvents(newState)

      case None =>
      // Quit
    }
  }

  def main(args: Array[String]) = {
    queueEvent(InitEvent)
    handleEvents(StateStart)
  }
}