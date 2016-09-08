#!/bin/bash
export PATH=/home/pi/.sdkman/candidates/gradle/current/bin:$PATH
SERIAL=`cat /proc/cpuinfo | grep Serial | cut -d ':' -f 2`
export SERIAL

while : ; do
    echo "launcher starting $SERIAL"
	git pull

	gradle execute --no-daemon
		
	if [[ $? -ne 50 ]]; then
		break;
	fi

done


