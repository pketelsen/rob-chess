package controller

import view.RobotView
import robot.Tracking
import robot.Robot

case class Host(hostname: String, port: Int)
case class PlayerInfo(playerType: PlayerType)

abstract sealed class ApplicationEvent

case object InitEvent extends ApplicationEvent

case class RobotConnectEvent(host: Host) extends ApplicationEvent
case class TrackingConnectEvent(host: Host) extends ApplicationEvent

case class RobotConnectedEvent(robot: Option[Robot]) extends ApplicationEvent
case class TrackingConnectedEvent(tracking: Option[(Tracking, Tracking)]) extends ApplicationEvent

case object CalibrateRobotEvent extends ApplicationEvent
case object MeasureBoardEvent extends ApplicationEvent

case class RobotSetupEvent(robot: Option[RobotView]) extends ApplicationEvent

case object NewGameEvent extends ApplicationEvent

case object ClearedEvent extends ApplicationEvent

case class StartGameEvent(
  whiteInfo: PlayerInfo,
  blackInfo: PlayerInfo)
    extends ApplicationEvent

case object NextTurnEvent extends ApplicationEvent

case object AIMoveEvent
  extends ApplicationEvent

case class MessageEvent(message: String) extends ApplicationEvent

case object QuitEvent extends ApplicationEvent