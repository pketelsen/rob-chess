package view.gui

import java.awt
import java.awt.BasicStroke
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.RenderingHints
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D

import scala.collection.mutable
import scala.concurrent.Channel
import scala.concurrent.Future

import com.kitfox.svg.app.beans.SVGIcon

import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JToggleButton
import javax.swing.ScrollPaneConstants
import model.Bishop
import model.Black
import model.BoardPos
import model.Color
import model.Knight
import model.Move
import model.Piece
import model.Queen
import model.Rook
import model.White
import view.Action
import view.BoardView

class GameGUI extends AbstractGUI with BoardView {
  private val borderWidth = 0.015
  private val fieldSize = (1 - 2 * borderWidth) / 8
  private val svgScale = 550.0

  private val borderColor = new awt.Color(0.3f, 0.3f, 0.3f)
  private val whiteBgColor = new awt.Color(0.8f, 0.8f, 0.8f)
  private val blackBgColor = new awt.Color(0.6f, 0.6f, 0.6f)
  private val selectionColor = new awt.Color(1.0f, 1.0f, 1.0f)
  private val hoverColor = new awt.Color(1.0f, 1.0f, 1.0f, 0.15f)

  private var gamePanel: JPanel = null
  private var gameHistoryArea: JTextArea = null
  private var statusArea: JTextArea = null

  private val promotionIcons = mutable.Map[(Color, Piece), SVGIcon]()
  private val promotionButtons = mutable.Map[Piece, JToggleButton]()

  private var svg: SVG = null

  private var turn: Option[Color] = None
  private var hover: Option[BoardPos] = None
  private var selected: Option[BoardPos] = None
  private val promotion = mutable.Map[Color, Piece](White -> Queen, Black -> Queen)
  private val moveChannel = new Channel[List[Move]]

  // To avoid accessing a mutable sequence from the Swing thread
  private var boardData: Map[BoardPos, (Color, Piece)] = Map()

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

    hover.foreach {
      case BoardPos(file, rank) =>
        if (selected.isDefined ||
          boardData.get(BoardPos(file, rank)).map(_._1) == turn) {
          g.setColor(hoverColor)
          g.fill(new Rectangle2D.Double(file, 7 - rank, 1, 1))
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

  private def paintSelection(g: Graphics2D): Unit = selected.foreach {
    case BoardPos(file, rank) =>
      g.setStroke(new BasicStroke(0.05f))
      g.setColor(selectionColor)
      g.draw(new Rectangle2D.Double(file, 7 - rank, 1, 1))
  }

  private def updateBoardData(): Unit = {
    val newBoardData = (0 until 8).flatMap { file =>
      (0 until 8).map { rank =>
        board.boardState(rank)(file) match {
          case Some((piece, color)) =>
            Some(BoardPos(file, rank), (color, piece))

          case None =>
            None
        }

      }
    }.flatten.toMap

    run {
      boardData = newBoardData

      if (gamePanel != null)
        gamePanel.repaint()
    }
  }

  private def getBoardPos(x: Double, y: Double): Option[BoardPos] = {
    val w = gamePanel.getWidth()
    val h = gamePanel.getHeight()
    val size = Math.min(w, h)

    val xt = (x - (w - size) / 2) / size
    val yt = (y - (h - size) / 2) / size

    val file = (xt - borderWidth) / fieldSize
    val rank = (yt - borderWidth) / fieldSize

    if (file < 0 || file >= 8 || rank < 0 || rank >= 8)
      None
    else
      Some(BoardPos(file.toInt, 7 - rank.toInt))
  }

  private def updatePromotion(): Unit = {
    promotionButtons.foreach {
      case (piece, button) =>
        turn match {
          case Some(color) =>
            button.setEnabled(true)
            button.setIcon(promotionIcons((color, piece)))
            button.setSelected(piece == promotion(color))

          case None =>
            button.setEnabled(false)
        }
    }
  }

  private def createPromotionButton(piece: Piece): JToggleButton = {
    promotionIcons((White, piece)) = svg.getIcon(White, piece)
    promotionIcons((Black, piece)) = svg.getIcon(Black, piece)

    val button = new JToggleButton(promotionIcons((White, piece)))

    button.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent): Unit = {
        turn.foreach(promotion(_) = piece)
        updatePromotion()
      }
    })

    button.setEnabled(false)

    promotionButtons(piece) = button

    button
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
        paintSelection(g)

        // Reset transform after painting
        g.setTransform(new AffineTransform)
      }
    }

    gamePanel.addMouseListener(new MouseAdapter {
      override def mouseClicked(e: MouseEvent): Unit = {
        if (turn == None)
          return

        (getBoardPos(e.getX, e.getY), selected) match {
          case (Some(pos), Some(sel)) if (sel == pos) =>
            selected = None

          case (Some(pos), _) if (boardData.get(pos).map(_._1) == turn) =>
            selected = Some(pos)

          case (Some(pos), Some(sel)) =>
            moveChannel.write(List(
              Move(sel, pos, promotion.get(turn.get)),
              Move(sel, pos, None)))
            turn = None

          case _ =>
        }

        gamePanel.repaint()
      }
    })

    gamePanel.addMouseMotionListener(new MouseMotionAdapter {
      override def mouseMoved(e: MouseEvent): Unit = {
        val oldHover = hover
        hover = getBoardPos(e.getX, e.getY)

        if (hover != oldHover)
          gamePanel.repaint()
      }
    })

    gamePanel.setPreferredSize(new Dimension(640, 640))
    gamePanel.setMinimumSize(new Dimension(10, 10))

    val newGameButton = new JButton("New game")

    gameHistoryArea = new JTextArea
    gameHistoryArea.setEditable(false)

    val gameHistoryScrollPane = new JScrollPane(gameHistoryArea)
    gameHistoryScrollPane.setVerticalScrollBarPolicy(
      ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
    gameHistoryScrollPane.setPreferredSize(new Dimension(150, 100))
    gameHistoryScrollPane.setMinimumSize(new Dimension(150, 100))

    val promotionPanel = new JPanel(new GridLayout(1, 0))
    promotionPanel.setPreferredSize(new Dimension(64, 64))
    promotionPanel.setMinimumSize(new Dimension(64, 64))

    promotionPanel.add(createPromotionButton(Queen))
    promotionPanel.add(createPromotionButton(Rook))
    promotionPanel.add(createPromotionButton(Bishop))
    promotionPanel.add(createPromotionButton(Knight))

    updatePromotion()

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
      c.gridheight = 3
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
      c.weighty = 1
      c.gridx = 1
      c.gridy = 1
      c
    })

    frame.add(promotionPanel, {
      val c = new GridBagConstraints
      c.fill = GridBagConstraints.BOTH
      c.gridx = 1
      c.gridy = 2
      c
    })

    frame.add(statusScrollPane, {
      val c = new GridBagConstraints
      c.fill = GridBagConstraints.BOTH
      c.weighty = 0.1
      c.gridx = 0
      c.gridy = 3
      c.gridwidth = 2
      c
    })

    frame.pack()
    frame.setVisible(true)
  }

  def getMove(color: Color): List[Move] = {
    run {
      turn = Some(color)
      updatePromotion()
    }

    moveChannel.read
  }

  def acceptMove(): Unit = {
    run {
      selected = None
    }
  }

  def AIMove(): Unit = {
    run {
      updatePromotion()
    }
  }

  def abortMove(): Unit = {
    /* This will kill the game will a NullPointerException
     * (which is caught by the Game's ExecutionContext)
     */
    moveChannel.write(null)
  }

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