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
import scala.concurrent.Channel

class Process(proc: ScalaProcess, stdin: BufferedWriter, stdout: BufferedReader) {
  def destroy(): Unit = {
    proc.destroy()
    stdin.close()
    stdout.close()
  }

  def readLine(): String = stdout.readLine()
  def writeLine(line: String): Unit = {
    stdin.write(line + "\n")
    stdin.flush()
  }
}

object Process {
  def apply(p: ProcessBuilder): Process = {
    val stdinChannel = new Channel[OutputStream];
    val stdoutChannel = new Channel[InputStream];

    val proc = p.run(new ProcessIO(
      { stdinChannel write _ },
      { stdoutChannel write _ },
      _.close()))

    val stdin = stdinChannel.read
    val stdout = stdoutChannel.read

    new Process(proc,
      new BufferedWriter(new OutputStreamWriter(stdin)),
      new BufferedReader(new InputStreamReader(stdout)))
  }
}