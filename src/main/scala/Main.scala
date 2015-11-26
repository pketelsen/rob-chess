object Main {
  def main(args: Array[String]) = {
    val robot = new Robot("localhost", 5005)

    println("Status: " + robot.getStatus())
    println("Robot type: " + robot.getRobot())
    println("Set speed: " + robot.command("SetAdeptSpeed 10"))

    val r = robot.getPositionJoints()
    println("MovePTPJoints: " + robot.movePTPJoints(r.map(_ - 5)))

    robot.close()
  }
}