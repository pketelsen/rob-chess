package robot

import breeze.linalg.DenseMatrix

package object types {
  type Mat = DenseMatrix[Double]

  /** Converts a 4x4 Matrix to a string of 12 numbers omitting the last row */
  def matToString(mat: Mat): String = mat.t.toDenseVector.toArray.toList.take(12).mkString(" ")

  /** Converts a String of 12 Numbers to a 4x4 Matrix with 0 0 0 1. */
  def stringToMat(s: String): Mat = listToMat(s.split(" ").map(_.toDouble).toList)

  def listToMat(l: List[Double]): Mat = {
    val List(m11, m12, m13, m14,
      m21, m22, m23, m24,
      m31, m32, m33, m34) = l

    DenseMatrix((m11, m12, m13, m14),
      (m21, m22, m23, m24),
      (m31, m32, m33, m34),
      (0.0, 0.0, 0.0, 1.0))
  }
  
  object rot {
    private def c(a: Double): Double = Math.cos(a / 180.0 * Math.PI)
    private def s(a: Double): Double = Math.sin(a / 180.0 * Math.PI)
    
    def x(a: Double) = DenseMatrix(
      (1.0, 0.0, 0.0, 0.0),
      (0.0, c(a), -s(a), 0.0),
      (0.0, s(a), c(a), 0.0),
      (0.0, 0.0, 0.0, 1.0))
    def y(a: Double) = DenseMatrix(
      (c(a), 0.0, s(a), 0.0),
      (0.0, 1.0, 0.0, 0.0),
      (-s(a), 0.0, c(a), 0.0),
      (0.0, 0.0, 0.0, 1.0))
    def z(a: Double) = DenseMatrix(
      (c(a), -s(a), 0.0, 0.0),
      (s(a), c(a), 0.0, 0.0),
      (0.0, 0.0, 1.0, 0.0),
      (0.0, 0.0, 0.0, 1.0))
  }

  case class Status(hand: String, elbow: String, arm: String) {
    override def toString = List(hand, elbow, arm).mkString(" ")
  }
}