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

import java.nio.file._
import attribute.BasicFileAttributes
import collection.mutable.{HashMap => mHMap, HashSet => mHSet}
import scheduling.ThreadPool
import statechange.LocationStateChangeEvent

class Location(path : Path) {
  var locationChanges = new LocationChangeQueue()

  updateResourcesAt(path)

  // TODO At the moment is called multiple times for the same file due to the java FSW implementation, this is
  // inefficient but functionally not a problem as this does not change the internal state of LocationState
  def updateResourcesAt(location : Path)
  {
    location.toFile.isFile match {
      case true => locationChanges.processLocationUpdate(location)
      case _ => Files.walkFileTree(path, ResourceVisitor)
    }
  }

  object ResourceVisitor extends SimpleFileVisitor[Path]
  {
    val watcher:WatchService = FileSystems.getDefault().newWatchService()
    val watchKeys = new mHMap[WatchKey,Path]

    override def preVisitDirectory(dirPath:Path, att:BasicFileAttributes):FileVisitResult  =
    {
      watchKeys.put(dirPath.register(watcher, StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY), dirPath)
      FileVisitResult.CONTINUE
    }

    override def visitFile(filePath:Path, att:BasicFileAttributes):FileVisitResult  =
    {
      updateResourcesAt(filePath)
      FileVisitResult.CONTINUE
    }
  }

  def startChangeScanning()
  {
    ThreadPool.execute(scanChanges _)
  }

  def scanChanges():Object =
  {
    // TODO The Little Turing Machine That Could (halt)
    while (true)
    {
      val key = ResourceVisitor.watcher.take()
      val it = key.pollEvents().iterator
      while (it.hasNext())
      {
        val event = it.next()
        val kind = event.kind()
        // TODO could do pattern patching on kind here
        val path = ResourceVisitor.watchKeys.get(key).get.resolve(event.asInstanceOf[WatchEvent[Path]].context())
        updateResourcesAt(path)
      }
      if (!key.reset())
      {
        ResourceVisitor.watchKeys.remove(key)
      }
    }
    ResourceVisitor.watchKeys
  }

  def dequeueChanges(): mHSet[LocationStateChangeEvent] =
  {
    val changesUntilNow = locationChanges.stateChangeQueue
    // Reset the changed state to the current state
    locationChanges = new LocationChangeQueue(locationChanges.currentState)
    changesUntilNow
  }
}
