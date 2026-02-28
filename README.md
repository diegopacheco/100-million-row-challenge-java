# 100 Million Row Challenge - Java

Java 25 solution for the [100 Million Row Challenge](https://github.com/tempestphp/100-million-row-challenge).
Parses 100M CSV rows of website visit data and produces a JSON file with visit counts per URL path per day.

## Input Format

```
https://stitcher.io/blog/some-post,2026-01-24T01:16:58+00:00
https://stitcher.io/blog/another-post,2024-01-24T01:16:58+00:00
```

## Output Format

```json
{
    "\/blog\/some-post": {
        "2025-01-24": 1,
        "2026-01-24": 2
    }
}
```

## How It Works

- Memory-mapped file I/O via `MappedByteBuffer` for fast reads
- File is split into chunks aligned to newline boundaries (one chunk per CPU core)
- Each chunk is processed in parallel using virtual threads
- Thread-local `TreeMap` results are merged at the end
- Output is sorted alphabetically by path and date
- Zero external dependencies - pure Java 25

## Build

```
./build.sh
```

## Run

Generate data and process it:
```
./run.sh
```

Or step by step:
```
java -cp target/classes challenge.Main generate 1000000
java -cp target/classes -Xmx8g challenge.Main process
```

Data files are stored in `target/data/`.

## Results

```
❯ ./run.sh
=== Generating 100,000,000 rows ===
Generated 10000000 rows...
Generated 20000000 rows...
Generated 30000000 rows...
Generated 40000000 rows...
Generated 50000000 rows...
Generated 60000000 rows...
Generated 70000000 rows...
Generated 80000000 rows...
Generated 90000000 rows...
Generated 100000000 rows to target/data/measurements.txt

=== Processing measurements.txt ===
Processed 50 unique paths to target/data/output.json
Completed in 7.171s

=== Done ===
Output written to target/data/output.json
{
    "\/blog\/11-million-rows-in-seconds": {
        "2024-01-01": 2078,
        "2024-01-02": 2034,
        "2024-01-03": 2011,
        "2024-01-04": 1982,
        "2024-01-05": 2002,
        "2024-01-06": 1979,
        "2024-01-07": 1988,
        "2024-01-08": 2033,
        "2024-01-09": 2099,
        "2024-01-10": 2034,
        "2024-01-11": 2038,
        "2024-01-12": 1970,
        "2024-01-13": 1976,
        "2024-01-14": 1942,
        "2024-01-15": 2019,
        "2024-01-16": 1971,
        "2024-01-17": 1999,
        "2024-01-18": 1953,
```
