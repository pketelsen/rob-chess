package view.gui

import com.kitfox.svg.SVGCache
import model.Color
import model.Piece
import model.Pawn
import model.White
import model.Black
import model.Knight
import model.King
import model.Bishop
import model.Queen
import model.Rook
import com.kitfox.svg.SVGDiagram
import com.kitfox.svg.app.beans.SVGIcon
import javax.swing.Icon
import java.awt.Dimension

class SVG {
  private val universe = SVGCache.getSVGUniverse

  private def name(color: Color, piece: Piece): String = {
    val c = color match {
      case White => "w"
      case Black => "b"
    }

    val p = piece match {
      case Pawn => "pawn"
      case Rook => "rook"
      case Knight => "knight"
      case Bishop => "bishop"
      case Queen => "queen"
      case King => "king"
    }

    s"${p}_${c}"
  }

  val pieces: Map[(Color, Piece), SVGDiagram] = {
    (Seq(White, Black) flatMap { color =>
      Seq(Pawn, Rook, Knight, Bishop, Queen, King) map { piece =>
        val n = name(color, piece)
        val diag = universe.getDiagram(universe.loadSVG(getClass.getResourceAsStream(s"/${n}.svg"), n))
        ((color, piece), diag)
      }
    }).toMap
  }

  def getIcon(color: Color, piece: Piece): SVGIcon = {
    val icon = new SVGIcon
    icon.setSvgUniverse(universe)
    icon.setSvgURI(universe.getStreamBuiltURI(name(color, piece)))
    icon.setScaleToFit(true)
    icon.setAntiAlias(true)
    icon.setPreferredSize(new Dimension(32, 32))
    icon
  }
}