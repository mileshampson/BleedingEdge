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
import scala.Array
import util.Random

// TODO
package object Codec
{
  def statesToBytes(states: List[LocationState]): Array[Byte] = states.map(state => LocationState.asBytes(state)).reduce(_++_)

  // TODO make tail recursive so we don't have to worry about the recursion depth for large byte arrays
  def bytesToStates(bytes: Array[Byte], n: Int = 0): List[LocationState] =
  {
    if (n >= bytes.length) Nil
    else {
      val state = bytesToState(bytes, n)
      state :: bytesToStates(bytes, n + state.byteId.originalLength)
    }
  }

  def bytesToState(bytes: Array[Byte], startPos: Int) : LocationState = LocationState(bytes, startPos, stateRequestFn _)

  // TODO need to start a receiver rather than returning these bytes. Can either speculatively block this thread for
  // TODO a while pending a response, or immediately fail the operation and rely on the client retrying it
  def stateRequestFn(hostname: String, resourceId: String, resourceLength: Int): Array[Byte] =
  {
    val outgoingBytes = new ByteArrayOutputStream()
    val outBuffer = new DataOutputStream(outgoingBytes)
    outBuffer.writeShort(MessageType.STATE_REQUEST.ordinal())
    outBuffer.writeUTF(hostname)
    outBuffer.writeInt(resourceLength)
    outBuffer.writeUTF(resourceId)
    outgoingBytes.toByteArray
  }
}
