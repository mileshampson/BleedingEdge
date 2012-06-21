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

import scala.collection.mutable.{Queue => mQueue}
import org.bleedingedge.monitoring.Resource
import org.bleedingedge.monitoring.logging.LocalLogger
import java.nio.file.Path

class LocationStateMachine()
{
  var eventQueue = new mQueue[UpdateEvent]

  def update(oldResource: Option[Resource], newResource: Option[Resource])
  {
    logUpdate(oldResource, newResource)
    val event = new UpdateEvent(oldResource.map{_.path}.getOrElse(None), newResource.map{_.path}.getOrElse(None))
    // For efficiency if this is a create check if we already have a delete with this
    // path, in which case the two operations can be replaced by a move operation
    if (event.eventType == UpdateType.CREATE)
    {
      // TODO , also can generalise this to any type of repeated event
      // TODO should create a "compressed queue" for this instead (and no timestamps!)
    }
    eventQueue.enqueue(event)
  }

  private def logUpdate(oldResource: Option[Resource], newResource: Option[Resource])
  {
    LocalLogger.recordDebug("Updating. Path from " +
      oldResource.map{_.path}.getOrElse("None") + " to " +
      newResource.map{_.path}.getOrElse("None") + " and hash from " +
      oldResource.map{_.hashCode}.getOrElse("None") + " to " +
      newResource.map{_.hashCode}.getOrElse("None"))
  }
}
