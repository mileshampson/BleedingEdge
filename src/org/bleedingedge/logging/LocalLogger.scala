/*
 * Copyright (c) 2012, Miles Hampson
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.bleedingedge.monitoring.logging

import java.text.SimpleDateFormat
import sun.reflect.Reflection
import java.util.Date
import java.io.{File, FileOutputStream, PrintWriter}
import java.nio.file.Paths

// TODO synchronise access by making an actor
object LocalLogger extends Logger
{
  val DIVIDER = "->"
  val EXCEPTION_NOTE = "An Exception was recorded"
  val timestamp = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss.SS")
  val record = LogWriter
  val debug = true; // TODO from properties

  def recordException(exception: Exception)
  {
    record.error(EXCEPTION_NOTE + DIVIDER + timestamp.format(new Date()) +
      DIVIDER + exception.getMessage())
  }

  def recordError(problem: String, resolution: String)
  {
    val caller = Reflection.getCallerClass(2).getName()
    record.error(timestamp.format(new Date()) + DIVIDER + caller +
      DIVIDER + problem + DIVIDER + resolution)
  }

  def recordEvent(eventString: String)
  {
    record.event(timestamp.format(new Date()) + DIVIDER + eventString)
  }

  def recordDebug(eventString: String)
  {
    if (debug)
    {
      record.debug(timestamp.format(new Date()) + DIVIDER +
        Thread.currentThread().getName() + DIVIDER + eventString)
    }
  }

  object LogWriter
  {
    val ERROR_LOG_FILE_NAME = "ErrorLog.txt"
    val EVENT_LOG_FILE_NAME = "EventLog.txt"
    val DEBUG_LOG_FILE_NAME = "DebugLog.txt"
    val path:String = "out" + File.separator + timestamp.format(new Date())
    new File(path).mkdirs()
    println("Creating log files in folder " + path)
    val errorWriter = new PrintWriter(new FileOutputStream(new File(path, ERROR_LOG_FILE_NAME)))
    val eventWriter = new PrintWriter(new FileOutputStream(new File(path, EVENT_LOG_FILE_NAME)))
    val debugWriter = new PrintWriter(new FileOutputStream(new File(path, DEBUG_LOG_FILE_NAME)))

    def error(error: String)
    {
      errorWriter.println(error)
      errorWriter.flush()
    }

    def event(event: String)
    {
      eventWriter.println(event)
      eventWriter.flush()
    }

    def debug(debug: String)
    {
      println(debug)
      debugWriter.println(debug)
      debugWriter.flush()
    }

    override def finalize()
    {
      errorWriter.close()
      eventWriter.close()
      debugWriter.close()
    }
  }
}
