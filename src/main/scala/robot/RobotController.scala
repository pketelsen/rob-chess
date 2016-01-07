package robot

import java.util.Random
import breeze.linalg.DenseMatrix
import breeze.linalg.InjectNumericOps
import model.Game
import robot.types.Mat
import model.GameSubscriber
import model.Move
import scala.concurrent.Future
import controller.Host
import scala.annotation.tailrec

class RobotController(robotHost: Host, trackingHost: Host) extends GameSubscriber {
  private val markerEffector = "Gripper_29012015"
  private val markerChessboard = "Chessboard"
  private val homePos = List(5.606552, -49.14139, 86.9076, 13.33459, 32.99462, -148.7874)

  val t_Eff_Mark = DenseMatrix((0.3638659886373817, 0.4200696539172075, 0.8313501236968803, 2285.3600652953446),
    (-0.14022391123549183, 0.9070640300818531, -0.3969535237902405, -923.8465175435686),
    (-0.9208359229693214, 0.027862720426141574, 0.3889535599267515, 789.2420371276054),
    (0.0, 0.0, 0.0, 1.0))
  val t_Rob_Track = DenseMatrix((0.776766016036591, -0.6295755011617276, 0.016408676595092264, -6.640952861466634),
    (0.01750470614808647, -0.0044616384649865184, -0.9998368262095954, -91.6379379125317),
    (0.6295459805235131, 0.7769264972435732, 0.007554885010426726, -122.25921477273421),
    (0.0, 0.0, 0.0, 1.0))

  val robot = new Robot(robotHost)
  val trackingEffector = Tracking(trackingHost, markerEffector)
  val trackingChessboard = Tracking(trackingHost, markerChessboard)

  robot.setSpeed(13)
  robot.command("SetAdeptFine 50")
  robot.movePTPJoints(homePos)
  robot.gripperGoHome

  //val (t_Eff_Mark, t_Rob_Track): (Mat, Mat) = calibrate()

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
    val base = robot.getPositionHomRowWise()
    val status = robot.getStatus()
    val radius = 150

    def measurement(): Option[Measurement] = {
      val t1 = System.currentTimeMillis()
      println("starting to move...")
      val possible = robot.moveMinChangeRowWiseStatus(base * RobotController.random(radius), status)

      if (!possible) {
        println("Movement impossible")
        return None
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
        .map(_ => trackingEffector.getNextValueMatrixRowWise())
        .foldLeft((DenseMatrix.zeros[Double](4, 4), 0.0, 0.0))(Function.untupled {
          case ((accMat, accQ, accN), (_, visible, t_Track_Marker, q)) =>
            if (visible && q < 0.5)
              (accMat + t_Track_Marker, accQ + q, accN + 1)
            else
              (accMat, accQ, accN)
        })

      if (count < 90) {
        println("marker not visible")
        return None
      }

      println("visible measurement. quality: " + q / count)
      println(s"${n - 1} to go")
      Some(Measurement(t_Robot_Eff, t_Track_Marker :/ count))
    }

    @tailrec
    def measurements(l: List[Measurement] = List()): List[Measurement] = {
      if (l.length >= numMeasurements)
        return l

      measurement() match {
        case Some(m) =>
          println(s"${numMeasurements - l.length} to go")
          measurements(m :: l)

        case None =>
          measurements(l)
      }
    }

    Calibration.calibrate(measurements())
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
