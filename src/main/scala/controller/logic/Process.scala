package controller.logic

import java.io.BufferedReader
import java.io.BufferedWriter
import scala.sys.process.{ Process => ScalaProcess, _ }
import java.io.InputStream
import java.io.OutputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.OutputStreamWriter
import java.io.InputStreamReader

class Process(proc: ScalaProcess, stdin: BufferedWriter, stdout: BufferedReader) {
  def destroy(): Unit = {
    proc.destroy()
    stdin.close()
    stdout.close()
  }

  def readLine(): String = stdout.readLine()
  def writeLine(line: String): Unit = stdin.write(line + "\n")
}

object Process {
  def apply(p: ProcessBuilder): Process = {
    var stdin: Option[OutputStream] = None;
    var stdout: Option[InputStream] = None;

    val proc = p.run(new ProcessIO(
      { v => stdin = Some(v) },
      { v => stdout = Some(v) },
      _.close()))

    new Process(proc,
      new BufferedWriter(new OutputStreamWriter(stdin.get)),
      new BufferedReader(new InputStreamReader(stdout.get)))
  }
}