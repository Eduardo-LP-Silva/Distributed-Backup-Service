#!/bin/bash
if [ "$#" == 3 ]
then 
	java app.TestClient "$1" BACKUP "$2" "$3"
else
	echo "Usage: backup.sh <access_point> <file> <replication>"
fi
