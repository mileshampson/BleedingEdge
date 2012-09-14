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

import collection.mutable.{MultiMap => mMMap, HashMap => mHMap, Set => mSet, Queue => mQueue}
import containers.{LocationStateChangeEvent, Resource}
import java.nio.file._
import monitoring.logging.LocalLogger
import org.bleedingedge.Transposition._

package object Resource
{
  private def updateResource(changedPath: Path, currentResources: mMMap[Resource, Path]): mMMap[Resource, Path] =
    if (!changedPath.toFile.isDirectory && changedPath.toFile.exists)
      currentResources.addBinding(new Resource(changedPath), changedPath) else currentResources

  private def filterNonExistent(toCheck: mMMap[Resource, Path]): Seq[(Resource, Path)] =
    toCheck.map{case (resource, paths) => paths.filter(path => path.toFile.exists()).map((resource, _))}.flatten.toSeq

  private def loadResourcesAt(pathToHandle: Path, scanKeys: mHMap[WatchKey,Path],
                              watcherToAdd: WatchService, result: mMMap[Resource, Path])
  {
    if (pathToHandle.toFile.isDirectory)
    {
      scanKeys.put(pathToHandle.register(watcherToAdd, StandardWatchEventKinds.ENTRY_CREATE,
                   StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY), pathToHandle)
      for (containedFileHandle <- pathToHandle.toFile.listFiles())
        loadResourcesAt(containedFileHandle.toPath, scanKeys, watcherToAdd, result)
    }
    else if (pathToHandle.toFile.exists)
      result.addBinding(new Resource(pathToHandle), pathToHandle)
  }

  def scanDirectoryForChanges(dirPath: Path, stateChangeQueue: mQueue[LocationStateChangeEvent])
  {
    val baselineState: Seq[(Resource, Path)] = Seq.empty
    val currentState: mMMap[Resource, Path] = new mHMap[Resource, mSet[Path]] with mMMap[Resource, Path]
    val watcher: WatchService = FileSystems.getDefault.newWatchService()
    var watchKeys = mHMap.empty[WatchKey,Path]
    loadResourcesAt(dirPath, watchKeys, watcher, currentState)
    stateChangeQueue ++= generateChangeEventsBetween(baselineState, filterNonExistent(currentState))
    try
    {
      Iterator.continually(watcher.take).foreach(key =>
      {
        val it = key.pollEvents().iterator
        while (it.hasNext)
        {
          val path: Path = watchKeys.get(key).get.resolve(it.next().asInstanceOf[WatchEvent[Path]].context())
          LocalLogger.recordDebug("Update at " + path)
          loadResourcesAt(path, watchKeys, watcher, currentState)
          // TODO regenerate all is inefficent
          stateChangeQueue.clear()
          stateChangeQueue ++= generateChangeEventsBetween(baselineState, filterNonExistent(updateResource(path, currentState)))
        }
        // TODO check !key.reset() => watchKeys -= key
      })
    }
    catch
    {
      case e: InterruptedException =>
      {
        watcher.close()
        LocalLogger.recordDebug(e.getMessage)
      }
    }
  }
}
