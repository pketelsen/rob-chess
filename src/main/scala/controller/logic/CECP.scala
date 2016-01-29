package controller.logic

import scala.annotation.tailrec
import scala.collection.mutable.Queue
import scala.sys.process.stringSeqToProcess

import controller.AIMoveEvent
import controller.Application
import controller.Player
import model.Color
import model.Move
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
  val patternInvalidMove = """(?i)^(?:illegal|invalid) move""".r
  val patternAIMove = """(?i)^(?:move|my move is :) (\S+)$""".r

  val patternResultWhiteWins = """^1-0 \{(.*)\}$""".r
  val patternResultBlackWins = """^0-1 \{(.*)\}$""".r
  val patternResultDraw = """^1/2-1/2 \{(.*)\}$""".r
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

  def acceptMove(): Unit = ()

  protected def handleLine(line: String): Unit = {
    line match {
      case CECP.patternAIMove(m) =>
        m match {
          case Move.Match(move) =>
            moves += move

          case _ =>
            throw new RuntimeException("CECP AI player made unparsable move")
        }
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

  def getMove(wasInvalid: Boolean): List[Move] = {
    if (wasInvalid)
      throw new RuntimeException("Engine rejected CECP AI player move")

    Application.showMessage("AI is thinking...")
    Application.queueEvent(AIMoveEvent)

    List(waitForMove())
  }
}