package view.gui

import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import controller.Application
import controller.QuitEvent
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

abstract class AbstractGUI[T <: JFrame](initializeFrame: => T) {
  private def invoke[R](f: => R): R = {
    var ret: Option[R] = None

    SwingUtilities.invokeAndWait(
      new Runnable() {
        def run() {
          ret = Some(f)
        }
      })

    ret.get
  }

  private val frame: T = invoke(initializeFrame)

  def run[R](f: T => R): R = invoke {
    f(frame)
  }
}

abstract class AbstractGUIFrame(subtitle: String = "") extends JFrame("rob-chess" + subtitle) {
  setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE)

  addWindowListener(new WindowAdapter {
    override def windowClosing(e: WindowEvent): Unit = {
      Application.queueEvent(QuitEvent)
      dispose()
    }
  })
}