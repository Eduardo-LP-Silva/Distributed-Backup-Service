#!/bin/bash
if [ "$#" == 2 ]
then 
	java app.TestClient "$1" RECLAIM "$2"
else
	echo "Usage: reclaim.sh <access_point> <max_space>"
fi
