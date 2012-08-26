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

import org.bleedingedge.monitoring.Resource
import java.nio.file.Path

class LocationStateChangeEvent(val oldResPath: Option[(Resource, Path)], val newResPath: Option[(Resource, Path)])
{
  var eventType:UpdateType = UpdateType.NOT_RELATED

  if (oldResPath.isEmpty)
  {
    eventType = if(newResPath.isEmpty) UpdateType.NO_CHANGE else UpdateType.CREATE
  }
  else if (newResPath.isEmpty)
  {
    eventType = UpdateType.DELETE
  }
  // The same resource in both parameters
  else if (newResPath.get._1.equals(oldResPath.get._1))
  {
    if (newResPath.get._2.equals(oldResPath.get._2))
    {
      // The same resource has an identical path in the add and delete events (they cancel each other out) or no event
      // has occurred (start and end are the same), in either case we can treat this as a NOP
      eventType =  UpdateType.NO_CHANGE
    }
    // The same resource with a different path is a move
    else
    {
      // Treat paths that only differ in the last element as a special case of move, RENAME
      eventType =  if(newResPath.get._2.getFileName.equals(oldResPath.get._2.getParent)) UpdateType.RENAME
                   else UpdateType.MOVE
    }
  }
  // Different resources with the same path can be treated as an update
  else if (newResPath.get._2.equals(oldResPath.get._2))
  {
    eventType = UpdateType.UPDATE
  }
  // Both the resource and the path are unrelated.
  else {
    eventType = UpdateType.NOT_RELATED
  }

  override def equals(that: Any) = {
    that match {
      case r: LocationStateChangeEvent => r.oldResPath.equals(oldResPath) && r.newResPath.equals(newResPath)
      case _ => false
    }
  }

  override def hashCode = oldResPath.map{ _._1}.hashCode() + oldResPath.map{ _._2}.hashCode() +
                          newResPath.map{ _._1}.hashCode() + newResPath.map{ _._2}.hashCode()

  override def toString = eventType.toString + "(" + oldResPath.map{ _._1} + "@" + oldResPath.map{ _._2} + "->" +
                                                     newResPath.map{ _._1} + "@" + newResPath.map{ _._2} + ")"
}
