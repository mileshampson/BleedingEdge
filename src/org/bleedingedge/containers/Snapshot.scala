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

package org.bleedingedge.containers

class Snapshot private() extends Ordered[Snapshot]
{
  var lastUpdateTime = System.nanoTime()
  var locationStates:Map[String, LocationState] = Map()
  def states : List[LocationState] = locationStates.values.toList
  def compare(that: Snapshot) =  lastUpdateTime.compareTo(that.lastUpdateTime)
}

object Snapshot
{
  // TODO remove shared mutable state
  private var currentSnapshot = new Snapshot()
  def apply(state: LocationState) : Option[Snapshot] =
  {
    var finishedSnapshot: Option[Snapshot] = None
    if (currentSnapshot.locationStates.contains(state.location))
    {
      finishedSnapshot = Some(currentSnapshot)
      currentSnapshot = new Snapshot()
    }
    currentSnapshot.locationStates += (state.location -> state)
    currentSnapshot.lastUpdateTime = System.nanoTime()
    finishedSnapshot
  }
}