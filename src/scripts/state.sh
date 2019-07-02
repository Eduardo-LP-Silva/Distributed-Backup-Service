#!/bin/bash
if [ "$#" == 1 ]
then 
	java app.TestClient "$1" STATE
else
	echo "Usage: state.sh <access_point>"
fi
