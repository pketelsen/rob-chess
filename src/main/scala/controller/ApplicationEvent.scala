package controller

import view.RobotView

case class Host(hostname: String, port: Int)
case class PlayerInfo(playerType: PlayerType)

abstract sealed class ApplicationEvent

case object InitEvent extends ApplicationEvent

case class RobotSetupEvent(robot: Option[RobotView]) extends ApplicationEvent

case class StartGameEvent(
  whiteInfo: PlayerInfo,
  blackInfo: PlayerInfo)
    extends ApplicationEvent

case object NextTurnEvent extends ApplicationEvent

case object EndGameEvent
  extends ApplicationEvent

case object AIMoveEvent
  extends ApplicationEvent

case class MessageEvent(message: String) extends ApplicationEvent

case object QuitEvent extends ApplicationEvent