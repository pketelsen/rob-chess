package robot

import breeze.linalg._
import robot.types._

case class Measurement(m: Mat, n: Mat) {
  val mInv = inv(m)
  val mRot = mInv(0 until 3, 0 until 3)
  val mTrans = mInv(0 until 3, 3)

  def A: Mat = {
    val aLeft = (0 until 4) map { row =>
      (0 until 3) map { col =>
        mRot * n(col, row)
      } reduce ((x, y) => DenseMatrix.horzcat(x, y))
    } reduce ((x, y) => DenseMatrix.vertcat(x, y))

    val aMiddle = DenseMatrix.vertcat(
      DenseMatrix.zeros[Double](9, 3),
      mRot)

    val aRight = -DenseMatrix.eye[Double](12)

    DenseMatrix.horzcat(aLeft, aMiddle, aRight)
  }

  def b: DenseVector[Double] = {
    DenseVector.vertcat(DenseVector.zeros[Double](9), -mTrans)
  }
}

object Calibration {
  def calibrate(measurements: Seq[Measurement]): (Mat, Mat) = {
    val A = DenseMatrix.vertcat(measurements map (_.A): _*)
    val b = DenseVector.vertcat(measurements map (_.b): _*)

    val w = A \ b

    val rotX = w(0 until 9).toDenseMatrix.reshape(3, 3)
    val transX = w(9 until 12).toDenseMatrix.t

    val svd.SVD(uX, _, vX) = svd(rotX)

    val rotY = w(12 until 21).toDenseMatrix.reshape(3, 3)
    val transY = w(21 until 24).toDenseMatrix.t

    val svd.SVD(uY, _, vY) = svd(rotY)

    val bottom = DenseMatrix((0.0, 0.0, 0.0, 1.0))

    val X = DenseMatrix.vertcat(
      DenseMatrix.horzcat(uX * vX, transX),
      bottom)
    val Y = DenseMatrix.vertcat(
      DenseMatrix.horzcat(uY * uY, transY),
      bottom)

    (X, Y)
  }
}

object Testdata {
  private val ms = Seq(
    DenseMatrix((-0.009079, -0.301517, -0.953418, 113.127815), (-0.271279, 0.918443, -0.287873, 230.66814), (0.962458, 0.256029, -0.090134, 704.183), (0.0, 0.0, 0.0, 1.0)),
    DenseMatrix((0.051851, -0.147721, -0.987669, 194.35318), (0.033141, 0.988709, -0.146137, 351.85446), (0.998105, -0.025155, 0.056161, 650.02985), (0.0, 0.0, 0.0, 1.0)),
    DenseMatrix((0.098553, -0.175092, -0.979607, 122.7095), (0.371173, 0.919829, -0.127066, 505.3294), (0.923319, -0.351081, 0.155641, 486.30917), (0.0, 0.0, 0.0, 1.0)),
    DenseMatrix((-0.072797, -0.444089, -0.89302, 238.74896), (0.729191, 0.587173, -0.351437, 528.65625), (0.680427, -0.676766, 0.281081, 318.02377), (0.0, 0.0, 0.0, 1.0)),
    DenseMatrix((0.170479, -0.30817, -0.935932, 49.03385), (-0.543114, 0.763141, -0.350204, 179.53284), (0.82217, 0.56802, -0.037272, 674.0917), (0.0, 0.0, 0.0, 1.0)),
    DenseMatrix((0.193769, 0.031439, -0.980543, 66.10966), (-0.899317, 0.405084, -0.16473, 81.05772), (0.392024, 0.913739, 0.106766, 874.40125), (0.0, 0.0, 0.0, 1.0)),
    DenseMatrix((-0.009771, -0.154141, -0.988001, 75.716644), (0.474964, 0.86876, -0.140235, 152.77081), (0.879951, -0.470635, 0.064723, 573.3948), (0.0, 0.0, 0.0, 1.0)),
    DenseMatrix((-0.063474, -0.426773, -0.902129, 52.326466), (0.736958, 0.5895, -0.330729, 170.11517), (0.672951, -0.685824, 0.277096, 662.24854), (0.0, 0.0, 0.0, 1.0)),
    DenseMatrix((-0.026473, -0.315705, -0.948488, 73.16237), (0.532274, 0.798682, -0.280698, 48.299664), (0.846158, -0.512287, 0.146898, 637.1944), (0.0, 0.0, 0.0, 1.0)),
    DenseMatrix((-0.149883, -0.495498, -0.85558, 61.036617), (0.610864, 0.63402, -0.474198, 105.781906), (0.777419, -0.593717, 0.207653, 655.03827), (0.0, 0.0, 0.0, 1.0)))

