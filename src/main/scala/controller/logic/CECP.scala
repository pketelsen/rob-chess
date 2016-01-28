package controller.logic

import scala.annotation.tailrec
import scala.collection.mutable.Queue
import scala.sys.process.stringSeqToProcess
import controller.Application
import controller.Player
import model.Bishop
import model.BoardPos
import model.King
import model.Knight
import model.Move
import model.Piece
import model.Queen
import model.Rook
import model.Color
import model.White

/**
 * The Chess Engine Communication Protocol used by gnuchess, crafty, ...
 */
abstract class CECP(val name: String) {
  var nextPing: Int = 0

  val proc = Process(Seq("gnuchess", "--xboard"))
  writeLine("xboard")
  writeLine("protover 2")

  // TODO handle feature flags

  /** Handles asynchronous output of the CECP engine */
  protected def handleLine(line: String): Unit

  protected def readLine(): String = {
    val line = proc.readLine()
    handleLine(line)
    //println(s"Input from $name: $line")
    line
  }

  protected def writeLine(line: String): Unit =
    proc.writeLine(line)

  private def getNextPing(): Int = {
    val p = nextPing
    nextPing = nextPing + 1
    p
  }

  @tailrec
  protected final def await[A](cb: String => Option[A]): A = {
    cb(readLine()) match {
      case Some(ret) => ret
      case None => await(cb)
    }
  }

  protected def sync(cb: String => Unit): Unit = {
    val p = getNextPing()
    writeLine(s"ping $p")

    await { line =>
      if (line != s"pong $p") {
        cb(line)
        None
      } else
        Some(())
    }
  }

  def destroy(): Unit = proc.destroy()

  def attemptMove(move: Move): Boolean = {
    writeLine(move.toString)

    var valid = true

    sync { line =>
      if (CECP.patternInvalidMove.findFirstIn(line).isDefined)
        valid = false
    }

    sync { _ => }

    valid
  }
}

object CECP {
  // TODO Clean up and move to Move.unapply, ... methods
  def file(v: String): Int =
    v match {
      case "a" => 0
      case "b" => 1
      case "c" => 2
      case "d" => 3
      case "e" => 4
      case "f" => 5
      case "g" => 6
      case "h" => 7
    }

  def rank(v: String): Int =
    v match {
      case "1" => 0
      case "2" => 1
      case "3" => 2
      case "4" => 3
      case "5" => 4
      case "6" => 5
      case "7" => 6
      case "8" => 7
    }

  def piece(v: String): Piece =
    v match {
      case "r" => Rook
      case "n" => Knight
      case "b" => Bishop
      case "k" => King
      case "q" => Queen
    }

  val patternInvalidMove = """(?i)^(?:illegal|invalid) move""".r
  val patternAIMove = """(?i)^(?:move|my move is :) (\S+)$""".r

  val patternNormalMove = """^(.)(.)(.)(.)$""".r
  val patternPromotionMove = """^(.)(.)(.)(.)(.)$""".r

  val patternResultWhiteWins = """^1-0 \{(.*)\}$""".r
  val patternResultBlackWins = """^0-1 \{(.*)\}$""".r
  val patternResultDraw = """^1/2-1/2 \{(.*)\}$""".r

  def parseMove(input: String): Move = {
    input.toLowerCase match {
      case CECP.patternNormalMove(srcFile, srcRank, destFile, destRank) =>
        Move(
          BoardPos(CECP.file(srcFile), CECP.rank(srcRank)),
          BoardPos(CECP.file(destFile), CECP.rank(destRank)),
          None)
      case CECP.patternPromotionMove(srcFile, srcRank, destFile, destRank, piece) =>
        Move(
          BoardPos(CECP.file(srcFile), CECP.rank(srcRank)),
          BoardPos(CECP.file(destFile), CECP.rank(destRank)),
          Some(CECP.piece(piece)))
    }
  }
}

class CECPLogic extends CECP("logic") with ChessLogic {
  private var result: Option[Result] = None

  writeLine("force")

  def getResult: Option[Result] = result

  protected def handleLine(line: String): Unit = {
    if (result != None)
      return

    line match {
      case CECP.patternResultWhiteWins(message) =>
        result = Some(ResultWhiteWins(message))
      case CECP.patternResultBlackWins(message) =>
        result = Some(ResultBlackWins(message))
      case CECP.patternResultDraw(message) =>
        result = Some(ResultDraw(message))

      case _ =>
    }
  }
}

class CECPPlayer(val color: Color) extends CECP(color.toString()) with Player {
  val moves = Queue[Move]()

  if (color == White)
    writeLine("go")

  def opponentMove(move: Move): Unit = {
    if (!attemptMove(move))
      throw new RuntimeException("CECP AI player rejected valid move")
  }

  protected def handleLine(line: String): Unit = {
    line match {
      case CECP.patternAIMove(m) =>
        moves += CECP.parseMove(m)
      case _ =>
    }
  }

  @tailrec
  private def waitForMove(): Move = {
    if (moves.isEmpty) {
      readLine()
      waitForMove()
    } else {
      moves.dequeue()
    }
  }

  def getMove(wasInvalid: Boolean): Move = {
    if (wasInvalid)
      throw new RuntimeException("Engine rejected CECP AI player move")

    Application.showMessage("AI is thinking...")

    waitForMove()
  }
}