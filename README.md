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

### Native

```
❯ ./run-native.sh
Warning: The option '-H:+SharedArenaSupport' is experimental and must be enabled via '-H:+UnlockExperimentalVMOptions' in the future.
Warning: Please re-evaluate whether any experimental option is required, and either remove or unlock it. The build output lists all active experimental options, including where they come from and possible alternatives. If you think an experimental option should be considered as stable, please file an issue.
========================================================================================================================
GraalVM Native Image: Generating 'challenge-native' (executable)...
========================================================================================================================
[1/8] Initializing...                                                                                    (4.8s @ 0.39GB)
 Java version: 25.0.1+8-LTS, vendor version: Oracle GraalVM 25.0.1+8.1
 Graal compiler: optimization level: 3, target machine: native, PGO: ML-inferred
 C compiler: cc (apple, arm64, 17.0.0)
 Garbage collector: Serial GC (max heap size: 80% of RAM)
 1 user-specific feature(s):
 - com.oracle.svm.thirdparty.gson.GsonFeature
------------------------------------------------------------------------------------------------------------------------
 1 experimental option(s) unlocked:
 - '-H:+SharedArenaSupport' (origin(s): command line)
------------------------------------------------------------------------------------------------------------------------
Build resources:
 - 28.45GB of memory (20.7% of system memory, using available memory)
 - 16 thread(s) (100.0% of 16 available processor(s), determined at start)
[2/8] Performing analysis...  [******]                                                                   (5.5s @ 0.53GB)
    3,559 types,   3,987 fields, and  18,703 methods found reachable
    1,147 types,      43 fields, and     861 methods registered for reflection
       59 types,      59 fields, and      52 methods registered for JNI access
        0 downcalls and 0 upcalls registered for foreign access
        4 native libraries: -framework Foundation, dl, pthread, z
[3/8] Building universe...                                                                               (1.2s @ 0.65GB)
[4/8] Parsing methods...      [**]                                                                       (3.2s @ 0.88GB)
[5/8] Inlining methods...     [***]                                                                      (0.9s @ 0.63GB)
[6/8] Compiling methods...    [*****]                                                                   (28.7s @ 1.42GB)
[7/8] Laying out methods...   [*]                                                                        (1.6s @ 1.56GB)
[8/8] Creating image...       [*]                                                                        (1.5s @ 0.71GB)
  10.42MB (53.32%) for code area:     8,931 compilation units
   8.78MB (44.92%) for image heap:  132,818 objects and 55 resources
 344.46kB ( 1.76%) for other data
  19.55MB in total image size, 19.55MB in total file size
------------------------------------------------------------------------------------------------------------------------
Top 10 origins of code area:                                Top 10 object types in image heap:
   7.41MB java.base                                            2.28MB byte[] for code metadata
   1.98MB svm.jar (Native Image)                               1.35MB byte[] for java.lang.String                                   209.53kB java.logging                                       796.32kB java.lang.String
 186.85kB com.oracle.svm.svm_enterprise                      527.62kB heap alignment                                                 92.16kB challenge                                          461.10kB byte[] for general heap data                                   89.97kB jdk.proxy2                                         404.95kB java.lang.Class
  89.90kB jdk.graal.compiler                                 370.14kB com.oracle.svm.core.hub.DynamicHubCompanion
  76.61kB jdk.proxy1                                         359.30kB java.util.concurrent.ConcurrentHashMap$Node
  76.60kB org.graalvm.nativeimage.configure                  267.74kB java.util.HashMap$Node
  53.89kB org.graalvm.nativeimage.base                       247.34kB java.lang.Object[]
  99.84kB for 7 more packages                                  1.71MB for 1023 more object types
                            Use '--emit build-report' to create a report with more details.
------------------------------------------------------------------------------------------------------------------------
Security report:
 - Binary includes Java deserialization.
 - CycloneDX SBOM with 5 component(s) is embedded in binary (417B). 5 type(s) could not be associated to a component.
 - Advanced obfuscation not enabled; enable with '-H:AdvancedObfuscation=""' (experimental support).
------------------------------------------------------------------------------------------------------------------------
Recommendations:
 PGO:  Use Profile-Guided Optimizations ('--pgo') for improved throughput.
 FUTR: Use '--future-defaults=all' to prepare for future releases.
 HEAP: Set max heap for improved and more predictable memory usage.
 QBM:  Use the quick build mode ('-Ob') to speed up builds during development.
------------------------------------------------------------------------------------------------------------------------
                        2.3s (4.8% of total time) in 199 GCs | Peak RSS: 2.64GB | CPU load: 7.89
------------------------------------------------------------------------------------------------------------------------
Build artifacts:
 /Users/diegopacheco/git/diegopacheco/100-million-row-challenge-java/target/challenge-native (executable)
========================================================================================================================
Finished generating 'challenge-native' in 48.0s.
Native image built: target/challenge-native
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
Completed in 1.670s

=== Done ===
Output written to target/data/output.json
{
    "\/blog\/11-million-rows-in-seconds": {
        "2024-01-01": 1969,
        "2024-01-02": 1968,
        "2024-01-03": 1974,
        "2024-01-04": 1996,
        "2024-01-05": 2066,
        "2024-01-06": 2050,
        "2024-01-07": 1982,
        "2024-01-08": 1995,
        "2024-01-09": 1997,
        "2024-01-10": 1970,
        "2024-01-11": 1959,
        "2024-01-12": 1950,
        "2024-01-13": 2032,
        "2024-01-14": 1994,
        "2024-01-15": 1939,
        "2024-01-16": 2051,                                                                                                                "2024-01-17": 1978,
        "2024-01-18": 2015,
```

