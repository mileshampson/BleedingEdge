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

import java.security.MessageDigest
import java.math.BigInteger
import scala.Array
import java.io.{ByteArrayInputStream, DataInputStream, DataOutputStream, ByteArrayOutputStream}
import java.net.InetAddress
import scala.Predef._

/**
 * Identifies the 'where' and 'what' of a monitored byte array for later comparison.
 * @param location for example a filesystem path
 * @param bytes for example the contents of a file. Optional if the bytes are not currently available.
 * @param byteLookupFn A function providing the bytes representing the state of the location. Defaults to the bytes
 *                    parameter, if the bytes were not available at construction this should specify how to get them.
 */
class LocationState private (val location: String, val bytes: Array[Byte] = Array.empty)
                            (val byteLookupFn: () => Array[Byte] = () => bytes)
{
  assert(location != null, "Snapshot initialised with invalid location")
  val byteId = new Resource(bytes)

  override def equals(that: Any) =
  {
    that match
    {
      case r: LocationState => r.location == location && r.byteId == byteId
      case _ => false
    }
  }

  override def hashCode = byteId.hashCode + location.hashCode

  override def toString = byteId + "@" + location

  /**
   * Uniquely identifies the contents of the specified byte array without storing it.
   */
  final class Resource(val bytes: Array[Byte])
  {
    assert(bytes != null, "Snapshot resource was initialised with invalid state information")
    // This will return the same value for the same resource across multiple JVMs
    var resourceHash = new BigInteger(1, MessageDigest.getInstance("MD5").digest(bytes)).toString(16)
    // A slight optimisation to comparison time, at the expense of having to transmit more information,
    // is to also store the length to avoid comparing long hash strings for unequally sized byte arrays
    var originalLength = bytes.length

    // Equal Resources always return true, but there is also an exceedingly small (around
    // k^2/(6.8*10^38) percent for k resources with our hash function) chance of the md5 hash
    // of the bytes of two different resources also being equal. This does not violate the equals
    // contract, but means it is theoretically possible that a change may be missed at some point.
    override def equals(that: Any) =
    {
      that match
      {
        case r: Resource => r.originalLength == originalLength && r.resourceHash == resourceHash
        case _ => false
      }
    }

    // The 32 bit space of possible hashes here is less than the 128 bit space of possible
    // md5 hashes that can be identifying the resource. The only impact in the rare event
    // of different Resources having the same hash will be a small performance penalty on
    // the lookup of those Resources in hash based data structures holding them.
    override def hashCode = resourceHash.hashCode

    override def toString = resourceHash
  }
}

/**
 * This companion class provides a singleton snapshot representing no data, used to match on other instances of no data.
 * It also provides serialisation and deserialisation of the information identifying the location.
 */
object LocationState
{
  private val emptyState = new LocationState("")()
  def apply() = emptyState
  def apply(location: String, bytes: Array[Byte]) = new LocationState(location, bytes)()

  def apply(bytes: Array[Byte], startReadPos: Int, byteLookup: (String, String, Int) => Array[Byte]) =
  {
    val incomingBytes = new ByteArrayInputStream(bytes, startReadPos, bytes.length)
    val inBuffer = new DataInputStream(incomingBytes)
    val msgType = inBuffer.readShort()
    val location = inBuffer.readUTF()
    val length = inBuffer.readInt()
    val hash = inBuffer.readUTF()
    val state = new LocationState(location)(byteLookupFn = () => byteLookup(inBuffer.readUTF(), hash, length))
    state.byteId.resourceHash = hash
    state.byteId.originalLength = length
    state
  }

  def asBytes(state: LocationState): Array[Byte] =
  {
    val outgoingBytes = new ByteArrayOutputStream()
    val outBuffer = new DataOutputStream(outgoingBytes)
    outBuffer.writeShort(MessageType.STATE_BROADCAST.ordinal())
    outBuffer.writeUTF(state.location)
    outBuffer.writeInt(state.byteId.originalLength)
    outBuffer.writeUTF(state.byteId.resourceHash)
    outBuffer.writeUTF(InetAddress.getLocalHost.getHostName)
    outgoingBytes.toByteArray
  }
}
