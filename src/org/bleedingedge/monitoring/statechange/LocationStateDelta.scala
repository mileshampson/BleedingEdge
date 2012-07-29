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

package org.bleedingedge.monitoring.statechange

import java.nio.file.Path
import org.bleedingedge.monitoring.Resource
import collection.mutable.{MultiMap => mMMap, Queue => mQueue}
import collection.mutable

class LocationStateDelta()
{
  var baselineState = new LocationState()

  def computeDeltaToState(newState: LocationState): mQueue[LocationStateChangeEvent]   =
  {
    val eventQueue = new mQueue[LocationStateChangeEvent]
    val oldResources  = baselineState.getExistingResources()
    val newResources  = newState.getExistingResources()
    // Pair each old resource against a new resource to generate an event.
    while (!oldResources.isEmpty) {
      val (oldResource, oldPath) = oldResources.dequeue()
      val eventCandidate:Option[(Resource, Path)] = newResources.find(newResource => new LocationStateChangeEvent(
        Option(oldResource, oldPath), Option(newResource._1, newResource._2)).eventType != UpdateType.NOT_RELATED)
      eventQueue.enqueue(new LocationStateChangeEvent(Option(oldResource, oldPath), eventCandidate))
      if (eventCandidate.isDefined) newResources.dequeueFirst(newResource => newResource.equals(eventCandidate.get))
    }
    // Add all create events
    while (!newResources.isEmpty) {
      val (newResource, newPath) = newResources.dequeue()
      eventQueue.enqueue(new LocationStateChangeEvent(None, Option(newResource, newPath)))
    }
    // All event candidates get added
    eventQueue
  }
}
