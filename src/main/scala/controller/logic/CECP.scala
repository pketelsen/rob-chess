package controller.logic

import model.Move
import scala.annotation.tailrec

/**
 * The Cess Engine Communication Protocol used by gnuchess, crafty, ...
 */
class CECP {
  var nextPing: Int = 0

  val proc = Process(Seq("gnuchess", "--xboard"))
  println("GNUChess initialized: " + proc.readLine())
  writeLine("xboard")
  writeLine("protover 2")

  // TODO handle feature flags

  def readLine(): String = proc.readLine()
  def writeLine(line: String): Unit = proc.writeLine(line)

  private def getNextPing(): Int = {
    val p = nextPing
    nextPing = nextPing + 1
    p
  }

  @tailrec
  private def doWait(cb: String => Unit, p: Int): Unit = {
    val s = readLine()
    if (s != s"pong $p") {
      cb(s)
      doWait(cb, p)
    }
  }

  def wait(cb: String => Unit): Unit = {
    val p = getNextPing()
    writeLine(s"ping $p")
    doWait(cb, p)
  }

  def destroy(): Unit = proc.destroy()
}

class CECPLogic extends CECP with ChessLogic {
  val patternInvalidMove = "^Invalid move: ".r

  writeLine("force")

  def attemptMove(move: Move): Boolean = {
    writeLine(move.toString)

    var valid = true

    wait { line =>
      if (patternInvalidMove.findFirstMatchIn(line).isDefined)
        valid = false
    }

    valid
  }
}