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

import java.nio.file._
import java.security.MessageDigest
import java.math.BigInteger

/**
 * Uniquely identifies a resource from its current state in the specified location.
 * Any resources that are considered unequal can later be split and the hashes of
 * the split components compared to see which section needs to be transmitted.
 */
final class Resource(path : Path)
{
  // This will return the same value for the same resource across multiple JVMs
  val resourceHash = new BigInteger(1, MessageDigest.getInstance("MD5").digest(
    Files.readAllBytes(path))).toString(16)
  // A slight optimisation to comparison time, at the expense of having to transmit more information,
  // is to also store file length to avoid comparing long hash strings for unequally sized files
  val fileLength = path.toFile.length()

  // Equal Resources always return true, but there is also an exceedingly small (around
  // k^2/(6.8*10^38) percent for k resources with our hash function) chance of the md5 hash
  // of the bytes of two different files also being equal. This does not violate the equals
  // contract, but means it is theoretically possible that a file change may be missed at some point.
  override def equals(that: Any) = {
    that match {
      case r: Resource => r.fileLength == fileLength && r.resourceHash.equals(resourceHash)
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


