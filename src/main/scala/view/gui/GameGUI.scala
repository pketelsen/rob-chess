package view.gui

import java.awt
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.io.StdIn

import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.ScrollPaneConstants
import model.BoardPos
import model.Color
import model.Move
import model.Piece
import view.Action
import view.BoardView

class GameGUI extends AbstractGUI with BoardView {
  private val borderWidth = 0.015
  private val fieldSize = (1 - 2 * borderWidth) / 8
  private val svgScale = 550.0

  private val borderColor = new awt.Color(0.3f, 0.3f, 0.3f)
  private val whiteBgColor = new awt.Color(0.8f, 0.8f, 0.8f)
  private val blackBgColor = new awt.Color(0.6f, 0.6f, 0.6f)

  private var gamePanel: JPanel = null
  private var gameHistoryArea: JTextArea = null
  private var statusArea: JTextArea = null

  private var svg: SVG = null

  // To avoid accessing a mutable sequence from the Swing thread
  private var boardData: Seq[(BoardPos, (Color, Piece))] = Seq()

  initialize()

  private def paintBoard(g: Graphics2D): Unit = {
    g.setColor(whiteBgColor)
    g.fill(new Rectangle2D.Double(0, 0, 8, 8))

    g.setColor(blackBgColor)
    (0 until 8).foreach { file =>
      (0 until 8).foreach { rank =>
        if ((file + rank) % 2 == 1) {
          g.fill(new Rectangle2D.Double(file, rank, 1, 1))
        }
      }
    }
  }

  private def paintPiece(g: Graphics2D, file: Int, rank: Int, color: Color, piece: Piece): Unit = {
    val svgPiece = svg.pieces((color, piece))
    val t = g.getTransform

    g.translate(file, 7 - rank)
    g.scale(1 / svgScale, 1 / svgScale)
    g.translate((svgScale - svgPiece.getWidth) / 2, (svgScale - svgPiece.getHeight) / 2)
    svgPiece.render(g)

    g.setTransform(t)
  }

  private def paintPieces(g: Graphics2D): Unit = {
    boardData.foreach {
      case (pos, (color, piece)) =>
        paintPiece(g, pos.file, pos.rank, color, piece)
    }
  }

  private def updateBoardData(): Unit = {
    val newBoardData = Seq((0 until 8).flatMap { file =>
      (0 until 8).map { rank =>
        board.boardState(rank)(file) match {
          case Some((piece, color)) =>
            Some(BoardPos(file, rank), (color, piece))

          case None =>
            None
        }

      }
    }.flatten: _*)

    run {
      boardData = newBoardData

      if (gamePanel != null)
        gamePanel.repaint()
    }
  }

  updateBoardData()

  def setupGUI(frame: JFrame) {
    svg = new SVG()

    frame.setLayout(new GridBagLayout)

    gamePanel = new JPanel(null) {
      override def paint(graphics: Graphics): Unit = {
        val g = graphics.asInstanceOf[Graphics2D]

        g.setRenderingHint(
          RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON)

        val w = getWidth()
        val h = getHeight()
        val size = Math.min(w, h)

        g.translate((w - size) / 2, (h - size) / 2)
        g.scale(size, size)

        g.setColor(borderColor)
        g.fill(new Rectangle2D.Double(0, 0, 1, 1))

        g.translate(borderWidth, borderWidth)
        g.scale(fieldSize, fieldSize)

        paintBoard(g)
        paintPieces(g)

        // Reset transform after painting
        g.setTransform(new AffineTransform)
      }
    }

    gamePanel.setPreferredSize(new Dimension(640, 640))
    gamePanel.setMinimumSize(new Dimension(10, 10))

    val newGameButton = new JButton("New game")

    gameHistoryArea = new JTextArea
    gameHistoryArea.setEditable(false)

    val gameHistoryScrollPane = new JScrollPane(gameHistoryArea)
    gameHistoryScrollPane.setVerticalScrollBarPolicy(
      ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
    gameHistoryScrollPane.setPreferredSize(new Dimension(150, 100))
    gameHistoryScrollPane.setMinimumSize(new Dimension(10, 10))

    statusArea = new JTextArea
    statusArea.setEditable(false)

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
    if (statusArea == null)
      return

    val oldText = statusArea.getText()
    val newText =
      if (oldText == "")
        message
      else
        oldText + "\n" + message

    statusArea.setText(newText)
  }

  def handleActions(l: List[Action]): Future[Unit] = {
    updateBoardData()
    Future.successful(())
  }
}