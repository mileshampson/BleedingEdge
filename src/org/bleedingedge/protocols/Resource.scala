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

package org.bleedingedge

import collection.mutable.{HashMap => mHMap}
import containers.LocationState
import java.nio.file._
import monitoring.logging.LocalLogger
import java.io._
import actors.Actor
import scala.collection.JavaConversions._

package object Resource
{

  /**
   * Passes the current state and and subsequent changes in the specified directory to the specified actor.
   * @param dirPath path representing the root directory. All subdirectories will be included in scanning.
   * @param stateChangeReceiver an actor to receive a series of LocationState updates for each file presently in the
   *                            directory or its subdirectories, and any added while the scanning is running.
   */
  def scanDirectoryForChanges(dirPath: Path, stateChangeReceiver: Actor)
  {
    val watcher: WatchService = FileSystems.getDefault.newWatchService()
    val watchKeys = mHMap.empty[WatchKey,Path]
    loadResourcesAt(dirPath, watchKeys, watcher, stateChangeReceiver, existingKey=null)
    try
    {
      LocalLogger.recordDebug("Starting monitoring of " + dirPath)
      Iterator.continually(watcher.take).foreach(key =>
      {
        val keyPathOption: Option[Path] = watchKeys.get(key)
        LocalLogger.recordDebug("Taking watch key " + keyPathOption)
        for(event <- key.pollEvents().asInstanceOf[java.util.List[WatchEvent[Path]]])  // TODO (... if key.isValid()) to work around double deletions received when file is deletion followed by its containing dir. Sometimes this causes missed events on windows though...
        {
          val path: Path = keyPathOption.get.resolve(event.context())
          LocalLogger.recordDebug("Change at " + path)
          loadResourcesAt(path, watchKeys, watcher, stateChangeReceiver, key)
        }
        if (!key.reset()) {
          LocalLogger.recordDebug("Watch key reset at " + keyPathOption.getOrElse("unknown or removed location"))
          loadResourcesAt(keyPathOption.get, watchKeys, watcher, stateChangeReceiver, key)
        }
      })
    }
    catch
    {
      case e: InterruptedException =>
      {
        LocalLogger.recordDebug("Scanning interupted")
        watcher.close()
        LocalLogger.recordDebug(e.getMessage)
      }
    }
  }

  private def loadResourcesAt(pathToHandle: Path, scanKeys: mHMap[WatchKey,Path],
                              watcherToAdd: WatchService, stateChangeReceiver: Actor, existingKey: WatchKey)
  {
    val fileToHandle = pathToHandle.toFile
    if (fileToHandle.isDirectory)
    {
      if (fileToHandle.exists)
      {
        LocalLogger.recordDebug("Made aware of directory " + pathToHandle + " containing " + fileToHandle.length + " files")
        scanKeys.put(pathToHandle.register(watcherToAdd, StandardWatchEventKinds.ENTRY_CREATE,
          StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY), pathToHandle)
        for (containedFileHandle <- fileToHandle.listFiles())
          loadResourcesAt(containedFileHandle.toPath, scanKeys, watcherToAdd, stateChangeReceiver, existingKey)
      }
      else
      {
        LocalLogger.recordDebug("Made aware of deletion of directory " + pathToHandle)
        pathToHandle.register(watcherToAdd)
        scanKeys -= existingKey
      }
    }
    else if (fileToHandle.exists)
    {
      LocalLogger.recordDebug("Made aware of a resource at " + pathToHandle)
      stateChangeReceiver ! LocationState(pathToHandle.toString, bytesFromFile(fileToHandle))
    }
    else
    {
      LocalLogger.recordDebug("Made aware of a deletion of resource at " + pathToHandle)
      stateChangeReceiver ! LocationState(pathToHandle.toString)
    }
  }

  def bytesFromFile(fileName: File): Array[Byte] =
  {
    var fis: FileInputStream = null
    var bis: BufferedInputStream = null
    try {
      fis = new FileInputStream(fileName)
      bis = new BufferedInputStream(fis)
      Stream.continually(bis.read).takeWhile(-1 !=).map(_.toByte).toArray
    }
    finally {
      close(fis)
      close(bis)
    }
  }

  def close(c: Closeable)
  {
    assert(c != null, "An existing file should have had a closable constructred for it")
    try {
      c.close()
    }
  }
}
