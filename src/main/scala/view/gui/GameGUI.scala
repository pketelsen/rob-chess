package view.gui

import java.awt
import java.awt.BasicStroke
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.LayoutManager
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

import controller.Action
import controller.Application
import controller.BoardState
import controller.BoardSubscriber
import controller.NewGameEvent
import controller.logic.Result
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JToggleButton
import javax.swing.ScrollPaneConstants
import javax.swing.WindowConstants
import javax.swing.border.EmptyBorder
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

class GameGUI extends AbstractGUI[GameGUIFrame](new GameGUIFrame) with BoardSubscriber {
  private def getMoveChannel = run(_.getMoveChannel)

  def reset(): Future[Unit] = {
    run(_.reset())
    Future.successful(())
  }

  def showMessage(message: String): Unit = run(_.showMessage(message))
  def AIMove(color: Color): Unit = run(_.setTurn(color, true))

  def handleMove(l: List[Action], color: Color, move: String, board: BoardState): Future[Unit] = {
    run { frame =>
      frame.addGameHistory(move, color)
      frame.updateBoardState(board)
    }

    Future.successful(())
  }

  def getMove(color: Color): Option[List[Move]] = {
    run(_.setTurn(color, false))
    getMoveChannel.read
  }

  def acceptMove(result: Option[Result]): Unit = run(_.acceptMove(result))

  def abortMove(): Unit = {
    getMoveChannel.write(None)
  }
}

class GameGUIFrame extends JFrame("rob-chess") with AbstractGUIWindow {
  private var turn: Option[Color] = None
  private var hover: Option[BoardPos] = None
  private var selected: Option[BoardPos] = None
  private val promotion = mutable.Map[Color, Piece](White -> Queen, Black -> Queen)
  private var moveChannel = new Channel[Option[List[Move]]]

  private var boardState: BoardState = BoardState()

  private var counter = 1

  def reset(): Unit = {
    turn = None
    selected = None

    promotion(White) = Queen
    promotion(Black) = Queen

    moveChannel = new Channel[Option[List[Move]]]

    counter = 1

    statusLabel.setText(" ")
    gameHistoryArea.setText("")

    updateBoardState(BoardState())
  }

  class GamePanel extends JPanel(null) {
    private val borderWidth = 0.015
    private val fieldSize = (1 - 2 * borderWidth) / 8
    private val svgScale = 550.0

    private val borderColor = new awt.Color(0.3f, 0.3f, 0.3f)
    private val whiteBgColor = new awt.Color(0.8f, 0.8f, 0.8f)
    private val blackBgColor = new awt.Color(0.6f, 0.6f, 0.6f)
    private val selectionColor = new awt.Color(1.0f, 1.0f, 1.0f)
    private val hoverColor = new awt.Color(1.0f, 1.0f, 1.0f, 0.15f)

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

    addMouseListener(new MouseAdapter {
      override def mouseClicked(e: MouseEvent): Unit = {
        if (turn == None)
          return

        (getBoardPos(e.getX, e.getY), selected) match {
          case (Some(pos), Some(sel)) if (sel == pos) =>
            selected = None

          case (Some(pos), _) if (boardState(pos).map(_._2) == turn) =>
            selected = Some(pos)

          case (Some(pos), Some(sel)) =>
            moveChannel.write(Some(List(
              Move(sel, pos, promotion.get(turn.get)),
              Move(sel, pos, None))))
            turn = None

          case _ =>
        }

        repaint()
      }
    })

    addMouseMotionListener(new MouseMotionAdapter {
      override def mouseMoved(e: MouseEvent): Unit = {
        val oldHover = hover
        hover = getBoardPos(e.getX, e.getY)

        if (hover != oldHover)
          repaint()
      }
    })

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
  }

  private val svg = new SVG()

  private val gamePanel = new GamePanel
  private val gameHistoryArea = new JTextArea
  private val statusLabel = new JLabel(" ") // Non-empty content to get correct height

  private val (promotionIcons, promotionButtons) = createPromotionButtons()

  setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE)

  {
    val newGameButton = new JButton("New game")
    newGameButton.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent): Unit = {
        Application.queueEvent(NewGameEvent)
      }
    })

    gameHistoryArea.setEditable(false)

    val gameHistoryScrollPane = new JScrollPane(gameHistoryArea)
    gameHistoryScrollPane.setVerticalScrollBarPolicy(
      ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)

    val promotionPanel = new JPanel(new GridLayout(1, 0))

    promotionButtons.foreach {
      case (_, button) => promotionPanel.add(button)
    }

    statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 20))
    statusLabel.setBorder(new EmptyBorder(5, 10, 5, 10))

    setLayout(new GridBagLayout)

    gamePanel.setPreferredSize(new Dimension(0, 0))
    add(gamePanel, {
      val c = new GridBagConstraints
      c.fill = GridBagConstraints.BOTH
      c.weightx = 0.8
      c.weighty = 1
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
    add(sidePanel, {
      val c = new GridBagConstraints
      c.fill = GridBagConstraints.BOTH
      c.weightx = 0.2
      c.weighty = 1
      c.gridx = 1
      c.gridy = 0
      c
    })

    sidePanel.add(newGameButton)
    sidePanel.add(gameHistoryScrollPane)
    sidePanel.add(promotionPanel)

    add(statusLabel, {
      val c = new GridBagConstraints
      c.fill = GridBagConstraints.BOTH
      c.weightx = 1
      c.weighty = 0
      c.gridx = 0
      c.gridy = 1
      c.gridwidth = 2
      c
    })

    pack()

    val statusLabelHeight = statusLabel.getPreferredSize.height

    statusLabel.setPreferredSize(new Dimension(0, statusLabelHeight))

    /* Board size: 680x680
     * Width: board width * 5/4
     * Height: board height + status label height
     */
    setSize(new Dimension(850, 680 + statusLabelHeight))
  }

  private def createPromotionButton(piece: Piece): (SVGIcon, SVGIcon, JToggleButton) = {
    val whiteIcon = svg.getIcon(White, piece)
    val blackIcon = svg.getIcon(Black, piece)

    val button = new JToggleButton(whiteIcon)

    button.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent): Unit = {
        turn.foreach { color =>
          promotion(color) = piece
          updatePromotion(None)
        }
      }
    })

    button.setEnabled(false)

    (whiteIcon, blackIcon, button)
  }

  private def createPromotionButtons(): (Map[(Color, Piece), SVGIcon], Seq[(Piece, JToggleButton)]) = {
    val (whiteIcons: Seq[((Color, Piece), SVGIcon)], blackIcons: Seq[((Color, Piece), SVGIcon)], buttons) =
      Seq(Queen, Rook, Bishop, Knight).map { piece =>
        val (whiteIcon, blackIcon, button) = createPromotionButton(piece)
        (
          (White, piece) -> whiteIcon,
          (Black, piece) -> blackIcon,
          piece -> button)
      }.unzip3

    ((whiteIcons ++ blackIcons).toMap, buttons)
  }

  def updateBoardState(board: BoardState): Unit = {
    boardState = board
    gamePanel.repaint()
  }

  private def updatePromotion(color: Option[Color]): Unit = {
    promotionButtons.foreach {
      case (piece, button) =>
        color.foreach { c =>
          button.setIcon(promotionIcons((c, piece)))
        }

        turn match {
          case Some(c) =>
            button.setEnabled(true)
            button.setSelected(piece == promotion(c))

          case None =>
            button.setEnabled(false)
            button.setSelected(false)
        }
    }
  }

  def setTurn(color: Color, ai: Boolean): Unit = {
    if (!ai)
      turn = Some(color)

    updatePromotion(Some(color))
  }

  def acceptMove(result: Option[Result]): Unit = {
    selected = None

    if (result.isDefined)
      updatePromotion(None)
  }

  def addGameHistory(move: String, color: Color): Unit = {
    if (gameHistoryArea.getText != "" && color == White)
      gameHistoryArea.append("\n")

    gameHistoryArea.append(
      color match {
        case White =>
          s"${counter}. ${move}"

        case Black =>
          counter = counter + 1
          s"\t${move}"
      })
  }

  def getMoveChannel: Channel[Option[List[Move]]] =
    moveChannel

  def showMessage(message: String): Unit = {
    statusLabel.setText(message)
  }
}
