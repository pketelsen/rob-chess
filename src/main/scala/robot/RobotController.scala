package robot

import java.util.Random
import breeze.linalg.DenseMatrix
import breeze.linalg.InjectNumericOps
import model.Game
import robot.types.Mat
import model.GameSubscriber
import model.Move
import scala.concurrent.Future

class RobotController(robot: Robot, tracking: Tracking) extends GameSubscriber {
  robot.setSpeed(10)
  robot.command("SetAdeptFine 50")

  tracking.setFormatMatrixRowWise()

  val (t_Eff_Mark, t_Rob_Track): (Mat, Mat) = calibrate()

  println(t_Eff_Mark.toString)
  println(t_Rob_Track.toString)

  /*def getMarkerPosInRobCoord(marker: String): Option[Mat] = {
    if (!tracking.chooseMarker(marker)) {
      throw new RuntimeException("Marker not available.")
    }
    val (_, visible, t_Track_Marker, q) = tracking.getNextValueMatrixRowWise()

    if (visible) {
      Some(t_Rob_Track * t_Track_Marker)
    } else {
      None
    }
  }*/

  def calibrate(): (Mat, Mat) = {
    val numMeasurements = 10
    val homePos = robot.getPositionHomRowWise()
    val status = robot.getStatus()
    val radius = 150

    def measurements(n: Int, l: List[Measurement] = List()): List[Measurement] = {
      if (n <= 0) return l

      val t1 = System.currentTimeMillis()
      println("starting to move...")
      val possible = robot.moveMinChangeRowWiseStatus(homePos * RobotController.random(radius), status)

      if (!possible) {
        println("Movement impossible")
        return measurements(n, l)
      }

      val t2 = (System.currentTimeMillis() - t1) / 1000.0
      println(s"Movement took $t2 seconds")

      try {
        Thread.sleep(150)
      } catch {
        case _: InterruptedException =>
      }

      val t_Robot_Eff = robot.getPositionHomRowWise()

      val (t_Track_Marker, q, count) = (1 to 100)
        .map(_ => tracking.getNextValueMatrixRowWise())
        .foldLeft((DenseMatrix.zeros[Double](4, 4), 0.0, 0.0))(Function.untupled {
          case ((accMat, accQ, accN), (_, visible, t_Track_Marker, q)) =>
            if (visible && q < 0.5)
              (accMat + t_Track_Marker, accQ + q, accN + 1)
            else
              (accMat, accQ, accN)
        })

      if (count < 90) {
        println("marker not visible")
        return measurements(n, l)
      }

      println("visible measurement. quality: " + q/count)
      println(s"$n to go")
      measurements(n - 1, Measurement(t_Robot_Eff, t_Track_Marker :/ count) :: l)
    }

    Calibration.calibrate(measurements(numMeasurements))
  }
  
  def handle(move: Move): Future[Unit] = Future.successful(())
}

object RobotController {

  /** Random point and orientation on sphere defined by radius r. */
  def random(r: Double): Mat = {
    val rand = new Random();

    val x = rand.nextGaussian()
    val y = rand.nextGaussian()
    val z = rand.nextGaussian()

    val t = (r / Math.sqrt(x * x + y * y + z * z)) * DenseMatrix(x, y, z)

    def rdAg() = Math.PI * (Math.random() - .5)
    def c(a: Double): Double = Math.cos(a)
    def s(a: Double): Double = Math.sin(a)
    val a = rdAg() / 4
    val b = rdAg() / 4
    val g = rdAg()

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

    val hom = DenseMatrix.horzcat(rz * ry * rx, t)
    DenseMatrix.vertcat(hom, DenseMatrix(0.0, 0.0, 0.0, 1.0).t)

  }
}