import controller._
import robot._
import java.util.Scanner
/**
 * Main for testing purposes.
 */

object Main {
  def main(args: Array[String]): Unit = {
    val robot = new Robot(Host("localhost", 5005))

    println("Status: " + robot.getStatus())
    println("Robot type: " + robot.getRobot())
    robot.setSpeed(10)

    val in = new Scanner(System.in)
    val tracking = new Tracking(Host("141.83.19.44", 5000))

    val homePos = List(-1.333557, -52.33612, 104.445, -180.0972, 64.73969, 288.9345)
    robot.movePTPJoints(homePos)

    val markers = tracking.markers
    println("Choose a marker: " + markers.zipWithIndex.map { case (name, num) => "(" + num + "): " + name }.mkString(", "))
    assert(tracking.chooseMarker(markers(in.nextInt)))

    val robControl = new RobotController(robot, tracking)

    // TODO test calibration by showing a marker, and moving to that spot
    // val testMarker = ???
    // val markerPos = robControl.getMarkerPosInRobCoord(testMarker)

  }
}