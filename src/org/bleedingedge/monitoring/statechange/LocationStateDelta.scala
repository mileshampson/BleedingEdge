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

class LocationStateDelta()
{
  var baselineState = new LocationState()

  def computeDeltaToState(newState: LocationState): mQueue[AddDeleteEvent]   =
  {
    val eventQueue = new mQueue[AddDeleteEvent]
    val addDelLists = compareResourcesWith(newState)
    // TODO create queue of add and remove events
    // TODO remove NONEs, anything left over needs to be removed from the queue of add and remove events
    addDelLists._1.foreach(addElm => addDelLists._2.foreach(delElm => eventQueue.enqueue(
      new AddDeleteEvent(addElm._1, Option(addElm._2), delElm._1, Option(delElm._2)))))
    eventQueue
  }

  // A sequence of all the added, and a sequence of all the deleted, resources between the specified location states
  private def compareResourcesWith(newState: LocationState): (Seq[(Resource, Path)], Seq[(Resource, Path)]) =
  {
    val oldResources  = baselineState.getExistingResources()
    val newResources  = newState.getExistingResources()
    val deletedResources = oldResources -- newResources.keys
    val addedResources = newResources -- oldResources.keys
    val equalResources = oldResources -- deletedResources.keys
    for (equalResource <- equalResources)
    {
      val oldPaths = equalResource._2
      // This Option has to be a Some by the above definition of Equal resources
      val newPaths = newResources.get(equalResource._1).get
      require(!newPaths.isEmpty, "Assumes the LocationState has stripped out resources with no paths")
      val deletedPaths = oldPaths -- newPaths
      if (!deletedPaths.isEmpty)
      {
        deletedResources.put(equalResource._1, deletedPaths)
      }
      val addedPaths = newPaths -- oldPaths
      if (!addedPaths.isEmpty)
      {
        addedResources.put(equalResource._1, addedPaths)
      }

    }
    (addedResources.flatten.toList, deletedResources.flatten.toList)
  }
}
