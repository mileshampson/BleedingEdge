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

import statechange.{LocationStateChangeEvent, LocationState}
import collection.mutable.{Queue => mQueue}
import java.nio.file.Path

class LocationChangeQueue
{
  private val baselineState = new LocationState()
  private val currentState = new LocationState()
  val stateChangeQueue: mQueue[LocationStateChangeEvent] = new mQueue[LocationStateChangeEvent]()

  /**
   * Recalculate all the updates from baseline to the new state created by the specified path update. Need to
   * recalculate all as some of the exiting events may have been cancelled out.
   * @param updatedPath path with a change
   */
  def processLocationUpdate(updatedPath: Path)
  {
    currentState.updateResourceAt(updatedPath)
    val newEvents = baselineState.computeDeltaToState(currentState)
    if (!newEvents.isEmpty)
    {
      stateChangeQueue ++= newEvents
      // TODO notify change
    }
  }
}
