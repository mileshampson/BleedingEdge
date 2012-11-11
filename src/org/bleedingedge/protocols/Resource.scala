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

import collection.mutable.{HashMap => mHMap, MutableList => mList}
import containers.LocationState
import java.nio.file._
import monitoring.logging.LocalLogger
import java.io.{FileInputStream, BufferedInputStream, File}

package object Resource
{
  private def loadResourcesAt(pathToHandle: Path, scanKeys: mHMap[WatchKey,Path],
                              watcherToAdd: WatchService, updateList: mList[LocationState])
  {
    val fileToHandle = pathToHandle.toFile
    if (fileToHandle.isDirectory)
    {
      scanKeys.put(pathToHandle.register(watcherToAdd, StandardWatchEventKinds.ENTRY_CREATE,
                   StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY), pathToHandle)
      for (containedFileHandle <- fileToHandle.listFiles())
        loadResourcesAt(containedFileHandle.toPath, scanKeys, watcherToAdd, updateList)
    }
    else if (fileToHandle.exists)
      updateList++= Transposition.packChange(new LocationState(pathToHandle.toUri.getPath, bytesFromFile(fileToHandle)))
  }

  // TODO send message to appropriate actor rather than adding to the second parameter
  def scanDirectoryForChanges(dirPath: Path, updateList: mList[LocationState])
  {
    val watcher: WatchService = FileSystems.getDefault.newWatchService()
    val watchKeys = mHMap.empty[WatchKey,Path]
    loadResourcesAt(dirPath, watchKeys, watcher, updateList)
    try
    {
      Iterator.continually(watcher.take).foreach(key =>
      {
        val it = key.pollEvents().iterator
        while (it.hasNext)
        {
          val path: Path = watchKeys.get(key).get.resolve(it.next().asInstanceOf[WatchEvent[Path]].context())
          LocalLogger.recordDebug("Update at " + path)
          loadResourcesAt(path, watchKeys, watcher, updateList)
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

  def bytesFromFile(fileName: File): Array[Byte] =
  {
    val bis = new BufferedInputStream(new FileInputStream(fileName))
    Stream.continually(bis.read).takeWhile(-1 !=).map(_.toByte).toArray
  }

//  private def filterNonExistent(toCheck: mMMap[Resource, Path]): Seq[(Resource, Path)] =
//    toCheck.map{case (resource, paths) => paths.filter(path => path.toFile.exists()).map((resource, _))}.flatten.toSeq
}
