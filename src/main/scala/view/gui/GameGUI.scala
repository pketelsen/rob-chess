package view.gui

import java.awt
import java.awt.BasicStroke
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.LayoutManager
import java.awt.RenderingHints
import java.awt.Toolkit
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

import controller.Action
import controller.BoardState
import controller.BoardSubscriber
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JToggleButton
import javax.swing.ScrollPaneConstants
import javax.swing.UIManager
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

class GameGUI extends AbstractGUI with BoardSubscriber {
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

  private var boardState: BoardState = null

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
          (turn != None &&
            boardState(BoardPos(file, rank)).map(_._2) == turn)) {
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
    boardState.state.foreach {
      case (pos, (piece, color)) =>
        paintPiece(g, pos.file, pos.rank, color, piece)
    }
  }

  private def paintSelection(g: Graphics2D): Unit = selected.foreach {
    case BoardPos(file, rank) =>
      g.setStroke(new BasicStroke(0.05f))
      g.setColor(selectionColor)
      g.draw(new Rectangle2D.Double(file, 7 - rank, 1, 1))
  }

  private def updateBoardState(board: BoardState): Unit = {
    run {
      boardState = board

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

  private def updatePromotion(color: Color): Unit = {
    promotionButtons.foreach {
      case (piece, button) =>
        button.setIcon(promotionIcons((color, piece)))

        if (turn == None) {
          button.setEnabled(false)
          button.setSelected(false)
        } else {
          button.setEnabled(true)
          button.setSelected(piece == promotion(color))
        }
    }
  }

  private def createPromotionButton(piece: Piece): JToggleButton = {
    promotionIcons((White, piece)) = svg.getIcon(White, piece)
    promotionIcons((Black, piece)) = svg.getIcon(Black, piece)

    val button = new JToggleButton(promotionIcons((White, piece)))

    button.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent): Unit = {
        turn.foreach { color =>
          promotion(color) = piece
          updatePromotion(color)
        }
      }
    })

    button.setEnabled(false)

    promotionButtons(piece) = button

    button
  }

  def setupGUI(frame: JFrame) {
    svg = new SVG()

    /* Board size: 680x680
     * Width: board width * 5/4
     * Height: board height * 20/17
     */
    frame.setPreferredSize(new Dimension(850, 800))
    frame.setLayout(new GridBagLayout)

    gamePanel = new JPanel(null) {
      override def paint(graphics: Graphics): Unit = {
        if (boardState == null)
          return

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

          case (Some(pos), _) if (boardState(pos).map(_._2) == turn) =>
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

    val newGameButton = new JButton("New game")

    gameHistoryArea = new JTextArea
    gameHistoryArea.setEditable(false)

    val gameHistoryScrollPane = new JScrollPane(gameHistoryArea)
    gameHistoryScrollPane.setVerticalScrollBarPolicy(
      ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)

    val promotionPanel = new JPanel(new GridLayout(1, 0))

    Seq(Queen, Rook, Bishop, Knight).foreach { piece =>
      promotionPanel.add(createPromotionButton(piece))
    }

    statusArea = new JTextArea
    statusArea.setEditable(false)

    val statusScrollPane = new JScrollPane(statusArea)
    statusScrollPane.setVerticalScrollBarPolicy(
      ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)

    gamePanel.setPreferredSize(new Dimension(0, 0))
    frame.add(gamePanel, {
      val c = new GridBagConstraints
      c.fill = GridBagConstraints.BOTH
      c.weightx = 0.8
      c.weighty = 0.85
      c.gridx = 0
      c.gridy = 0
      c
    })

    val sidePanel = new JPanel(new LayoutManager {
      def addLayoutComponent(name: String, comp: Component): Unit = ()
      def removeLayoutComponent(comp: Component): Unit = ()

      def preferredLayoutSize(parent: Container): Dimension =
        new Dimension(0, 0)

      def minimumLayoutSize(parent: Container): Dimension =
        new Dimension(0, 0)

      // Very hacky, but simple
      def layoutContainer(parent: Container) {
        val width = parent.getWidth
        val height = parent.getHeight
        val promotionPanelHeight = width / 4

        val newGameButtonHeight = newGameButton.getPreferredSize.height
        newGameButton.setBounds(0, 0, width, newGameButtonHeight)

        gameHistoryScrollPane.setBounds(0, newGameButtonHeight, width, height - newGameButtonHeight - promotionPanelHeight)

        promotionPanel.setBounds(0, height - promotionPanelHeight, width, promotionPanelHeight)

        promotionIcons.values.foreach {
          _.setPreferredSize(new Dimension(promotionPanelHeight * 2 / 3, promotionPanelHeight * 2 / 3))
        }
      }
    })

    sidePanel.setPreferredSize(new Dimension(0, 0))
    frame.add(sidePanel, {
      val c = new GridBagConstraints
      c.fill = GridBagConstraints.BOTH
      c.weightx = 0.2
      c.weighty = 0.15
      c.gridx = 1
      c.gridy = 0
      c
    })

    sidePanel.add(newGameButton)
    sidePanel.add(gameHistoryScrollPane)
    sidePanel.add(promotionPanel)

    statusScrollPane.setPreferredSize(new Dimension(0, 0))
    frame.add(statusScrollPane, {
      val c = new GridBagConstraints
      c.fill = GridBagConstraints.BOTH
      c.weightx = 1
      c.weighty = 0.15
      c.gridx = 0
      c.gridy = 1
      c.gridwidth = 2
      c
    })

    frame.pack()
    frame.setVisible(true)
  }

  def getMove(color: Color): List[Move] = {
    run {
      turn = Some(color)
      updatePromotion(color)
    }

    moveChannel.read
  }

  def acceptMove(): Unit = {
    run {
      selected = None
    }
  }

  def AIMove(color: Color): Unit = {
    run {
      updatePromotion(color)
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

  def resetBoard(board: BoardState): Unit = {
    updateBoardState(board)
  }

  def handleActions(l: List[Action], board: BoardState): Future[Unit] = {
    updateBoardState(board)
    Future.successful(())
  }
}

object GameGUI {
  def initUI(): Unit = {
    System.setProperty("awt.useSystemAAFontSettings", "on")
    System.setProperty("swing.aatext", "true")

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
