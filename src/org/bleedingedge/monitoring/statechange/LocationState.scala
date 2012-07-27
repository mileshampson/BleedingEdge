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

import collection.mutable.{MultiMap => mMMap, HashMap => mHMap, Set => mSet, MapProxy=> mProx}
import java.nio.file.Path
import org.bleedingedge.monitoring.Resource

class LocationState()
{
  private final val resources = new mHMap[Resource, mSet[Path]] with mMMap[Resource, Path]

  def updateResourceAt(path : Path)
  {
    require(path != null, "We never receive updates with null path")
    resources.addBinding(new Resource(path), path)
  }

  // The stored resources that currently exist on the filesystem. TODO pre-compute for network transfer
  def getExistingResources(): mMMap[Resource, Path] =
  {
    new mProx[Resource,Set[Path]] with mMMap[Resource, Path] {
      val self = resources.map{case (res, pths) => (res, pths.filter(pth => pth.toFile.exists()))}.filter(_._2.nonEmpty)
    }
  }

}
