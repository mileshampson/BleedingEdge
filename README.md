Bleeding Edge
================================
This is a protocol stack for distributed file synchronization, released 
under a free software license as documented in each source file. The
architecture of the library is designed to support parrallel processing
by modelling each layer's tasks as stateless functions that can be run
independently when data becomes available from other layers, as such this 
library is being switched from an old Java prototype to this github 
hosted Scala (and later Clojure) implementation.

Layers
------------------------------------------------------------------------
**0 - Scheduler. Handles the running and interaction of other layers.**
* Creates the queues used to store the I/O for each layer.
* Monitors the queues for new items, invoking a function capable of 
processing each. Functions are provided by the other layers and are either 
mapping outgoing filesystem changes towards bytes, or reducing incoming 
bytes towards filesystem changes.
* Defines a logging monad.

**1 - Application. Provides an API.**
* Processes user specification of locations where file system events can 
be shared, which groups to communicate them with, and what filtering to apply.

**2 - Resource. The representation of the filesystem.**
* A function monitoring a location and outputting its state.
* A function taking a set of commands and performing them on the filesystem.

**3 - Transposition. Generates changes from timestamped filesystem data.** 
* A function folding filesystem states into transmissable snapshots.
* A function folding filesystem states into a queue of changes to be performed 
locally.

**4 - Codec. Deconstructs and reconstructs state information.**
* A function outputting the fountain codes of a snapshot.
* A partially applied function for reassembling snapshots from fountain codes.
* A function shifting incoming create and update bytes into snapshots.

**5 - Network. Translates between variable length data sequences and 
multicast packets.**
* A function taking a data sequence and a group identifier which 
broadcasts the sequence to all members of the group.
* Functions for transmitting information about groups.
* Functions for receiving information about groups.
* A function that receives multicast packets and outputs a data sequence.
* A function for requesting data directly from a specified host.

------------------------------------------------------------------------
<div class="footer">
 &copy; 2012 Miles Hampson
</div>