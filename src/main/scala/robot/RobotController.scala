package robot

import controller.Host
import robot.Calibration._
import robot.types._
import breeze.linalg._

class RobotController(robHost: Host, trackHost: Host) {

  val robot = new Robot(robHost)
  val tracking = new Tracking(trackHost)

  robot.setSpeed(10)

  val marker = ??? //TODO

  val (t_Eff_Mark: Mat, t_Rob_Track: Mat) = RobotController.calibrate(robot, tracking, marker)

  def getMarkerPosInRobCoord(marker: String): Option[Mat] = {
    if (!tracking.chooseMarker(marker)) {
      throw new RuntimeException("Marker not available.")
    }
    val (_, visible, t_Track_Marker, q) = tracking.getNextValueMatrixRowWise()

    if (visible) {
      Some(t_Rob_Track * t_Track_Marker)
    } else {
      None
    }
  }
}

object RobotController {

  def calibrate(robot: Robot, tracking: Tracking, marker: String): (Mat, Mat) = {
    var measurements = Seq[Measurement]()
    val numMeasurements = 10
    val homePos = robot.getPositionHomRowWise() //TODO homePos speichern
    val status = robot.getStatus()
    val radius = 20 //TODO choose radius
    tracking.chooseMarker(marker)
    while (measurements.size < numMeasurements) {

      val t_Robot_Eff = homePos * random(radius)
      robot.moveMinChangeRowWiseStatus(t_Robot_Eff, status)

      val (_, visible, t_Track_Marker, q) = tracking.getNextValueMatrixRowWise()
      //TODO maybe do something with the quality
      if (visible)
        measurements = Measurement(t_Robot_Eff, t_Track_Marker) +: measurements
    }

    Calibration.calibrate(measurements)
  }

  /** Random point and orientation on sphere defined by radius r. */
  def random(r: Double): Mat = {
    def randTran = r * (Math.random() - .5) * 2
    def randAng = Math.PI * (Math.random() - .5)
    def c(a: Double): Double = Math.cos(a)
    def s(a: Double): Double = Math.sin(a)

    val x = randTran
    val y = randTran
    val z = randTran
    val t = DenseMatrix(x, y, z, 1.0)

    val a = randAng
    val b = randAng
    val g = randAng

    val rx = DenseMatrix(
      (1.0, 0.0, 0.0),
      (0.0, c(a), -s(a)),
      (0.0, s(a), c(a)))
    val ry = DenseMatrix(
      (c(b), 0.0, s(b)),
      (0.0, 1.0, 0.0),
      (-s(b), 0.0, c(b)))
    val rz = DenseMatrix(
      (c(g), -s(g), 0.0),
      (s(g), c(g), 0.0),
      (0.0, 0.0, 1.0))

    val rot = DenseMatrix.vertcat(rz * ry * rx, DenseMatrix(0.0, 0.0, 0.0).t)

    DenseMatrix.horzcat(rot, t)

  }
}