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

package org.bleedingedge.monitoring

import java.nio.file.{Path, Files, Paths}
import java.io.File
import collection.mutable.{Queue => mQueue}
import org.bleedingedge.monitoring.logging.LocalLogger
import statechange.LocationStateChangeEvent
import util.Random

/**
 * Utility that manages and manipulates a file system location for test purposes.
 */
class LocationMutator(val baseDirString: String)
{
  val basePath = Paths.get(baseDirString)
  deleteAll()
  Files.createDirectories(basePath)
  LocalLogger.recordDebug("Test utility created dir at " + baseDirString)

  // Creates some random files and directories, returning the series of state change events representing these creations
  def createRandom() : mQueue[LocationStateChangeEvent] =
  {
    val events: mQueue[LocationStateChangeEvent] = new mQueue[LocationStateChangeEvent]()
    for (i <- 0.until(Random.nextInt(20)))
    {
      val numDirs = if (Random.nextInt(5) > 1) 1 else (Random.nextInt(9) + 1)
      val fullPath = baseDirString + Seq.fill(numDirs)(System.getProperty("file.separator") + Random.nextString(Random.nextInt(9)+1))
      val file = new File(fullPath)
      events.enqueue(if (file.isDirectory) createDir(file) else createFile(fullPath))
    }
    events
  }

  def createFile(fullPath: String) : LocationStateChangeEvent =
  {
    createDir(new File(fullPath).getParentFile())
    val fullPathType = Paths.get(fullPath)
    Files.createFile(fullPathType)
    LocalLogger.recordDebug("Test utility created file and dir at " + fullPath)
    new LocationStateChangeEvent(None, Some((new Resource(fullPathType), fullPathType)))
  }

  def createDir(fullPathFile: File) : LocationStateChangeEvent =
  {
    fullPathFile.mkdirs()
    new LocationStateChangeEvent(None, None)
  }

  def delete(dirs: String*)
  {
    for (dir <- dirs)
    {
      val fullPath = baseDirString + System.getProperty("file.separator") + dir
      deletePath(Paths.get(fullPath))
    }

  }

  def deleteAll()
  {
    deletePath(basePath)
  }

  private def deletePath(dir: Path) : Boolean =
  {
    if (Files.isDirectory(dir))
    {
      val children = dir.toFile.list()
      for (i <- 0 until children.length)
      {
        val success = deletePath(new File(dir.toFile, children(i)).toPath)
        if (!success)
        {
          LocalLogger.recordDebug("Test utility failed to delete directory at " + dir.toFile.getAbsolutePath)
          return false
        }
        else
        {
          LocalLogger.recordDebug("Test utility deleted directory at " + dir.toFile.getAbsolutePath)
        }
      }
    }
    val fileDirString = if(dir.toFile.isDirectory) "directory" else "file"
    val exists = dir.toFile.exists()
    if (dir.toFile.delete())
    {
      LocalLogger.recordDebug("Test utility deleted " + fileDirString + " at " + dir.toFile.getAbsolutePath)
      return true
    }
    else
    {
      if (exists)
      {
        LocalLogger.recordDebug("Test utility failed to delete " + fileDirString + " at " + dir.toFile.getAbsolutePath)
      }
      return false
    }
  }

}
