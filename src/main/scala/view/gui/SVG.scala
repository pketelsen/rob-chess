package view.gui

import com.kitfox.svg.SVGCache
import com.kitfox.svg.SVGDiagram
import com.kitfox.svg.app.beans.SVGIcon

import model.Bishop
import model.Black
import model.Color
import model.King
import model.Knight
import model.Pawn
import model.Piece
import model.Queen
import model.Rook
import model.White

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
    icon.setAntiAlias(true)
    icon.setScaleToFit(true)
    icon
  }
}