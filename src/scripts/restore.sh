#!/bin/bash
if [ "$#" == 2 ]
then 
	java app.TestClient "$1" RESTORE "$2"
else
	echo "Usage: restore.sh <access_point> <file>"
fi
