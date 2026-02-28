# 100 Million Row Challenge - Java Solution

## Problem Statement

Parse 100 million CSV rows of website visit data and produce a JSON file with visit counts per URL path per day.

## Input Format

```
https://stitcher.io/blog/some-post,2026-01-24T01:16:58+00:00
https://stitcher.io/blog/another-post,2024-01-24T01:16:58+00:00
```

## Output Format

Pretty-printed JSON with URL paths as keys and date-count maps as values:

```json
{
    "\/blog\/some-post": {
        "2025-01-24": 1,
        "2026-01-24": 2
    }
}
```

- Keys are URL paths (without the domain), with forward slashes escaped as `\/`
- Dates sorted ascending
- Pretty JSON output

## Architecture

### Generator (`Generator.java`)
- Predefined list of 50 blog URL paths
- Random dates within a 3-year range (2024-2026)
- Writes CSV rows to `measurements.txt`
- Uses BufferedWriter with 8MB buffer for fast I/O
- Configurable row count (default 1M, supports 100M)

### Processor (`Processor.java`)
- Memory-mapped file via `MappedByteBuffer` for fast reads
- Splits file into chunks aligned to newline boundaries
- Uses virtual threads (Java 25) for parallel chunk processing
- Each thread builds a local `TreeMap<String, TreeMap<String, Long>>`
- Merges all thread-local maps
- Output is sorted alphabetically by path and date (TreeMap guarantees ordering)
- Writes pretty JSON to `output.json`

## Multi-Threading Strategy

1. Memory-map the input file
2. Determine chunk boundaries (one per CPU core), aligned to newline boundaries
3. Each virtual thread processes its chunk independently, building a local TreeMap
4. Merge all TreeMaps sequentially
5. Serialize to JSON

## Performance Considerations

- Memory-mapped I/O avoids buffered read overhead
- Parallel chunk processing saturates all CPU cores
- Thread-local TreeMaps avoid contention
- Manual date extraction (first 10 chars of ISO datetime) avoids full datetime parsing
- Manual URL path extraction avoids URL parsing libraries
- TreeMap for sorted output without explicit sort step
- Virtual threads for lightweight parallelism
- No external dependencies - pure Java 25

## Dependencies

None. Pure Java 25 with no external libraries.
