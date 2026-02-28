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
Completed in 2.063s

=== Done ===
Output written to target/data/output.json
{
    "\/blog\/11-million-rows-in-seconds": {
        "2024-01-01": 1990,
        "2024-01-02": 1961,
        "2024-01-03": 2012,
        "2024-01-04": 1922,
        "2024-01-05": 1987,
        "2024-01-06": 1989,
        "2024-01-07": 1998,
        "2024-01-08": 2004,
        "2024-01-09": 2011,
        "2024-01-10": 1948,
        "2024-01-11": 2024,
        "2024-01-12": 1995,
        "2024-01-13": 1961,
        "2024-01-14": 1975,
        "2024-01-15": 1954,
        "2024-01-16": 1934,
        "2024-01-17": 1967,
        "2024-01-18": 1995,
```

### Related POC

* 100MRC Rust -> https://github.com/diegopacheco/100-million-row-challenge-rust
* 100MRC Zig -> https://github.com/diegopacheco/100-million-row-challenge-zig
* 1000RC Java 25 -> https://github.com/diegopacheco/100-million-row-challenge-java


### Comparison

```

  ┌───────────────────┬───────────────────────────────┬────────────────────────────┬──────────────────────────────────────────┐
  │      Aspect       │              Zig              │            Rust            │                 Java 25                  │
  ├───────────────────┼───────────────────────────────┼────────────────────────────┼──────────────────────────────────────────┤
  │ Time              │ 0.765s                        │ 1.587s                     │ 2.063s                                   │
  ├───────────────────┼───────────────────────────────┼────────────────────────────┼──────────────────────────────────────────┤
  │ Throughput        │ ~130.7M rows/s                │ ~63.0M rows/s              │ ~48.5M rows/s                            │
  ├───────────────────┼───────────────────────────────┼────────────────────────────┼──────────────────────────────────────────┤
  │ vs Fastest        │ 1.0x (baseline)               │ 2.07x slower               │ 2.70x slower                             │
  ├───────────────────┼───────────────────────────────┼────────────────────────────┼──────────────────────────────────────────┤
  │ I/O               │ posix.mmap (direct)           │ memmap2 crate (mmap)       │ MemorySegment + Foreign API (mmap)       │
  ├───────────────────┼───────────────────────────────┼────────────────────────────┼──────────────────────────────────────────┤
  │ Parallelism       │ std.Thread.spawn (OS threads) │ std::thread::scope (scoped │ Virtual threads                          │
  │                   │                               │  threads)                  │ (newVirtualThreadPerTaskExecutor)        │
  ├───────────────────┼───────────────────────────────┼────────────────────────────┼──────────────────────────────────────────┤
  │ Map type          │ StringArrayHashMap (flat      │ HashMap (std)              │ HashMap<Long, HashMap<Long, Long>>       │
  │                   │ array)                        │                            │                                          │
  ├───────────────────┼───────────────────────────────┼────────────────────────────┼──────────────────────────────────────────┤
  │ Path key          │ []const u8 slice (zero-copy   │ &str (borrowed from mmap)  │ long hash (custom encodePath)            │
  │                   │ from mmap)                    │                            │                                          │
  ├───────────────────┼───────────────────────────────┼────────────────────────────┼──────────────────────────────────────────┤
  │ Date key          │ [10]u8 (fixed array)          │ [u8; 10] (fixed array)     │ long (encoded YYYYMMDD)                  │
  ├───────────────────┼───────────────────────────────┼────────────────────────────┼──────────────────────────────────────────┤
  │ String alloc      │ Zero (slices into mmap)       │ Zero (borrows from mmap)   │ Zero (byte[] + long encoding)            │
  │ during parse      │                               │                            │                                          │
  ├───────────────────┼───────────────────────────────┼────────────────────────────┼──────────────────────────────────────────┤
  │ Build             │ -Doptimize=ReleaseFast        │ opt-level=3, lto=true,     │ None (plain javac)                       │
  │ optimization      │                               │ codegen-units=1            │                                          │
  ├───────────────────┼───────────────────────────────┼────────────────────────────┼──────────────────────────────────────────┤
  │ External deps     │ 0                             │ 2 (memmap2, rand)          │ 0                                        │
  ├───────────────────┼───────────────────────────────┼────────────────────────────┼──────────────────────────────────────────┤
  │ Lines of code     │ ~150                          │ ~120                       │ ~180                                     │
  │ (processor)       │                               │                            │                                          │
  └───────────────────┴───────────────────────────────┴────────────────────────────┴──────────────────────────────────────────┘
```
