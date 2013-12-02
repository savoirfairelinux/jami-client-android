#!/bin/sh

# execute all test to make a single output

for t in bug1 bug2 thread1 digest tcpstr1 url1; do
	echo $t test...
	echo $t test... 1>&2
	./$t
	if [ $? != 0 ]; then
		echo Exit with error from $t
	fi
done

