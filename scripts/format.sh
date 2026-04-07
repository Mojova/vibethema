#!/bin/bash
# Script to run Spotless code formatting

echo "Running code formatting..."
mvn spotless:apply
echo "Formatting complete."
