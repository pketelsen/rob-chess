package model

sealed abstract class Color {
  def other: Color
}

case object White extends Color {
  def other = Black
}
case object Black extends Color {
  def other = White
}

abstract sealed class Piece

object Piece {
  object Match {
    def unapply(string: String): Option[Piece] = string match {
      case "" => Some(Pawn)
      case "r" => Some(Rook)
      case "n" => Some(Knight)
      case "b" => Some(Bishop)
      case "k" => Some(King)
      case "q" => Some(Queen)
      case _ => None
    }
  }
}

case object Pawn extends Piece {
  override def toString() = ""
}

case object Rook extends Piece {
  override def toString() = "r"
}

case object Knight extends Piece {
  override def toString() = "n"
}

case object Bishop extends Piece {
  override def toString() = "b"
}

case object King extends Piece {
  override def toString() = "k"
}

case object Queen extends Piece {
  override def toString() = "q"
}

case class BoardPos(file: Int, rank: Int) {
  override def toString() = s"${"abcdefgh"(file)}${rank + 1}"
}

object BoardPos {
  object Match {
    object File {
      def unapply(v: Char): Option[Int] =
        v match {
          case 'a' => Some(0)
          case 'b' => Some(1)
          case 'c' => Some(2)
          case 'd' => Some(3)
          case 'e' => Some(4)
          case 'f' => Some(5)
          case 'g' => Some(6)
          case 'h' => Some(7)
          case _ => None
        }

    }

    object Rank {
      def unapply(v: Char): Option[Int] =
        v match {
          case '1' => Some(0)
          case '2' => Some(1)
          case '3' => Some(2)
          case '4' => Some(3)
          case '5' => Some(4)
          case '6' => Some(5)
          case '7' => Some(6)
          case '8' => Some(7)
          case _ => None
        }
    }

    def unapply(string: String): Option[BoardPos] = {
      println(string)
      if (string.length != 2)
        return None

      (string(0), string(1)) match {
        case (File(f), Rank(r)) => Some(BoardPos(f, r))
        case _ => None
      }
    }
  }
}

case class Move(src: BoardPos, dest: BoardPos, promotion: Option[Piece]) {
  override def toString(): String =
    src.toString + dest.toString + promotion.map(_.toString).getOrElse("")
}

object Move {
  object Match {
    def unapply(string: String): Option[Move] = {
      if (string.length < 4 || string.length > 5)
        return None

      (string.substring(0, 2), string.substring(2, 4)) match {
        case (BoardPos.Match(src), BoardPos.Match(dest)) => {
          string.length match {
            case 4 =>
              Some(Move(src, dest, None))

            case 5 => string.substring(4, 5) match {
              case Piece.Match(p) => Some(Move(src, dest, Some(p)))
              case _ => None
            }
          }
        }
        case _ => None
      }
    }
  }
}

case class GameState(moves: Seq[Move])