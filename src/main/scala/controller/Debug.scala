package controller

import java.io.FileOutputStream
import java.io.OutputStreamWriter

class Debug(name: String) {
  private val debug = true
  
  private val writer =
    if (debug) {
      new OutputStreamWriter(new FileOutputStream(name + ".log")) 
    } else {
      null
    }
  
  def log(message: String): Unit = {
    if (debug) {
      writer.write(message + "\n")
      writer.flush()
    }
  }
  
  def close(): Unit = {
    if (debug)
      writer.close()
  }
}