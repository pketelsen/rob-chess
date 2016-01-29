package view.gui

import scala.io.StdIn
import controller.logic.CECP
import model.Move
import controller.GameSubscriber
import view.BoardView
import javax.swing.JFrame
import view.Action
import scala.concurrent.Future
import scala.annotation.tailrec

class GameGUI extends JFrame with BoardView {
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