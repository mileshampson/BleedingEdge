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

import statechange.LocationStateChangeEvent

object MonitorLocationTest extends TestHarness
{
  var mutator: org.bleedingedge.monitoring.LocationMutator = _
  var location: org.bleedingedge.monitoring.Location = _

  def setUp()
  {
    mutator = new LocationMutator(System.getProperty("user.dir") + System.getProperty("file.separator") + "testDir")
    location = new Location(mutator.basePath)
  }

  def tearDown()
  {
    mutator.deleteAll()
  }

  def runTests()
  {
    singleOperationTypeTests()
  }

  def singleOperationTypeTests()
  {
    val createdSet: Seq[LocationStateChangeEvent] = mutator.createRandom()
    assertChangeEventsMatch(Seq.empty, "Scanning has not started", "Passed pre-scan test")

    location.startChangeScanning()
    assertChangeEventsMatch(createdSet, "Not all creates were enqueued", "All creates enqueued")

    // TODO delete (so also move) are failing due to not receiving an event to trigger a delta from something to nothing
    // TODO look at whether we just get one file system event for the activity
    val changedSet: Seq[LocationStateChangeEvent] = mutator.modifySomeExistingFiles()
    assertChangeEventsMatch(changedSet, "Not all modifies were enqueued", "All modifies enqueued")

    val deletedSet: Seq[LocationStateChangeEvent] = mutator.deleteAll()
    assertChangeEventsMatch(deletedSet, "Not all deletes were enqueued", "All deletes enqueued")
  }

  def combinedOperationTests()
  {
  }

  def assertChangeEventsMatch(expected: Seq[LocationStateChangeEvent], failMsg: String, passMsg: String)
  {
    var numLoops = 0
    while(location.numberOfChanges != expected.size && numLoops < 10)
    {
      numLoops+=1
      Thread.sleep(100)
    }
    val actual: Seq[LocationStateChangeEvent] = location.dequeueChanges()
    val additionalFailInfo = ". Expected [" + expected.mkString(",") + "] rather than [" + actual.mkString(",") + "]."
    val additionalPassInfo = " with expected result [" + actual.mkString(",") + "]."
    assertCondition(actual.equals(expected), failMsg + additionalFailInfo, passMsg + additionalPassInfo)
  }
}
