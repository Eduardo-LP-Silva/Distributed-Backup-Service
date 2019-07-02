#!/bin/bash
if [ "$#" == 2 ]
then 
	java app.TestClient "$1" DELETE "$2"
else
	echo "Usage: delete.sh <access_point> <file>"
fi
