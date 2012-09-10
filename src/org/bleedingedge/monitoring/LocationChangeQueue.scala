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

package org.bleedingedge.monitoring

import org.bleedingedge.containers.{Resource, LocationStateChangeEvent}
import collection.mutable.{MultiMap => mMMap, HashMap => mHMap, Set => mSet}
import java.nio.file.Path
import org.bleedingedge.Transposition.generateChangeEventsBetween
import org.bleedingedge.Resource.existingResources
import org.bleedingedge.Resource.updateResource


class LocationChangeQueue(var currentState: mMMap[Resource, Path] = new mHMap[Resource, mSet[Path]] with mMMap[Resource, Path])
{
  var stateChangeQueue: Seq[LocationStateChangeEvent] = Seq.empty

  /**
   * Recalculate all the updates from baseline to the new state created by the specified path update. Recalculate from
   * baseline to current, rather than incrementally, to pick up events that are later cancelled out.
   *
   * @param updatedPath path with a change
   */
  def processLocationUpdate(updatedPath: Path)
  {
    // TODO this depends on eval order, this class is being removed and this will be too.
    stateChangeQueue = generateChangeEventsBetween(existingResources(currentState),
      existingResources(updateResource(updatedPath, currentState)))
  }
}
