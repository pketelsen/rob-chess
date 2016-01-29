package view.gui

import java.awt
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.geom.AffineTransform
import scala.annotation.tailrec
import scala.concurrent.Future
import scala.io.StdIn
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.ScrollPaneConstants
import model.Move
import view.Action
import view.BoardView
import java.awt.geom.Rectangle2D
import javax.swing.SwingUtilities

class GameGUI extends AbstractGUI with BoardView {
  private val borderWidth = 0.015
  private val fieldSize = (1 - 2 * borderWidth) / 8

  private val borderColor = new awt.Color(0.3f, 0.3f, 0.3f)
  private val whiteBgColor = new awt.Color(0.8f, 0.8f, 0.8f)
  private val blackBgColor = new awt.Color(0.6f, 0.6f, 0.6f)

  private var gameHistoryArea: Option[JTextArea] = None
  private var statusArea: Option[JTextArea] = None

  initialize()

  private def paintBoard(g: Graphics2D): Unit = {
    g.setColor(borderColor)
    g.fill(new Rectangle2D.Double(0, 0, 1, 1))

    g.setColor(whiteBgColor)
    g.fill(new Rectangle2D.Double(borderWidth, borderWidth, 1 - 2 * borderWidth, 1 - 2 * borderWidth))

    g.setColor(blackBgColor)
    (0 until 8).foreach { x =>
      (0 until 8).foreach { y =>
        if ((x + y) % 2 == 1) {
          val dx = borderWidth + x * fieldSize
          val dy = borderWidth + y * fieldSize
          g.fill(new Rectangle2D.Double(dx, dy, fieldSize, fieldSize))
        }
      }
    }
  }

  def setupGUI(frame: JFrame) {
    frame.setLayout(new GridBagLayout)

    val gamePanel = new JPanel {
      override def paint(graphics: Graphics): Unit = {
        val g = graphics.asInstanceOf[Graphics2D]

        val w = getWidth()
        val h = getHeight()
        val size = Math.min(w, h)

        g.translate((w - size) / 2, (h - size) / 2)
        g.scale(size, size)

        paintBoard(g)

        // Reset transform after painting
        g.setTransform(new AffineTransform)
      }
    }

    gamePanel.setPreferredSize(new Dimension(640, 640))
    gamePanel.setMinimumSize(new Dimension(10, 10))

    val newGameButton = new JButton("New game")

    val gameHistoryArea = new JTextArea
    gameHistoryArea.setEditable(false)
    this.gameHistoryArea = Some(gameHistoryArea)

    val gameHistoryScrollPane = new JScrollPane(gameHistoryArea)
    gameHistoryScrollPane.setVerticalScrollBarPolicy(
      ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
    gameHistoryScrollPane.setPreferredSize(new Dimension(150, 100))
    gameHistoryScrollPane.setMinimumSize(new Dimension(10, 10))

    val statusArea = new JTextArea
    statusArea.setEditable(false)
    this.statusArea = Some(statusArea)

    val statusScrollPane = new JScrollPane(statusArea)
    statusScrollPane.setVerticalScrollBarPolicy(
      ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
    statusScrollPane.setPreferredSize(new Dimension(150, 100))
    statusScrollPane.setMinimumSize(new Dimension(10, 10))

    frame.add(gamePanel, {
      val c = new GridBagConstraints
      c.fill = GridBagConstraints.BOTH
      c.weightx = 0.9
      c.weighty = 0.9
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
      c.weightx = 0.1
      c.gridx = 1
      c.gridy = 1
      c
    })

    frame.add(statusScrollPane, {
      val c = new GridBagConstraints
      c.fill = GridBagConstraints.BOTH
      c.weighty = 0.1
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

  def showMessage(message: String): Unit = run {
    statusArea match {
      case Some(status) =>
        val oldText = status.getText()
        val newText =
          if (oldText == "")
            message
          else
            oldText + "\n" + message

        status.setText(newText)

      case None =>
    }
  }

  def handleActions(l: List[Action]): Future[Unit] = {
    Future.successful(())
  }
}