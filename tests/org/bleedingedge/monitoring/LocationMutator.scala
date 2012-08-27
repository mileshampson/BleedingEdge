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

import java.nio.file.{StandardCopyOption, Path, Files, Paths}
import java.io.{FileWriter, BufferedWriter, File}
import collection.mutable.{HashSet => mHSet}
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
  // Allow the test to be replayed if necessary
  val randomSeed = System.currentTimeMillis()
  val rand = new Random(randomSeed)
  logDebug("set up with seed " + randomSeed + " and base directory created at " + baseDirString)

  /**
   * Creates a random number of files under the top level directory, and possibly some deeper directories and files.
   * TODO create multiple files in subdirectories.
   * @return A queue containing the state change events required to generate the end state of the test directory.
   */
  def createRandom() : mHSet[LocationStateChangeEvent] =
  {
    val events: mHSet[LocationStateChangeEvent] = new mHSet[LocationStateChangeEvent]()
    for (i <- 0.until(rand.nextInt(5)+1))
    {
      val numDirs = if (rand.nextInt(5) > 1) 1 else (rand.nextInt(5) + 1)
      val fullPath = baseDirString + System.getProperty("file.separator") + Seq.fill(numDirs)(
        java.lang.Long.toString(rand.nextLong(), 36).substring(1)).mkString(System.getProperty("file.separator"))
      events.add(createFile(fullPath))
    }
    events
  }

  def createFile(fullPath: String) : LocationStateChangeEvent =
  {
    new File(fullPath).getParentFile().mkdirs()
    val fullPathType = Paths.get(fullPath)
    Files.createFile(fullPathType)
    val res = new Resource(fullPathType)
    logDebug("created " + res + " at " + fullPath)
    new LocationStateChangeEvent(None, Some((res, fullPathType)))
  }

  def modifySomeExistingFiles() : mHSet[LocationStateChangeEvent] =
  {
    modifySomeExistingFiles(new File(baseDirString))
  }

  def modifySomeExistingFiles(toModify: File) : mHSet[LocationStateChangeEvent] =
  {
    val events: mHSet[LocationStateChangeEvent] = new mHSet[LocationStateChangeEvent]()
    toModify.listFiles().foreach(file =>
      if (file.isDirectory()) {
        events ++= modifySomeExistingFiles(file)
      } else {
        // To do or not to do
        if (rand.nextBoolean())
        {
          // An update
          if (rand.nextBoolean())
          {
            val fullPath = file.getAbsolutePath
            val fullPathType = Paths.get(fullPath)
            val oldRes = new Resource(fullPathType)
            // Write a small amount of random characters to the file
            val out = new BufferedWriter(new FileWriter(file))
            out.write(java.lang.Long.toString(rand.nextLong(), 36).substring(1))
            out.close()
            val newRes = new Resource(fullPathType)
            logDebug("modified " + oldRes + " to " + newRes + " at " + fullPath)
            events.add(new LocationStateChangeEvent(Some((oldRes, fullPathType)), Some((newRes, fullPathType))))
          }
          // else a move
          else
          {
            val oldPathString = file.getAbsolutePath
            val oldPath = Paths.get(oldPathString)
            val oldRes = new Resource(oldPath)
            var newFileName = baseDirString
            // A rename (move in the same directory)
            if (rand.nextBoolean())
            {
              newFileName = file.getParent + System.getProperty("file.separator") +
                            java.lang.Long.toString(rand.nextLong(), 36).substring(1)
              logDebug("renamed " + oldRes + " from " + oldPathString + " to " + newFileName)
            }
            // else a move anywhere in the testDir
            else
            {
              val numDirs = if (rand.nextInt(5) > 1) 1 else (rand.nextInt(5) + 1)
              newFileName += System.getProperty("file.separator") + Seq.fill(numDirs)(java.lang.Long.toString(
                rand.nextLong(), 36).substring(1)).mkString(System.getProperty("file.separator"))
              new File(newFileName).getParentFile().mkdirs()
              logDebug("moved " + oldRes + " from " + oldPathString + " to " + newFileName)
            }
            val newPath = Paths.get(newFileName)
            Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING)
            val newRes = new Resource(newPath)
            events.add(new LocationStateChangeEvent(Some((oldRes, oldPath)), Some((newRes, newPath))))
          }
        }
      })
    events
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
          logDebug("failed to delete directory at " + dir.toFile.getAbsolutePath)
          return false
        }
        else
        {
          logDebug("deleted directory at " + dir.toFile.getAbsolutePath)
        }
      }
    }
    val fileDirString = if(dir.toFile.isDirectory) "directory" else "file"
    val exists = dir.toFile.exists()
    if (dir.toFile.delete())
    {
      logDebug("deleted " + fileDirString + " at " + dir.toFile.getAbsolutePath)
      return true
    }
    else
    {
      if (exists)
      {
        logDebug("failed to delete " + fileDirString + " at " + dir.toFile.getAbsolutePath)
      }
      return false
    }
  }

  def logDebug(debug: String)
  {
    LocalLogger.recordDebug("Test file generator - " + debug)
  }
}
