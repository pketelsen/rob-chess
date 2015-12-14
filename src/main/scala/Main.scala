import controller._
import robot._
import java.util.Scanner
/**
 * Main for testing purposes.
 */

object Main {
  def main(args: Array[String]) = {
    val robot = new Robot(Host("localhost", 5005))

    println("Status: " + robot.getStatus())
    println("Robot type: " + robot.getRobot())
    println("Set speed: " + robot.setSpeed(10))

    val in = new Scanner(System.in)
    val tracking = new Tracking(Host("localhost", 5000))

    //TODO homePosition anfahren (erstmal manuell)

    val markers = tracking.markers
    println("Choose a marker: " + markers.zipWithIndex.map { case (name, num) => "(" + num + "): " + name }.mkString(", "))
    val marker = markers(in.nextInt())

    val robControl = new RobotController(robot, tracking, marker)

    //TODO test calibration by showing a marker, and moving to that spot
    val testMarker = ???
    val markerPos = robControl.getMarkerPosInRobCoord(testMarker)

  }
}