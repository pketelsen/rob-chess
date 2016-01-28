package gui

import scala.io.StdIn

import controller.logic.CECP
import model.Move

class GameGUI {
  def getMove(): Move = {
    println("Please enter move: ")
    val in = StdIn.readLine()
    CECP.parseMove(in)
  }
  
  def showMessage(message: String): Unit =
    println(message)
}