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

object MonitorLocationTest extends TestHarness
{
  var mutator: LocationMutator = _
  var location: Location = _

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
    addRemoveTest()
  }

  def addRemoveTest()
  {
    assertUpdateNumber(0, "The test directory has not been created", "Passed initial state test")

    val fSep = System.getProperty("file.separator")
    mutator.create("file1", "subDir" + fSep + "file2")
    assertUpdateNumber(0, "Scanning has not started", "Passed pre-scan test")

    location.startChangeScanning()
    assertUpdateNumber(2, "Scanning has started on a location containing files", "Passed initial scan test")

    mutator.create("subDir" + fSep + "file3")
    assertUpdateNumber(3, "Another file was added to the scanned directory", "Passed added file in subdirectory test")

    mutator.delete("subDir" + fSep + "file3")
    assertUpdateNumber(4, "A file was deleted from the scanned directory", "Passed deleted file in subdirectory test")
    mutator.delete("file1", "subDir" + fSep + "file2")
    assertUpdateNumber(6, "A subdirectory and file have been deleted", "Passed deleted directory test")
  }

  def assertUpdateNumber(num: Int, failMsg: String, passMsg: String)
  {
    // TODO We don't have a notify-on-change mechanism available to use yet, so poll the location for updates
    var numLoops = 0
    while(location.locationChanges.stateChangeQueue.length != num && numLoops < 5)
    {
      numLoops+=1
      Thread.sleep(100)
    }
    val additionalFailInfo = ". Expected " + num + " updates rather than " + location.locationChanges.stateChangeQueue.length + " updates."
    val additionalPassInfo = ". There are " + num + " updates."
    assertCondition(location.locationChanges.stateChangeQueue.length == num, failMsg + additionalFailInfo, passMsg + additionalPassInfo)
  }
}
