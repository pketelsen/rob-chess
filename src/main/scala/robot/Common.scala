package robot

import java.io._
import java.net._
import breeze.linalg._
import robot.types._
import controller.Host

class LineSocket(host: Host) {

  private val sock = new Socket(host.hostname, host.port)
  private val reader = new BufferedReader(new InputStreamReader(sock.getInputStream()))
  private val writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()))

  protected def send(str: String) = {
    writer.write(str + "\n")
    writer.flush()
  }

  protected def recv(lines: Int): List[String] = lines match {
    case 0 => Nil
    case _ => reader.readLine() :: recv(lines - 1)
  }

  def close() = sock.close()
}

class CommandSocket(host: Host) extends LineSocket(host) {
  def command(cmd: String): String = command(cmd, 1) match {
    case List(reply) => reply
  }

  def command(cmd: String, replyLines: Int): List[String] = {
    send(cmd)
    return recv(replyLines)
  }
}

class Robot(host: Host) extends CommandSocket(host) {
  recv(1) // "welcome to rob6server ..."
  if (command("Hello Robot") != "accepted")
    throw new RuntimeException("Protocol initialization failed")

  def getStatus(): Status = {
    val a = command("GetStatus").split(" ")
    Status(a(0), a(1), a(2))
  }
  def getRobot(): String =
    command("GetRobot")

  def setVerbosity(value: Int): Boolean =
    (command("SetVerbosity " + value) == "true")

  def setSpeed(value: Int): Boolean = {
    (command("SetAdeptSpeed " + value) == "true")
  }
  def movePTPJoints(values: List[Double]): Boolean =
    (command("MovePTPJoints " + String.join(" ", values.map(_.toString): _*)) == "true")

  def getPositionJoints(): List[Double] =
    command("GetPositionJoints").split(" ").toList.map(_.toDouble)

  def getPositionHomRowWise(): Mat = {
    stringToMat(command("GetPositionHomRowWise"))
  }
  def moveMinChangeRowWiseStatus(matrix: Mat, status: Status): Boolean = {
    (command("MoveMinChangeRowWiseStatus " + matToString(matrix) + " " + status) == "true")
  }

  def gripperGoHome: Boolean = {
    command("GripperGoHome", 3)(2) == "true"
  }

  def gripperMoveToPosition(pos: Double): Boolean = {
    command("GripperMoveToPosition " + pos, 3)(2) == "true"
  }
}

class Tracking(host: Host) extends CommandSocket(host) {
  val systemInfo: Map[String, String] = (command("CM_GETSYSTEM").split(" ").toList match {
    case "ANS_TRUE" :: tail => {
      tail.map(
        _.split("=", 2) match {
          case Array(k, v) => (k, v)
          case _           => throw new RuntimeException("Protocol initialization failed")
        })
    }
    case _ => throw new RuntimeException("Protocol initialization failed")
  }).toMap

  def markers: Array[String] = systemInfo("Tracker").split(";")

  def chooseMarker(marker: String): Boolean = (command(marker) == "ANS_TRUE")

  def setFormatQuaternions(): Boolean = (command("FORMAT_QUATERNIONS") == "ANS_TRUE")
  def setFormatMatrixRowWise(): Boolean = (command("FORMAT_MATRIXROWWISE") == "ANS_TRUE")

  def getNextValueMatrixRowWise(): (Double, Boolean, Mat, Double) = {
    command("CM_NEXTVALUE").split(" ").toList match {
      case ts :: v :: rest => {
        val mat = listToMat(rest.take(12).map(_.toDouble))
        val q = rest(12).toDouble

        return (ts.toDouble, v == "y", mat, q)
      }
      case _ => throw new RuntimeException("Invalid response")
    }
  }
}

object Tracking {
  def apply(host: Host, marker: String): Tracking = {
    val tracking = new Tracking(host)
    tracking.chooseMarker(marker)
    tracking.setFormatMatrixRowWise()
    return tracking
  }
}
