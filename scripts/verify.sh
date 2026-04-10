#!/bin/bash

# Configuration: Ensure paths are correct for the environment
export PATH="/opt/homebrew/bin:/usr/local/bin:/Users/jhkaisan/.cargo/bin:$PATH"

# Run Maven clean test and capture all output to a temporary file
TMP_LOG=$(mktemp)
mvn clean test > "$TMP_LOG" 2>&1
EXIT_CODE=$?

# If Maven exit code is 0, it's a success
if [ $EXIT_CODE -eq 0 ]; then
    echo "success"
    rm "$TMP_LOG"
    exit 0
fi

echo "--- Build/Test Failures Detected ---"

# --- 1. Filter Compilation Failures ---
if grep -q "COMPILATION ERROR" "$TMP_LOG"; then
    echo "[COMPILATION ERRORS]"
    # Capture from the start of errors to the summary line
    sed -n '/\[ERROR\] COMPILATION ERROR/,/\[INFO\] [0-9]* error/p' "$TMP_LOG" | grep "^\[ERROR\]"
fi

# --- 2. Filter Test Failures ---
if grep -q "Failures: [1-9]\|Errors: [1-9]" "$TMP_LOG"; then
    echo -e "\n[TEST FAILURES]"
    # Show the "Failed tests:" section
    sed -n '/Failed tests:/,/^\[INFO\]/p' "$TMP_LOG" | grep -v "\[INFO\]"
    
    # Show the final execution summary line
    grep "Tests run:.*Failures:.*Errors:" "$TMP_LOG" | tail -n 1
fi

# --- 3. Fallback for other errors (e.g., dependency issues, POM errors) ---
if ! grep -q "COMPILATION ERROR\|Failures: [1-9]\|Errors: [1-9]" "$TMP_LOG"; then
    echo "[BUILD FAILURE]"
    grep "^\[ERROR\]" "$TMP_LOG" | head -n 15
fi

# Cleanup
rm "$TMP_LOG"
exit $EXIT_CODE
