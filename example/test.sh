#!/bin/bash

# the path to the reduction candidate is passed as the first argument
program="$1"

# try to compile the reduction candidate
# if compilation fails, the reduction candidate no longer triggers the bug => return 0
gcc -w --std=c99 -c -o /dev/null "$program" 2> /dev/null > /dev/null || exit 0

# check if there is a line that contains the two literal numbers 13 and 3
# if there is, return 1 (the "bug" is triggered)
grep -q "[^.]\<13\>[^.].*[^.]\<3\>[^.]" "$program" && exit 1

# the reduction candidate does not trigger the bug => return 0
exit 0
