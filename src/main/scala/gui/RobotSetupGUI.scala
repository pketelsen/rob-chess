package gui

import controller.Application
import controller.RobotSetupEvent

class RobotSetupGUI {
  Application.queueEvent(RobotSetupEvent(None))
}