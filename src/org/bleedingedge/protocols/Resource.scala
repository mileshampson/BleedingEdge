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
        for(event <- key.pollEvents().asInstanceOf[java.util.List[WatchEvent[Path]]])
        {
          val path: Path = keyPathOption.get.resolve(event.context())
          LocalLogger.recordDebug("Change at " + path)
          loadResourcesAt(path, watchKeys, watcher, stateChangeReceiver, key)
        }
        // Re-queue the key for more events
        if (!key.reset()) {
          LocalLogger.recordDebug("Watch key at " + keyPathOption.getOrElse("unknown or removed location") +
                                  " could not be reset as the location was invalid. Removing watch on this location.")
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
    // On linux this works for deleted directories. Not so on windows.
    if (fileToHandle.isDirectory)
    {
      if (fileToHandle.exists)
      {
        val containedFiles = fileToHandle.listFiles()
        LocalLogger.recordDebug("Made aware of directory " + pathToHandle + " containing " + containedFiles.length + " files")
        for (containedFileHandle <- containedFiles)
          loadResourcesAt(containedFileHandle.toPath, scanKeys, watcherToAdd, stateChangeReceiver, existingKey)
        scanKeys.put(pathToHandle.register(watcherToAdd, StandardWatchEventKinds.ENTRY_CREATE,
          StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY), pathToHandle)
      }
    }
    else if (fileToHandle.exists)
    {
      LocalLogger.recordDebug("Made aware of a resource at " + pathToHandle)
      stateChangeReceiver ! LocationState(pathToHandle.toString, bytesFromFile(fileToHandle))
    }
    // After deletion, isDirectory is OS dependent. So catch both file and dir deletions here and check this ourselves.
    if (!fileToHandle.exists)
    {
      removeWatch(pathToHandle, scanKeys, watcherToAdd, existingKey)
      stateChangeReceiver ! LocationState(pathToHandle.toString)
    }
  }

  /**
   * Deletion events on windows do not specify the type of thing being deleted. We need to check if it was a
   * directory to prevent file deletions removing the watch on their parent directory.
   * @param pathToHandle
   * @param scanKeys
   * @param watcherToAdd
   * @param existingKey
   */
  def removeWatch(pathToHandle: Path, scanKeys: mHMap[WatchKey,Path], watcherToAdd: WatchService, existingKey: WatchKey)
  {
    // If this path was previously added against a watch key, it was previously a directory
    if (scanKeys.values.contains(pathToHandle))
    {
      LocalLogger.recordDebug("Made aware of a deletion of directory at " + pathToHandle)
      // Remove the entry from our list
      scanKeys -= existingKey
      // There is no API for de-registering the watcher, thus preventing subsequent deletion of the directory on windows
      // until the watch service is stopped (bug 6972833).
    }
    else {
      LocalLogger.recordDebug("Made aware of a deletion of resource at " + pathToHandle)
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
