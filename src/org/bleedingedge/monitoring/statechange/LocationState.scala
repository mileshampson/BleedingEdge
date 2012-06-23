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

import scala.collection.mutable.{Queue => mQueue, HashSet => mHSet}
import org.bleedingedge.monitoring.Resource
import java.nio.file.Path

class LocationState()
{
  private final val resources = new mHSet[Resource]

  def updateResourceAt(path : Path)
  {
    // The resource equality definition means this will update the path of an existing resource
    resources+=new Resource(Option(path))
  }

  def compareToState(secondState: LocationState): mQueue[UpdateEvent] =
  {
    val eventQueue = new mQueue[UpdateEvent]
    resources.foreach {firstResource =>
      {
        val secondResource: Option[Resource] = secondState.resources.find(p=>p.equals(firstResource))
        val event = new UpdateEvent(Option(firstResource), secondResource)
        if (event.updateType != UpdateType.NONE) eventQueue.enqueue()
      }
    }
    // Iterating over the old resources will miss any CREATEs (resources only in the new resource set)
    secondState.resources.foreach {secondResource =>
      {
        val firstResource: Option[Resource] = resources.find(p=>p.equals(secondResource))
        val event = new UpdateEvent(firstResource, Option(secondResource))
        if (event.updateType == UpdateType.CREATE) eventQueue.enqueue(event)
      }
    }
    eventQueue
  }
}
