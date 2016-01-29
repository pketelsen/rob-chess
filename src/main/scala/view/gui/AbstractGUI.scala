package view.gui

import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import controller.Application
import controller.QuitEvent
import javax.swing.JFrame
import javax.swing.WindowConstants

abstract class AbstractGUI(subtitle: String = "") extends JFrame("rob-chess" + subtitle) {
  setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE)

  addWindowListener(new WindowAdapter {
    override def windowClosing(e: WindowEvent): Unit = {
      Application.queueEvent(QuitEvent)
      dispose()
    }
  })

}