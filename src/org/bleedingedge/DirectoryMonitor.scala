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

import java.nio.file._
import collection.mutable.{Queue => mQueue}
import monitoring.logging.LocalLogger
import org.bleedingedge.containers.LocationStateChangeEvent
import org.bleedingedge.scheduling.ThreadPool
import org.bleedingedge.Resource._

// TODO temporary class for testing, to be removed
class DirectoryMonitor(path: Path)
{
  var stateChangeQueue: mQueue[LocationStateChangeEvent] = mQueue.empty

  def startChangeScanning()
  {
    ThreadPool.execute(scanChanges _)
  }

  def scanChanges():Object =
  {
    scanDirectoryForChanges(path, stateChangeQueue)
    LocalLogger.recordDebug("Scanning thread terminated")
    stateChangeQueue
  }

  def stopChangeScanning()
  {
    ThreadPool.terminateAll() // TODO this code needs to be removed before other threads are introduced
  }

  def dequeueChanges(): Seq[LocationStateChangeEvent] =
  {
    val changesUntilNow = stateChangeQueue
    stateChangeQueue = mQueue.empty
    changesUntilNow
  }

  def numberOfChanges = stateChangeQueue.size
}
