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

import collection.mutable.{MultiMap => mMMap, HashMap => mHMap, Set => mSet}
import java.nio.file.Path
import org.bleedingedge.monitoring.Resource
import org.bleedingedge.monitoring.logging.LocalLogger

/**
 * The resources that exist in a location at the current time
 */
class LocationState(val resources: mMMap[Resource, Path] = new mHMap[Resource, mSet[Path]] with mMMap[Resource, Path])
{
  def this(other: LocationState) = {
    this(other.resources.clone().asInstanceOf[mMMap[Resource, Path]])
  }

  def updateResourceAt(path : Path)
  {
    val resource = new Resource(path)
    resources.addBinding(resource, path)
    LocalLogger.recordDebug("Recorded state of " + resource + " at path " + path)
  }

  /**
   *  The stored resources that currently exist on the filesystem.
   */
  def getExistingResources(): Seq[(Resource, Path)] =
    resources.map{case (resource, paths) => paths.filter(path => path.toFile.exists()).map((resource, _))}.flatten.toSeq

  /**
   * Provides a sequence of events that transform the current location state into the specified location state.
   * @param newState to calculate transform to.
   * @return an immutable sequence of events that will carry out the requested transform.
   */
  def computeDeltaToState(newState: LocationState): Seq[LocationStateChangeEvent] =
  {
    val oldResources = getExistingResources()
    val newResources = newState.getExistingResources()
    val debugString = "Delta from " + oldResources + " to " + newResources
    // Fill with nulls to make length equal and pair each old resource against a new resource to generate an event.
    val pairs = oldResources.padTo(newResources.size, null) zip newResources.padTo(oldResources.size, null)
    val eventQueue :Seq[LocationStateChangeEvent] = pairs.map(eventPair =>
      new LocationStateChangeEvent(Option(eventPair._1), Option(eventPair._2)))
      .filterNot(event => event.eventType == UpdateType.NO_CHANGE)
    LocalLogger.recordDebug(debugString + " was " + eventQueue)
    eventQueue
  }
}
