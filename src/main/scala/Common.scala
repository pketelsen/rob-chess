import java.io._
import java.net._

class LineSocket(host: String, port: Int) {
  private val sock = new Socket(host, port)
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

class CommandSocket(host: String, port: Int) extends LineSocket(host, port) {
  def command(cmd: String): String = command(cmd, 1) match {
    case List(reply) => reply
  }

  def command(cmd: String, replyLines: Int): List[String] = {
    send(cmd)
    return recv(replyLines)
  }
}

class Robot(host: String, port: Int) extends CommandSocket(host, port) {
  recv(1) // "welcome to rob6server ..."
  if (command("Hello Robot") != "accepted")
    throw new RuntimeException("Protocol initialization failed")

  def getStatus(): String =
    command("GetStatus")

  def getRobot(): String =
    command("GetRobot")

  def setVerbosity(value: Int): Boolean =
    (command("SetVerbosity " + value) == "true")

  def movePTPJoints(values: List[Float]): Boolean =
    (command("MovePTPJoints " + String.join(" ", values.map(_.toString): _*)) == "true")

  def getPositionJoints(): List[Float] =
    command("GetPositionJoints").split(" ").toList.map(_.toFloat)
}

class Tracking(host: String, port: Int) extends CommandSocket(host, port) {
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

  def trackers: Array[String] = systemInfo("Tracker").split(";")

  def chooseTracker(tracker: String): Boolean = (command(tracker) == "ANS_TRUE")

  def setFormatQuaternions(): Boolean = (command("FORMAT_QUATERNIONS") == "ANS_TRUE")
  def setFormatMatrixRowwise(): Boolean = (command("FORMAT_MATRIXROWWISE") == "ANS_TRUE")

  type Vec4[T] = (T, T, T, T)
  type Mat3x4[T] = (Vec4[T], Vec4[T], Vec4[T])

  def getNextValueMatrixRowwise(): (Double, Boolean, Mat3x4[Float], Float) = {
    command("CM_NEXTVALUE").split(" ").toList match {
      case ts :: v :: rest => {
        val List(m11, m12, m13, m14,
          m21, m22, m23, m24,
          m31, m32, m33, m34,
          q) = rest.map(_.toFloat)

        return (ts.toDouble, v == "y",
          ((m11, m12, m13, m14),
            (m21, m22, m23, m24),
            (m31, m32, m33, m34)),
            q)
      }
      case _ => throw new RuntimeException("Invalid response")
    }
  }
}
