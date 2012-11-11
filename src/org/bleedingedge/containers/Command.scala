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

import java.nio.file._

abstract class Command
{
  def apply(dirBase: Path)

  def makePath(parent: Path, child: String): Path =
  {
    // TODO match common root recursive (Paths.get(from).getParent.equals(Paths.get(to).getParent))
    Paths.get(child)
  }
}

class MoveCommand(from: String, to: String) extends Command
{
  override def apply(dirBase: Path)
  {
    val localFrom = makePath(dirBase, from)
    val localTo = makePath(dirBase, from)
    Files.createDirectories(localTo)
    Files.move(localFrom, localTo.resolve(localFrom.getFileName), StandardCopyOption.REPLACE_EXISTING)
  }
}

class DeleteCommand(location: String) extends Command
{
  override def apply(dirBase: Path)
  {
    Files.deleteIfExists(makePath(dirBase, location))
  }
}

class CreateCommand(location: String, byteLookupFn: () => Array[Byte]) extends Command
{
  override def apply(dirBase: Path)
  {
    // Fail if the file already exists
    Files.write(makePath(dirBase, location), byteLookupFn(), StandardOpenOption.CREATE_NEW)
  }
}

class UpdateCommand(location: String, byteLookupFn: () => Array[Byte]) extends Command
{
  override def apply(dirBase: Path)
  {
    // TODO needs a lot of work to match sections (like rsync). For now just update everything (unsafe if failure)!
    Files.deleteIfExists(makePath(dirBase, location))
    Files.write(makePath(dirBase, location), byteLookupFn(), StandardOpenOption.CREATE_NEW)
  }
}

class DoNothingCommand() extends Command
{
  override def apply(dirBase: Path)
  {
  }
}

object Command
{

  def apply(earlier: LocationState, later: LocationState) =
  {
    // Optimise common cases by doing calculations once only
    val byteIdEqual = earlier.byteId == later.byteId
    val locationsEqual = earlier.location == later.location
    if (byteIdEqual) {
      if (locationsEqual) new DoNothingCommand()  // No change
      new MoveCommand(earlier.location, later.location)
    }
    else if (locationsEqual) {
      if (earlier.byteId.originalLength == 0) new CreateCommand(later.location, later.byteLookupFn)
      else if (later.byteId.originalLength == 0) new DeleteCommand(earlier.location)
      new UpdateCommand(later.location, later.byteLookupFn)
    }
    new DoNothingCommand() // Not related
  }

}


