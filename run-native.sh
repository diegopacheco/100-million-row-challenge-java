#!/bin/bash
./build-native.sh || exit 1

echo "=== Generating 100,000,000 rows ==="
./target/challenge-native generate 100000000

echo ""
echo "=== Processing measurements.txt ==="
./target/challenge-native process

echo ""
echo "=== Done ==="
echo "Output written to target/data/output.json"
head -20 target/data/output.json
