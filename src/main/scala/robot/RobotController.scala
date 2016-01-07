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

class RobotController(robotHost: Host, trackingHost: Host) extends GameSubscriber {
  private val markerEffector = "Gripper_29012015"
  //private val markerEffector = "NDITool3"
  private val markerChessboard = "Chessboard"
  private val homePos = List(5.606552, -49.14139, 86.9076, 13.33459, 32.99462, -148.7874)

  val t_Rob_Track = stringToMat("0.35228960972760986 0.3413153801671908 0.87143321151956 2363.359747339102 -0.1822804786470481 0.9383186586416051 -0.2938229431974666 -814.7526334382933 -0.9179683316880661 -0.0553344929166667 0.39277504491946014 806.7438669255289")
  val t_Eff_Mark = stringToMat("0.7756495053701288 -0.6310951382898147 0.00931510848221041 -7.356517905207702 0.014244645408630241 0.00274875194604135 -0.9998947616824094 -92.12209093535157 0.6310031179766532 0.7757005677384151 0.011121794551381015 -122.69712610862004")

  val robot = new Robot(robotHost)
  val trackingEffector = Tracking(trackingHost, markerEffector)
  val trackingChessboard = Tracking(trackingHost, markerChessboard)

  robot.setSpeed(8)
  robot.command("SetAdeptFine 50")
  //robot.movePTPJoints(homePos)
  //robot.gripperGoHome

  //val (t_Rob_Track, t_Eff_Mark): (Mat, Mat) = calibrate()

  println(matToString(t_Rob_Track))
  println(matToString(t_Eff_Mark))

  val t_Track_Board = measureTracker(trackingChessboard) match {
    case Some(m) => m
    case None    => throw new RuntimeException("Chessboard not visible")
  }

  val t_Rob_Board = t_Rob_Track * t_Track_Board
  val t_Board_Rob = inv(t_Rob_Board)
  println(t_Board_Rob)

  println(robot.getPositionHomRowWise())
  println(t_Board_Rob * robot.getPositionHomRowWise())
  moveToBoardPosition(0, 7, 30)

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
    val (dx, dy, dz) = (35, 30, -210)
    val (sx, sy, sz) = (56.5, 56.5, -1.0)

    val (x, y, z) = (dx + sx * rank, dy + sy * file, dz + sz * height)
    val m = DenseMatrix(
      (-1.0, 0.0, 0.0, x),
      (0.0, 1.0, 0.0, y),
      (0.0, 0.0, -1.0, z),
      (0.0, 0.0, 0.0, 1.0))

    println(t_Rob_Board * m)
    println(robot.moveMinChangeRowWiseStatus(t_Rob_Board * m, robot.getStatus()))
  }

  def measureTracker(tracking: Tracking): Option[Mat] = {
    val (mat, q, count) = (1 to 100)
      .map(_ => tracking.getNextValueMatrixRowWise())
      .foldLeft((DenseMatrix.zeros[Double](4, 4), 0.0, 0))(Function.untupled {
        case ((accMat, accQ, accN), (_, visible, t_Track_Marker, q)) =>
          if (visible && q < 1)
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
