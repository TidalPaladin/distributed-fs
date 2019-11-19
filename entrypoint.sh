#!/bin/sh
set -e
if [ $ID == 0 ]; then
	echo "Begin file" > /artifacts/file.txt
fi
exec java -jar /app/hw2.jar $MODE
