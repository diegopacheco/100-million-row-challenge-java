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
