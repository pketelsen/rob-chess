package controller

import scala.annotation.tailrec
import scala.concurrent.Channel

import controller.logic.CECPPlayer
import model.Black
import model.Color
import model.White
import view.ConsoleView
import view.RobotView
import view.gui.GUI
import view.gui.GameGUI
import view.gui.PlayerSetupGUI
import view.gui.RobotSetupGUI

object Application {
  private trait State

  private case class StateStart(gameGUI: GameGUI) extends State
  private case class StateRobotSetup(gameGUI: GameGUI, gui: RobotSetupGUI) extends State
  private case class StatePlayerSetup(gameGUI: GameGUI, gui: PlayerSetupGUI, robot: Option[RobotView]) extends State
  private case class StateRunning(gameGUI: GameGUI, robot: Option[RobotView], game: Game) extends State

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
      case (StateStart(gameGUI), InitEvent) =>
        val robotSetupGUI = new RobotSetupGUI
        Some(StateRobotSetup(gameGUI, robotSetupGUI))

      case (StateRobotSetup(gameGUI, robotSetupGUI), RobotSetupEvent(robot)) =>
        gameGUI.reset()

        val playerSetupGUI = new PlayerSetupGUI(gameGUI.window)
        Some(StatePlayerSetup(gameGUI, playerSetupGUI, robot))

      case (StatePlayerSetup(gameGUI, playerSetupGUI, robot), StartGameEvent(whiteInfo, blackInfo)) =>
        val white = mkPlayer(whiteInfo, White, gameGUI)
        val black = mkPlayer(blackInfo, Black, gameGUI)

        val game = new Game(white, black)

        game.board.subscribe(ConsoleView)
        game.board.subscribe(gameGUI)
        robot.foreach(game.board.subscribe(_))

        game.run()

        Some(StateRunning(gameGUI, robot, game))

      case (StateRunning(_, _, game), NextTurnEvent) =>
        game.run()

        Some(state)

      case (StateRunning(_, robot, game), EndGameEvent) =>
        queueEvent(QuitEvent)
        //TODO tell robot view to reset the board
        Some(state)

      case (StateRunning(_, _, game), AIMoveEvent) =>
        game.AIMove()
        Some(state)

      case (StateRunning(_, _, game), MessageEvent(message)) =>
        game.board.showMessage(message)
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

    GUI.init()
    val gameGUI = new GameGUI

    handleEvents(StateStart(gameGUI))

    gameGUI.dispose()
  }
}