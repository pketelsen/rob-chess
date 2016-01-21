package controller.logic

import model.Move

/**
 * The Cess Engine Communication Protocol used by gnuchess, crafty, ...
 */
class CECP {
  val proc = Process(Seq("gnuchess", "--xboard"))
  println("GNUChess initialized: " + proc.readLine())
  writeLine("xboard")

  def readLine(): String = proc.readLine()
  def writeLine(line: String): Unit = proc.writeLine(line)

  def destroy(): Unit = proc.destroy()
}

class CECPLogic extends CECP with ChessLogic {
  writeLine("force")

  def attemptMove(move: Move): Boolean = {
    false
  }
}