package view.gui

import java.awt.Toolkit

import javax.swing.SwingUtilities
import javax.swing.UIManager

object GUI extends Runnable {
  def init(): Unit = {
    System.setProperty("awt.useSystemAAFontSettings", "on")
    System.setProperty("swing.aatext", "true")

    SwingUtilities.invokeAndWait(this)
  }

  def run(): Unit = {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch {
      case (e: Exception) =>
    }

    val tk = Toolkit.getDefaultToolkit()

    try {
      val awtAppClassNameField =
        tk.getClass().getDeclaredField("awtAppClassName")
      awtAppClassNameField.setAccessible(true)
      awtAppClassNameField.set(tk, "rob-chess")
    } catch {
      case (e: Exception) =>
    }
  }
}