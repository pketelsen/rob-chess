package view.gui

import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import scala.annotation.tailrec
import scala.concurrent.Future
import scala.io.StdIn
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JTextArea
import model.Move
import view.Action
import view.BoardView
import javax.swing.JScrollPane
import java.awt.Dimension
import javax.swing.ScrollPaneConstants

class GameGUI extends AbstractGUI with BoardView {
  def setupGUI(frame: JFrame) {
    frame.setLayout(new GridBagLayout)

    val gamePanel = new JPanel
    gamePanel.setPreferredSize(new Dimension(640, 640))
    gamePanel.setMinimumSize(new Dimension(10, 10))

    val newGameButton = new JButton("New game")

    val gameHistoryArea = new JTextArea
    gameHistoryArea.setEditable(false)
    val gameHistoryScrollPane = new JScrollPane(gameHistoryArea)
    gameHistoryScrollPane.setVerticalScrollBarPolicy(
      ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
    gameHistoryScrollPane.setPreferredSize(new Dimension(150, 100))
    gameHistoryScrollPane.setMinimumSize(new Dimension(10, 10))

    val statusArea = new JTextArea
    statusArea.setEditable(false)

    val statusScrollPane = new JScrollPane(statusArea)
    statusScrollPane.setVerticalScrollBarPolicy(
      ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
    statusScrollPane.setPreferredSize(new Dimension(150, 100))
    statusScrollPane.setMinimumSize(new Dimension(10, 10))

    frame.add(gamePanel, {
      val c = new GridBagConstraints
      c.fill = GridBagConstraints.BOTH
      c.weightx = 1
      c.weighty = 1
      c.gridx = 0
      c.gridy = 0
      c.gridheight = 2
      c
    })

    frame.add(newGameButton, {
      val c = new GridBagConstraints
      c.fill = GridBagConstraints.BOTH
      c.gridx = 1
      c.gridy = 0
      c
    })

    frame.add(gameHistoryScrollPane, {
      val c = new GridBagConstraints
      c.fill = GridBagConstraints.BOTH
      c.gridx = 1
      c.gridy = 1
      c
    })

    frame.add(statusScrollPane, {
      val c = new GridBagConstraints
      c.fill = GridBagConstraints.BOTH
      c.gridx = 0
      c.gridy = 2
      c.gridwidth = 2
      c
    })

    frame.pack()
    frame.setVisible(true)
  }

  @tailrec
  private def askForMove(): Move = {
    println("Please enter move: ")
    StdIn.readLine() match {
      case Move.Match(move) => move
      case _ =>
        println("I don't understand")
        askForMove()
    }
  }

  def getMove(): Move = askForMove()

  def showMessage(message: String): Unit =
    println(message)

  def handleActions(l: List[Action]): Future[Unit] = {
    Future.successful(())
  }
}