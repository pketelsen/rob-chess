package robot

import java.util.Random
import breeze.linalg.DenseMatrix
import breeze.linalg.InjectNumericOps
import model.Game
import robot.types._
import model.GameSubscriber
import model.Move
import scala.concurrent.Future
import controller.Host
import scala.annotation.tailrec
import breeze.linalg.inv
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import scala.io.Source

class RobotController(robotHost: Host, trackingHost: Host) extends GameSubscriber {
  private val markerEffector = "Gripper_14012016"
  //private val markerEffector = "NDITool3"
  private val markerChessboard = "Chessboard"
  private val homePos = List(5.606552, -49.14139, 86.9076, 13.33459, 32.99462, -148.7874)

  val robot = new Robot(robotHost)
  val trackingEffector = Tracking(trackingHost, markerEffector)
  val trackingChessboard = Tracking(trackingHost, markerChessboard)

  robot.setSpeed(10)
  robot.command("SetAdeptFine 50")
  robot.gripperMoveToPosition(0.0025)

  val (t_Rob_Track, t_Eff_Mark): (Mat, Mat) = getCalibration(false)
  
  //robot.movePTPJoints(homePos)

  val t_Track_Board = measureTracker(trackingChessboard) match {
    case Some(m) => m
    case None    => throw new RuntimeException("Chessboard not visible")
  }

  val t_Rob_Board = t_Rob_Track * t_Track_Board
  val t_Board_Rob = inv(t_Rob_Board)
  println(t_Board_Rob)

  println(robot.getPositionHomRowWise())
  println(t_Board_Rob * robot.getPositionHomRowWise())
  moveToBoardPosition(3, 0, 5)

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

  def moveToBoardPosition(file: Int, rank: Int, height: Double) {
    val (dx, dy, dz) = (24, 20.5, -234)
    val (sx, sy, sz) = (57.25, 57.25, -1.0)
    
    def c(a: Double): Double = Math.cos(a / 180.0 * Math.PI)
    def s(a: Double): Double = Math.sin(a / 180.0 * Math.PI)
    val corr_ax = 2
    val corr_ay = -5
    val corr_az = 3.1
    val corr = rot.y(corr_ay) * rot.x(corr_ax) * rot.z(corr_az)

    val (x, y, z) = (dx + sx * rank, dy + sy * file, dz + sz * height)
    val m = DenseMatrix(
      (-1.0, 0.0, 0.0, x),
      (0.0, 1.0, 0.0, y),
      (0.0, 0.0, -1.0, z),
      (0.0, 0.0, 0.0, 1.0))

    println(t_Rob_Board * m)
    println(robot.moveMinChangeRowWiseStatus(t_Rob_Board * corr * m, robot.getStatus()))
  }

  def measureTracker(tracking: Tracking): Option[Mat] = {
    val (mat, q, count) = (1 to 100)
      .map(_ => tracking.getNextValueMatrixRowWise())
      .foldLeft((DenseMatrix.zeros[Double](4, 4), 0.0, 0))(Function.untupled {
        case ((accMat, accQ, accN), (_, visible, t_Track_Marker, q)) =>
          if (visible && q < 0.6)
            (accMat + t_Track_Marker, accQ + q, accN + 1)
          else
            (accMat, accQ, accN)
      })

    if (count < 90) {
      println(s"marker not visible ($count)")
      return None
    }

    println("visible measurement. quality: " + q / count)
    Some(mat :/ count.toDouble)
  }

  def getCalibration(calibrateAnew: Boolean) =
    if (calibrateAnew) {
      robot.movePTPJoints(homePos)
      robot.gripperGoHome
      val (t_RT, t_EM) = calibrate()

      println(matToString(t_RT))
      println(matToString(t_EM))

      val file = new File("calibration.txt")
      val bw = new BufferedWriter(new FileWriter(file))
      bw.write(matToString(t_RT))
      bw.newLine()
      bw.write(matToString(t_EM))
      bw.close()

      (t_RT, t_EM)
    } else {
      val in = Source.fromFile("calibration.txt").getLines().toIndexedSeq
      val t_RT = stringToMat(in(0))
      val t_EM = stringToMat(in(0))
      (t_RT, t_EM)
    }

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

      measureTracker(trackingEffector).map(t_Track_Marker =>
        Measurement(t_Robot_Eff, t_Track_Marker))
    }

    @tailrec
    def measurements(l: List[Measurement] = List()): List[Measurement] = {
      if (l.length >= numMeasurements)
        return l

      measurement() match {
        case Some(m) =>
          println(s"${numMeasurements - (1 + l.length)} to go")
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
