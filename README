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

**1 - Application. Provides an API.**
* Processes user specification of locations where file system events can 
be shared, which groups to communicate them with, and what filtering to apply.

**2 - Resource. Translates between a filesystem and the library.**
* A function monitoring a location and outputting a set of resources 
that change at that location.
* A function taking a queue of change events and perfoming them on
the filesystem.

**3 - Transposition. Shifts data between change events and resources.** 
* A function taking two sets of resources and outputting a queue of
events for each add, delete, move or update between the two.
* A function taking a queue of move events and a set of resources, which 
applies the changes to the resources and outputs them.

**4 - Codec. Translates between change events (specifying byte 
differences between abstract locations) and transmissible data sequences.**
* A function that uses fountain codes to contstruct network sequences
out of a set of change events.
* A function that takes an encoded network sequence and partially
reassembles a set of change events.
* A function that combines partially reassembled change events.

**5 - Network. Translates between variable length data sequences and 
multicast packets.**
* A function taking a data sequence and a group identifier which 
broadcasts the sequence to all members of the group.
* Functions for transmitting information about groups.
* Functions for receiving information about groups.
* A function that receives multicast packets and outputs a data sequence.

------------------------------------------------------------------------
<div class="footer">
 &copy; 2012 Miles Hampson
</div>