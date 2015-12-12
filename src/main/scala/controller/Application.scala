package controller

import model._
import controller._
import gui.GUI
import scala.concurrent.Channel


object Application {
  private val eventBus = new Channel[ApplicationEvent]
  private val gui = new GUI
  private var game: Option[Game] = None
  

  private def mkPlayer(info: PlayerInfo, white: Boolean): Player =
    if (info.ai)
      new AIPlayer(white)
    else
      new HumanPlayer(white, gui)
  
  def raiseEvent(event: ApplicationEvent) = eventBus.write(event)
  
  def showMessage(message: String) = raiseEvent(MessageEvent(message))
  
  def handleEvents(): Unit = {
    eventBus.read match {
      case StartCalibrationEvent(robot, tracking) => ???
      case StartGameEvent(whiteInfo, blackInfo) => {
        val white = mkPlayer(whiteInfo, true)
        val black = mkPlayer(blackInfo, false)
        game = Some(new Game(white, black))
      }
      case MessageEvent(message) => gui.showMessage(message)
      case QuitEvent => return
    }
    
    handleEvents()
  }

  def main(args: Array[String]) = handleEvents()
}