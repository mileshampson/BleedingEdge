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
import logging.LocalLogger
import scala.collection.mutable.{HashSet => mHSet}
import scala.collection.mutable.{HashMap => mHMap}
import scheduling.ThreadPool
import statechange.LocationStateMachine

class Location(path : Path) {
  private final val lsm: LocationStateMachine = new LocationStateMachine()
  private final val resources = new mHSet[Resource]
  updateResourcesAt(path)

  def updateResourcesAt(location : Path)
  {
    location.toFile.isFile match {
      case true =>
      {
        // This may "equal" an existing resource, but have a new path
        val newResource = new Resource(Option(location))
        lsm.update(resources.find(p=>p.equals(newResource)), Option(newResource))
        resources+=newResource
      }
      case _ => Files.walkFileTree(path, ResourceVisitor)
    }
  }


  object ResourceVisitor extends SimpleFileVisitor[Path]
  {
    val watcher:WatchService = FileSystems.getDefault().newWatchService()
    val watchKeys = new mHMap[WatchKey,Path]

    override def preVisitDirectory(dirPath:Path, att:BasicFileAttributes):FileVisitResult  =
    {
      LocalLogger.recordDebug("Watching directory " + dirPath + " for changes")
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
    // TODO status code loop condition, or The Little Turing Machine That Could (halt)
    while (true)
    {
      val key = ResourceVisitor.watcher.take()

      val it = key.pollEvents().iterator
      while (it.hasNext())
      {
        val event = it.next()
        val kind = event.kind()
        // TODO Do some pattern matching rather than this java type system fail crud
        val path = ResourceVisitor.watchKeys.get(key).get.resolve(
          event.asInstanceOf[WatchEvent[Path]].context())
        LocalLogger.recordDebug("Received change at " + path)
        updateResourcesAt(path)
      }
      if (!key.reset())
      {
        LocalLogger.recordDebug("Stopped watching directory " + path + " for changes")
        ResourceVisitor.watchKeys.remove(key)
      }
    }
    ResourceVisitor.watchKeys
    // TODO rewrite the above code. All of it. Every last line.
  }
}
