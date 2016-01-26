package controller

import controller.logic.Result

case class Host(hostname: String, port: Int)
case class PlayerInfo(ai: Boolean)

abstract sealed class ApplicationEvent

case class StartGameEvent(
  whiteInfo: PlayerInfo,
  blackInfo: PlayerInfo)
    extends ApplicationEvent

object NextTurnEvent extends ApplicationEvent

case class EndGameEvent(result: Result)
  extends ApplicationEvent

case class StartCalibrationEvent(
  robot: Host,
  tracking: Host)
    extends ApplicationEvent

object QuitEvent extends ApplicationEvent