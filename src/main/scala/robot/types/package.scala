package robot

import breeze.linalg.DenseMatrix


package object types {
  type Mat = DenseMatrix[Double]
  
  def matToString(mat: Mat) : String = {???}
  def stringToMat(s: String) : Mat = {???}
  
  case class Status(hand: String, elbow: String, arm: String) {
    override def toString = List(hand, elbow, arm).mkString(" ")
  }
}