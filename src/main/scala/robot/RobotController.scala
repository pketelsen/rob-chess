package robot

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.Random

import scala.annotation.tailrec
import scala.io.Source

import breeze.linalg.DenseMatrix
import breeze.linalg.inv
import controller.Host
import model.Black
import model.BoardPos
import model.Color
import model.White
import robot.piece.Piece
import robot.types.Mat
import robot.types.Mat
import robot.types.matToString
import robot.types.rot
import robot.types.stringToMat

class RobotController(robot: Robot, trackingEffector: Tracking, trackingChessboard: Tracking) extends RobotControl {
  private val homePos = List(8.188339, -72.18192, 85.697, 0.085899, 74.57073, -174.2732)
  private val gripperHomePos = 34
  private val baseHeight = 150

  var calibration: Option[Calibration] = None
  var t_Track_Board: Option[Mat] = None

  robot.setSpeed(15)
  robot.command("SetAdeptFine 50")

  def calibrateRobot() {
    calibration = Some(getCalibration())
  }

  def measureBoard() {
    t_Track_Board = measureTracker(trackingChessboard)
  }

  private def t_Rob_Board = calibration.get.t_Rob_Track * t_Track_Board.get
  private def t_Board_Rob = inv(t_Rob_Board)

  assert(robot.gripperGoHome())
  assert(robot.gripperMoveToPosition(gripperHomePos))
  assert(robot.movePTPJoints(homePos))

  // robot.gripperMoveToPosition(5)
  // println("starting capture")
  // capturePiece(BoardPos(0, 3), 14, Black, Queen)
  // promotePiece(14, Black, BoardPos(0, 3), Queen)

  // testPiece(getBoardPosition(BoardPos(0, 6)), King)
  // testPiece(getCapturedPosition(6, Black), King)

  //  moveToPosition(getCapturedPosition(0, Black)(20))
  //  moveToPosition(getBoardPosition(BoardPos(0, 6))())
  //  moveToPosition(getBoardPosition(BoardPos(0, 6))())
  //  moveToPosition(getCapturedPosition(0, Black)(20))
  //  moveToPosition(getCapturedPosition(0, Black)(-7))
  //  moveToPosition(getBoardPosition(BoardPos(0, 0))(1))
  //  moveToPosition(getBoardPosition(BoardPos(7, 7))(1))

  def movePiece(fromPosition: BoardPos, toPosition: BoardPos, piece: Piece) = {
    movePiece(getBoardPosition(fromPosition)_, getBoardPosition(toPosition)_, piece)
  }
  def promotePiece(fromIndex: Int, fromColor: Color, toPosition: BoardPos, piece: Piece) = {
    movePiece(getCapturedPosition(fromIndex, fromColor)_, getBoardPosition(toPosition)_, piece)
  }
  def capturePiece(fromPosition: BoardPos, toIndex: Int, toColor: Color, piece: Piece) = {
    movePiece(getBoardPosition(fromPosition)_, getCapturedPosition(toIndex, toColor)_, piece)
  }
  private def movePiece(from: Double => Mat, to: Double => Mat, piece: Piece) {
    liftPiece(from, piece)
    putPiece(to, piece)
  }

  private def liftPiece(pos: (Double => Mat), p: piece.Piece) {
    liftOrPutPiece(pos, p.gripHeight, p.gripWidth)
  }

  private def putPiece(pos: (Double => Mat), p: piece.Piece) {
    liftOrPutPiece(pos, p.gripHeight + 1, gripperHomePos)
  }

  private def liftOrPutPiece(pos: (Double => Mat), height: Double, width: Double) {
    moveToPosition(pos(baseHeight))
    moveToPosition(pos(height))
    robot.gripperMoveToPosition(width)
    moveToPosition(pos(baseHeight))
  }

  private def testPiece(pos: (Double => Mat), p: piece.Piece) {
    moveToPosition(pos(p.gripHeight + 10))
    robot.gripperMoveToPosition(p.gripWidth + 20)
  }

  private def boardCoordinates(x: Double, y: Double, z: Double) = {
    val (dx, dy, dz) = (20, 23.5, -228)

    def c(a: Double): Double = Math.cos(a / 180.0 * Math.PI)
    def s(a: Double): Double = Math.sin(a / 180.0 * Math.PI)
    val corr_ax = 2.1
    val corr_ay = -5
    val corr_az = 2.5
    val corr = rot.y(corr_ay) * rot.x(corr_ax) * rot.z(corr_az)

    val (xf, yf, zf) = (dx + x, dy + y, dz + z)
    val m = DenseMatrix(
      (-1.0, 0.0, 0.0, xf),
      (0.0, 1.0, 0.0, yf),
      (0.0, 0.0, -1.0, zf),
      (0.0, 0.0, 0.0, 1.0))

    t_Rob_Board * corr * m
  }
  /** Positions on the Chessboard */
  private def getBoardPosition(p: BoardPos)(height: Double): Mat = {
    val (sx, sy, sz) = (57.25, 57.25, -1.0)
    boardCoordinates(sx * p.rank, sy * p.file, sz * height)
  }

  /** Positions for captured pieces. No bookkeeping is done here. */
  private def getCapturedPosition(idx: Int, color: Color)(height: Double): Mat = {
    val (sx, sy, sz) = (50, 40, -1.0)
    val dz = 11
    val d = 90 // distance to board
    val y = color match {
      case Black => -d - (idx / 8) * sx
      case White => 7 * 57.25 + d + (idx / 8) * sx
    }
    val x = color match {
      case Black => 3.5 * 57.25 + 3.5 * sy - (idx % 8) * sy
      case White => 3.5 * 57.25 - 3.5 * sy + (idx % 8) * sy
    }
    val z = sz * height + dz
    boardCoordinates(x, y, z)
  }

  private def moveToPosition(position: Mat) {
    print("moving: ")
    println(robot.moveMinChangeRowWiseStatus(position, robot.getStatus()))
  }

  private def measureTracker(tracking: Tracking): Option[Mat] = {
    val (mat, q, count) = (1 to 100)
      .map(_ => tracking.getNextValueMatrixRowWise())
      .foldLeft((DenseMatrix.zeros[Double](4, 4), 0.0, 0))(Function.untupled {
        case ((accMat, accQ, accN), (_, visible, t_Track_Marker, q)) =>
          if (visible && q < 0.6)
            (accMat + t_Track_Marker, accQ + q, accN + 1)
          else
            (accMat, accQ, accN)
      })

    if (count < 60) {
      println(s"marker not visible ($count)")
      return None
    }

    println("visible measurement. quality: " + q / count)
    Some(mat :/ count.toDouble)
  }

  private def getCalibration(): Calibration = {
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

    val ret = Calibration.calibrate(measurements())

    assert(robot.movePTPJoints(homePos))

    ret
  }
}

object RobotController {
  def loadCalibration(file: File): Option[Calibration] = {
    try {
      val in = Source.fromFile(file).getLines().toIndexedSeq
      val t_RT = stringToMat(in(0))
      val t_EM = stringToMat(in(0))
      Some(Calibration(t_RT, t_EM))
    } catch {
      case _: Exception => None
    }
  }

  def saveCalibration(file: File, cal: Calibration) {
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(matToString(cal.t_Rob_Track))
    bw.newLine()
    bw.write(matToString(cal.t_Eff_Mark))
    bw.close()
  }

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
