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

import collection.mutable.{Set => mSet}
import org.bleedingedge.containers.LocationState
import actors.Actor
import org.bleedingedge.scheduling.ThreadPool
import org.bleedingedge.Resource._
import org.bleedingedge.monitoring.logging.LocalLogger

object ResourceTest extends TestHarness
{
  var mutator: org.bleedingedge.monitoring.LocationMutator = _
  var receiver: TestLocationReceiver = _

  def setUp()
  {
    mutator = new LocationMutator(System.getProperty("user.dir") + System.getProperty("file.separator") + "testDir")
    receiver = new TestLocationReceiver()
  }

  def tearDown()
  {
    ThreadPool.terminateAll()
    mutator.deleteAll()
  }

  def runTests()
  {
    singleOperationTypeTests()
  }

  def singleOperationTypeTests()
  {
    val createdSet: Seq[LocationState] = mutator.createRandom()
    assertChangeEventsMatch(Seq.empty, "Scanning has not started", "Passed pre-scan test")

    receiver.start()
    ThreadPool.execute(){scanChanges _}
    assertChangeEventsMatch(createdSet, "Not all creates were enqueued", "All creates enqueued")

    val changedSet: Seq[LocationState] = mutator.modifySomeExistingFiles()    // TODO some still failing
    assertChangeEventsMatch(changedSet, "Not all modifies were enqueued", "All modifies enqueued")

    val deletedSet: Seq[LocationState] = mutator.deleteAll()
    assertChangeEventsMatch(deletedSet, "Not all deletes were enqueued", "All deletes enqueued")
  }

  def assertChangeEventsMatch(expected: Seq[LocationState], failMsg: String, passMsg: String)
  {
    var numLoops = 0
    while(receiver.numberOfChanges != expected.size && numLoops < 10)
    {
      numLoops+=1
      Thread.sleep(100)
    }
    val actual: Seq[LocationState] = receiver.dequeueChanges()
    val additionalFailInfo = ". Expected [" + expected.mkString(",") + "] rather than [" + actual.mkString(",") + "]."
    val additionalPassInfo = " with expected result [" + actual.mkString(",") + "]."
    // Compare unordered
    assertCondition(expected.toSet.subsetOf(actual.toSet) && actual.toSet.subsetOf(expected.toSet),
                    failMsg + additionalFailInfo, passMsg + additionalPassInfo)
  }

  def scanChanges():Object =
  {
    scanDirectoryForChanges(mutator.basePath, receiver)
    LocalLogger.recordDebug("Scanning terminated")
    null
  }
}

class TestLocationReceiver extends Actor
{
  var updateList: mSet[LocationState] = mSet.empty
  def act()
  {
    while(true)
    {
      receive
      {
        case state:LocationState =>
          LocalLogger.recordDebug("Received a location state " + state)
          updateList += state
      }
    }
  }

  def dequeueChanges(): Seq[LocationState] =
  {
    val changesUntilNow = updateList.clone().toSeq
    updateList.clear()
    changesUntilNow
  }

  def numberOfChanges = updateList.size
}
