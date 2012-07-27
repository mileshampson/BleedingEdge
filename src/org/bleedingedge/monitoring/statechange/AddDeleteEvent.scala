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

class AddDeleteEvent(val addResource: Resource, val addPath: Option[Path], deleteResource: Resource, deletePath: Option[Path])
{
  var eventType:UpdateType = UpdateType.NONE

  // Assume delete and create are given as the same resource but with an optional path
  if (addPath.isEmpty)
  {
    eventType = UpdateType.DELETE
  }
  else if (deletePath.isEmpty)
  {
    eventType = UpdateType.CREATE
  }
  else if (addResource.equals(deleteResource))
  {
    // A path for the same Resource that has been added then deleted, or visa versa, is ignored as a NOP, otherwise move
    if (!addPath.get.equals(deletePath.get))
    {
      // Treat paths that only differ in the last element as a special case of move, RENAME
      eventType =  if(addPath.get.getFileName.equals(deletePath.get.getParent)) UpdateType.RENAME else UpdateType.MOVE
    }
  }
  // Different resources with the same path can be treated as an update
  else if (addPath.get.equals(deletePath.get))
  {
    eventType = UpdateType.UPDATE
  }
}
