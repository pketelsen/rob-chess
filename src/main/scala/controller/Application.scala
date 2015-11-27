package controller
import model._
import controller._
import gui.GUI

class Application extends Runnable {
  //TODO wie kann man die objekte sich gegenseitig kennen lassen,
  //ohne nachträglich die felder zu verändern?
  var game: Game = ???;
  var gui: GUI = ???;
  def addGUI(gui: GUI) = { this.gui = gui }
  def connectTracker(host: String, port: Integer) = ???
  def connectRobot(host: String, port: Integer) = ???
  def createGame(whiteName: String, blackName: String, whiteAI: Boolean, blackAI: Boolean) {
    val white: Player = if (whiteAI) new AIPlayer(whiteName) else new HumanPlayer(whiteName, gui.getMove)
    val black: Player = if (blackAI) new AIPlayer(blackName) else new HumanPlayer(blackName, gui.getMove)
    game = new Game(white,black)
    game.subscribe(gui)
  }
  override def run() = {}
}

object Application {
  private def main(args: Array[String]) = {
    val app = new Application()
    val gui = new GUI(app)
    app.addGUI(gui)
    //...
  }
}