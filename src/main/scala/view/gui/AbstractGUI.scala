package view.gui

import java.awt.Window
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import controller.Application
import controller.QuitEvent
import javax.swing.SwingUtilities

abstract class AbstractGUI[T <: Window](initializeWindow: => T) {
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

  val window: T = invoke(initializeWindow)

  SwingUtilities.invokeLater(new Runnable {
    def run() {
      window.setVisible(true)
    }
  })

  def run[R](f: T => R): R = invoke {
    f(window)
  }

  def dispose(): Unit = run(_.dispose())
}

abstract trait AbstractGUIWindow extends Window {
  addWindowListener(new WindowAdapter {
    override def windowClosing(e: WindowEvent): Unit = {
      Application.queueEvent(QuitEvent)
      dispose()
    }
  })
}