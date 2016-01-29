package view.gui

import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import controller.Application
import controller.QuitEvent
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

abstract class AbstractGUI(subtitle: String = "") {
  SwingUtilities.invokeAndWait(
    new Runnable() {
      def run() {
        val frame = new JFrame("rob-chess" + subtitle)
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE)

        frame.addWindowListener(new WindowAdapter {
          override def windowClosing(e: WindowEvent): Unit = {
            Application.queueEvent(QuitEvent)
            frame.dispose()
          }
        })

        setupGUI(frame)
      }
    })

  def setupGUI(frame: JFrame): Unit
}