### Related POC

* 100MRC Rust -> https://github.com/diegopacheco/100-million-row-challenge-rust
* 100MRC Zig -> https://github.com/diegopacheco/100-million-row-challenge-zig
* 1000RC Java 25 -> https://github.com/diegopacheco/100-million-row-challenge-java


### Comparison

```
  ┌───────────────────┬───────────────────────────────┬────────────────────────────────┬──────────────────────────────────────────┐
  │      Aspect       │              Zig              │             Rust               │              Java 25                     │
  ├───────────────────┼───────────────────────────────┼────────────────────────────────┼──────────────────────────────────────────┤
  │ Time              │ 0.765s                        │ 1.031s                         │ 2.063s (JVM) / 1.670s (Native)           │
  ├───────────────────┼───────────────────────────────┼────────────────────────────────┼──────────────────────────────────────────┤
  │ Throughput        │ ~130.7M rows/s                │ ~97.0M rows/s                  │ ~48.5M (JVM) / ~59.9M (Native) rows/s   │
  ├───────────────────┼───────────────────────────────┼────────────────────────────────┼──────────────────────────────────────────┤
  │ vs Fastest        │ 1.0x (baseline)               │ 1.35x slower                   │ 2.70x (JVM) / 2.18x (Native) slower     │
  ├───────────────────┼───────────────────────────────┼────────────────────────────────┼──────────────────────────────────────────┤
  │ I/O               │ posix.mmap (direct)           │ memmap2 crate (mmap)           │ MemorySegment + Foreign API (mmap)       │
  ├───────────────────┼───────────────────────────────┼────────────────────────────────┼──────────────────────────────────────────┤
  │ Parallelism       │ std.Thread.spawn (OS threads) │ std::thread::scope (scoped     │ Platform threads (FixedThreadPool)       │
  │                   │                               │  threads)                      │                                          │
  ├───────────────────┼───────────────────────────────┼────────────────────────────────┼──────────────────────────────────────────┤
  │ Map type          │ StringArrayHashMap (flat      │ AHashMap (ahash, pre-allocated)│ HashMap<String, LongLongMap>             │
  │                   │ array)                        │                                │ (custom open-addressing long->long)      │
  ├───────────────────┼───────────────────────────────┼────────────────────────────────┼──────────────────────────────────────────┤
  │ Path key          │ []const u8 slice (zero-copy   │ &str (borrowed from mmap)      │ String.intern() (interned references)    │
  │                   │ from mmap)                    │                                │                                          │
  ├───────────────────┼───────────────────────────────┼────────────────────────────────┼──────────────────────────────────────────┤
  │ Date key          │ [10]u8 (fixed array)          │ [u8; 10] (fixed array)         │ long (encoded YYYYMMDD)                  │
  ├───────────────────┼───────────────────────────────┼────────────────────────────────┼──────────────────────────────────────────┤
  │ String alloc      │ Zero (slices into mmap)       │ Zero (borrows from mmap)       │ Minimal (intern dedup + byte[] bulk)     │
  │ during parse      │                               │                                │                                          │
  ├───────────────────┼───────────────────────────────┼────────────────────────────────┼──────────────────────────────────────────┤
  │ Build             │ -Doptimize=ReleaseFast        │ opt-level=3, lto=true,         │ javac (JVM) / GraalVM native-image -O3   │
  │ optimization      │                               │ codegen-units=1                │ -march=native (Native)                   │
  ├───────────────────┼───────────────────────────────┼────────────────────────────────┼──────────────────────────────────────────┤
  │ JVM flags         │ N/A                           │ N/A                            │ -XX:+UseParallelGC -XX:+AlwaysPreTouch   │
  │                   │                               │                                │ -XX:-TieredCompilation                   │
  ├───────────────────┼───────────────────────────────┼────────────────────────────────┼──────────────────────────────────────────┤
  │ External deps     │ 0                             │ 4 (memmap2, rand, ahash,       │ 0                                        │
  │                   │                               │  memchr)                       │                                          │
  ├───────────────────┼───────────────────────────────┼────────────────────────────────┼──────────────────────────────────────────┤
  │ Lines of code     │ ~150                          │ ~120                           │ ~180                                     │
  │ (processor)       │                               │                                │                                          │
  └───────────────────┴───────────────────────────────┴────────────────────────────────┴──────────────────────────────────────────┘
```
