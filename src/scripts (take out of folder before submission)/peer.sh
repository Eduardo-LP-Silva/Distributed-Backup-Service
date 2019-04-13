#!/bin/bash
if [ "$#" == 3 ]
then
	java app.Peer "$1" "$2" "$3"
elif [ "$#" == 9 ]
then
	java app.Peer "$1" "$2" "$3" "$4" "$5" "$6" "$7" "$8" "$9"
else
	echo "Usage: peer.sh <version> <peer_num> <peer_access_point> [<mcAddr> <mcPort> <mdbAddr> <mdbPort> <mdrAddr> <mdrPort>]"
fi
