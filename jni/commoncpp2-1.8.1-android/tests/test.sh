#!/bin/sh

echo Executing all tests... please wait...
sh dotests.sh > testout.txt

if cmp testout.txt output.txt; then
	echo Test successfully
else
	echo 'Test failed :('
	echo 'Compare testout.txt (wrong) and output.txt (correct) for more information'
fi
