#!/bin/bash
# Script to run Spotless code formatting

# Run spotless in quiet mode to minimize logging
if mvn spotless:apply -q -B -ntp; then
    echo "Formatting successful."
else
    exit 1
fi
