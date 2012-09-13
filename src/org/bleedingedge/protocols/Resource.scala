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

import collection.mutable.{MultiMap => mMMap, HashMap => mHMap}
import containers.Resource
import java.nio.file._
import java.io.File
import java.util.concurrent.TimeUnit

package object Resource
{
  def updateResource(changedPath: Path, currentResources: mMMap[Resource, Path]): mMMap[Resource, Path] =
    currentResources.addBinding(new Resource(changedPath), changedPath)

  def filterNonExistent(toCheck: mMMap[Resource, Path]): Seq[(Resource, Path)] =
    toCheck.map{case (resource, paths) => paths.filter(path => path.toFile.exists()).map((resource, _))}.flatten.toSeq

  def loadResourcesAt(fileHandle: File, result: mMMap[Resource, Path])
  {
    if(fileHandle.isDirectory)
      for (containedFileHandle <- fileHandle.listFiles())
        loadResourcesAt(containedFileHandle, result)
    else
      result.addBinding(new Resource(fileHandle.toPath), fileHandle.toPath)
  }

  def addWatcherTo(addToPaths:Seq[(Resource, Path)], watcherToAdd:WatchService): mHMap[WatchKey,Path]  =
  {
    mHMap(addToPaths.map(item => (item._2.register(watcherToAdd, StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY), item._2)).toSeq: _*)
  }

  def scanDirectoryForChanges(locationUpdateFn: (Path) => Unit, watcher: WatchService, foundKeys: mHMap[WatchKey,Path])
  {
    Iterator.iterate(watcher.poll(100, TimeUnit.MILLISECONDS))(key =>
    {
      val it = key.pollEvents().iterator
      while (it.hasNext)
      {
        val path: Path = foundKeys.get(key).get.resolve(it.next().asInstanceOf[WatchEvent[Path]].context())
        locationUpdateFn(path)
        foundKeys ++= addWatcherTo(Seq((new Resource(path), path)), watcher)
      }
      if (!key.reset())
      {
        // TODO if this is the only way we get delete events need up update location here as well
        foundKeys -= key
      }
      key
    })
  }
}
