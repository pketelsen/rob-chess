package view.gui

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.io.StdIn

import javax.swing.JFrame
import model.Move
import view.Action
import view.BoardView

class GameGUI extends AbstractGUI with BoardView {
  def setupGUI(frame: JFrame) {
  }

  @tailrec
  private def askForMove(): Move = {
    println("Please enter move: ")
    StdIn.readLine() match {
      case Move.Match(move) => move
      case _ =>
        println("I don't understand")
        askForMove()
    }
  }

  def getMove(): Move = askForMove()

  def showMessage(message: String): Unit =
    println(message)

  def handleActions(l: List[Action]): Future[Unit] = {
    Future.successful(())
  }
}