  private val ns = Seq(
    DenseMatrix((0.01089574, -0.95936416, 0.28196044, -191.4947052), (0.42872949, -0.25026202, -0.86807831, 126.34811401), (0.9033672, 0.13034311, 0.40858092, -1835.64953613), (0.0, 0.0, 0.0, 1.0)),
    DenseMatrix((0.16306774, -0.98653437, -0.01260364, -114.61461639), (0.28551813, 0.05941461, -0.95652982, 254.45295715), (0.94439838, 0.15238059, 0.29136206, -1772.79528809), (0.0, 0.0, 0.0, 1.0)),
    DenseMatrix((0.26422762, -0.9051286, -0.33305551, 57.06136322), (0.25739982, 0.39898295, -0.88008973, 386.76428223), (0.92947786, 0.14681559, 0.33840226, -1880.57470703), (0.0, 0.0, 0.0, 1.0)),
    DenseMatrix((0.39245205, -0.67524564, -0.62451958, 247.05386353), (0.45356141, 0.73277375, -0.5072718, 410.05014038), (0.80016463, -0.08417813, 0.59384393, -1793.8527832), (0.0, 0.0, 0.0, 1.0)),
    DenseMatrix((0.05843245, -0.80264299, 0.59359067, -177.43884277), (0.48458361, -0.49705062, -0.71980512, 63.09404755), (0.87279114, 0.32970429, 0.35990375, -1894.15686035), (0.0, 0.0, 0.0, 1.0)),
    DenseMatrix((0.19198175, -0.37624534, 0.90641186, -384.57629395), (0.29341006, -0.85934418, -0.41885333, -33.83324814), (0.93651137, 0.34636255, -0.05458423, -1833.98803711), (0.0, 0.0, 0.0, 1.0)),
    DenseMatrix((0.17624434, -0.8734996, -0.45380214, -45.53543091), (0.2763455, 0.48638538, -0.82889229, 34.06449509), (0.94475981, 0.0206814, 0.32711035, -1871.25927734), (0.0, 0.0, 0.0, 1.0)),
    DenseMatrix((0.39000446, -0.6688009, -0.63293118, -127.21960449), (0.43809215, 0.7393521, -0.51130591, 46.80879593), (0.80992085, -0.0778706, 0.58134704, -1897.60168457), (0.0, 0.0, 0.0, 1.0)),
    DenseMatrix((0.25973297, -0.83992444, -0.47651413, -110.96953583), (0.40655713, 0.54269596, -0.73497782, -67.22206116), (0.87592813, -0.00283225, 0.4824333, -1859.29638672), (0.0, 0.0, 0.0, 1.0)),
    DenseMatrix((0.31979237, -0.78317646, -0.53326116, -125.42340088), (0.57776893, 0.60725569, -0.54536555, -13.04431152), (0.75094334, -0.13369799, 0.64669077, -1883.78735352), (0.0, 0.0, 0.0, 1.0)))

  val measurements: Seq[Measurement] = (ms zip ns) map (x => Measurement(x._1, x._2))

  val x = DenseMatrix(
    (0.09724766, 0.12900532, 0.98718658, 1935.76883024),
    (0.03666726, 0.98813829, -0.13359038, -103.15758058),
    (-0.99327412, 0.04905259, 0.09532711, 691.67109678),
    (0.0, 0.0, 0.0, 1.0));
  val y = DenseMatrix(
    (-0.00029431, 0.99813806, -0.03087099, 0.508958942),
    (0.01635512, -0.03203462, -0.99841412, 26.80600561),
    (-0.99930823, -0.003427950, -0.01818396, -16.99422957),
    (0.0, 0.0, 0.0, 1.0));
}

object CalibrationTest {
  def main(args: Array[String]): Unit = {
    val (x, y) = Calibration.calibrate(Testdata.measurements)
    println(x)
    println
    println(y)
  }
}