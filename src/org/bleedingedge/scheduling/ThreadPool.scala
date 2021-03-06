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

package org.bleedingedge.scheduling

import java.util.concurrent._
import org.bleedingedge.monitoring.logging.LocalLogger

// TODO actors, baby!
object ThreadPool
{
  private val executor = Executors.newFixedThreadPool(4)
  private val ecs = new ExecutorCompletionService[Object](executor)

  def execute()(runMe: () => Object):Future[Object] =
  {
    var returnVal: Object = null
    ecs.submit(new Callable[Object]
    {
       override def call():Object =
       {
         try
         {
           returnVal = runMe()
         }
         catch
         {
           case e: Exception =>
           {
             LocalLogger.recordDebug("Run exception " + e.getMessage)
             LocalLogger.recordError("Encountered error while running task", "Aborting task")
             LocalLogger.recordException(e)
           }
         }
         returnVal
       }
    })
  }

  def terminateAll()
  {
    val startThreads = Thread.getAllStackTraces.keySet.toArray.mkString(" ")
    executor.awaitTermination(10, TimeUnit.MILLISECONDS)
    executor.shutdown()
    executor.awaitTermination(10, TimeUnit.MILLISECONDS)
    executor.shutdownNow
    val endThreads = Thread.getAllStackTraces.keySet.toArray.mkString(" ")
    LocalLogger.recordDebug("Shut down started with  " + startThreads +
      ", now the currently running threads are " + endThreads)
  }
}
