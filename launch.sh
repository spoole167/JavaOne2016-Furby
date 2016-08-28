#!/bin/bash

SERIAL=`cat /proc/cpuinfo | grep Serial | cut -d ':' -f 2`
export SERIAL

while : ; do
    echo "launcher starting $SERIAL"
	#git pull

	gradle execute 
		
	if [[ $? -ne 50 ]]; then
		break;
	fi

done


