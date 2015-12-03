package controller

case class Host(hostname: String, port: Int)
case class PlayerInfo(ai: Boolean)

abstract sealed class ApplicationEvent

case class StartGameEvent(
  robot: Host,
  tracker: Host,
  whiteInfo: PlayerInfo,
  blackInfo: PlayerInfo)
    extends ApplicationEvent
    
case class MessageEvent(message: String) extends ApplicationEvent

object QuitEvent extends ApplicationEvent