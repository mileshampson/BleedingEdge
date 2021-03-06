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

import containers._
import java.io.{DataOutputStream, ByteArrayOutputStream}

package object Transposition
{
  /**
   * Updates the current snapshot (TODO specify snapshot and fold continually to avoid state update) with the specified
   * update, and provides the current location states if a new snapshot needs to be created due to the specified update
   * affecting an existing location.
   * @param locationState new location state information
   * @return a list of new location state information that needs to be published.
   */
  def packChange(locationState: LocationState): List[LocationState] = Snapshot(locationState).map{_.states}.get

  /**
   * The operations needed to transform the first snapshot into the second snapshot
   * @param oldSnapshot the old state
   * @param newSnapshot the new state
   * @return a sequence of operations for performing the changes
   */
  def operationsBetween(oldSnapshot: Snapshot, newSnapshot: Snapshot): Seq[Command] =
    allOperationsBetween(oldSnapshot, newSnapshot).filterNot(_.isInstanceOf[DoNothingCommand])

  private def allOperationsBetween(oldState: Snapshot, newState: Snapshot): Seq[Command] = oldState.states.zipAll(
    newState.states, LocationState(), LocationState()) map {case (earlier, later) => Command(earlier, later)}

}
