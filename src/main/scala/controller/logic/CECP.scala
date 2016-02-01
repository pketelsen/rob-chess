package controller.logic

import java.io.IOException
import scala.annotation.tailrec
import scala.collection.mutable.Queue
import scala.sys.process.stringSeqToProcess
import controller.AIMoveEvent
import controller.Application
import controller.MoveAbortedException
import controller.Player
import model.Black
import model.Color
import model.Move
import model.White
import controller.Debug

/**
 * The Chess Engine Communication Protocol used by gnuchess, crafty, ...
 */
abstract class CECP(val name: String) {
  val debug = new Debug(name)

  var nextPing: Int = 0

  val proc = Process(Seq("gnuchess", "--xboard"))
  writeLine("xboard")
  writeLine("protover 2")

  // TODO handle feature flags

  /** Handles asynchronous output of the CECP engine */
  protected def handleLine(line: String): Unit

  protected def readLine(): String = {
    val line = try {
      proc.readLine()
    } catch {
      case _: IOException =>
        throw new MoveAbortedException
    }

    debug.log("<<< " + line)

    handleLine(line)
    line
  }

  protected def writeLine(line: String): Unit = {
    debug.log(">>> " + line)

    try {
      proc.writeLine(line)
    } catch {
      case _: IOException =>
        throw new MoveAbortedException
    }
  }

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

    def waitForPong(line: String): Option[Unit] = {
      if (line != s"pong $p") {
        cb(line)
        None
      } else
        Some(())
    }

    writeLine(s"ping $p")

    await(waitForPong)
    await(waitForPong)
  }

  def destroy(): Unit = {
    proc.destroy()
    debug.close()
  }

  protected def sendMove(move: Move): Unit = writeLine(move.toString)
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
        result = Some(ResultWin(White, message))
      case CECP.patternResultBlackWins(message) =>
        result = Some(ResultWin(Black, message))
      case CECP.patternResultDraw(message) =>
        result = Some(ResultDraw(message))

      case _ =>
    }
  }

  def attemptMove(move: Move): Boolean = {
    writeLine(move.toString)

    var valid = true

    sync { line =>
      if (CECP.patternInvalidMove.findFirstIn(line).isDefined)
        valid = false
    }

    valid
  }

}

class CECPPlayer(val color: Color) extends CECP(color.toString()) with Player {
  val moves = Queue[Move]()

  if (color == White)
    writeLine("go")

  def opponentMove(move: Move): Unit = sendMove(move)

  def acceptMove(result: Option[Result]): Unit = ()

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

    Application.showMessage(s"${color} to move. AI is thinking...")
    Application.queueEvent(AIMoveEvent)

    List(waitForMove())
  }
}