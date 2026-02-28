#!/bin/bash
./build.sh

echo "=== Generating 100,000,000 rows ==="
java -cp target/classes challenge.Main generate 100000000

echo ""
echo "=== Processing measurements.txt ==="
java -cp target/classes -Xmx8g challenge.Main process

echo ""
echo "=== Done ==="
echo "Output written to target/data/output.json"
head -20 target/data/output.json
