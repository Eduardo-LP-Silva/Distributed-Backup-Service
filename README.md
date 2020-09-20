# Distributed-Backup-Service
A distributed application for managing file backups using UDP as the transport protocol.

## Overview
The system is composed of various machines or peers. Each one can execute 5 different protocols (concurrency is supported): 
* Backup - Choose a file to be backed up. This file will be split into smaller sized chunks and replicated in the system in accordance with the value specified.
* Restore - Choose a file to be restored locally. Due to the nature of the file identification process in the system (which includes file meta-data), to restore a file, i.e, to generate a file which was previously backed up in the system, a peer needs to have access to the original file.
* Delete - Remove a file and its chunks from the system. 
* Reclaim - Manage a peer's disk space available to the system. When lowering this value, if some chunks must be deleted and their replication falls short of the one specified when backed up, the peer will initiate a backup protocol for each one.
* State - It is presented some information regarding the system and that particular peer.

The execution of these requests is done through another process (a test client) that communicates with the peer on that machine through RMI. As such, there needs to be at least one RMI server running. 
As said previously, the communication between different peers is done through UDP multicasting. This raises some reliability issues, however, the system is prepared to deal with some of them through various mechanisms. 

## Usage
To run a peer, one needs to specify:
* The version of the protocol implemented - This must be the same as the other peers, usually 2.0.
* The peer's ID - A unique integer representing that peer.
* The peer's access point - The identifier used by the RMI server to identify that object.
* The port and address of each multicast channel of the system - 3 pairs in the form <Address, Port>, starting with the control channel and ending with the restore one. Note that these arguments can be omitted, although this is not recommended since in that case the peer will assume these values based on fixed/default ones.

To run a test client, the following arguments are necessary:
* Peer access point - The identifier associated with that peer in the RMI server.
* Operation - The operation/protocol to be executed in all caps.
* Argument 1 - The first argument to the protocol specified, when applicable.
* Argument 2 - When applicable (backup), the second argument to the protocol.

The arguments for each protocol are indicated below:

* BACKUP
  * File - The path to the file to be backed up.
  * Replication - An integer indicating the replication degree of the file, i.e, in how many peers the file will be backed up.
* RESTORE
  * File - The path to the file to be restored.
* DELETE
  * File - The path to the file to be deleted from the system.
* RECLAIM
  * Disk space - The maximum amount of disk space avaiable to the system.

The STATE protocol takes no arguments.
