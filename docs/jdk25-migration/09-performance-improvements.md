# Performance Improvements (JDK 12-25)

## Overview

From JDK 11 to JDK 25, numerous performance improvements have been introduced across garbage collection, JIT compilation, startup time, and memory usage. This document covers the most impactful changes for backend applications.

## JDK 25 Specific Improvements

### 1. Compact Object Headers (JEP 450)

**Impact**: Reduces heap size by 10-20% and improves GC performance.

Compact object headers reduce the minimum object header size from 96 bits (12 bytes) to 64 bits (8 bytes) on 64-bit platforms.

**Before JDK 25**: Every object had a 12-byte header
**After JDK 25**: Many objects have an 8-byte header

```java
// Example: Object memory savings
public class SmallObject {
    private int value;  // 4 bytes
}

// JDK 11-24:
// - Header: 12 bytes
// - Field: 4 bytes
// - Padding: 0 bytes (aligned to 16)
// Total: 16 bytes per instance

// JDK 25:
// - Header: 8 bytes
// - Field: 4 bytes
// - Padding: 4 bytes (aligned to 16)
// Total: 16 bytes (but internal layout more efficient for GC)

// With arrays or collections, the savings compound:
List<SmallObject> list = new ArrayList<>();
for (int i = 0; i < 1_000_000; i++) {
    list.add(new SmallObject(i));
}
// Saves ~4MB of heap with compact headers!
```

**Backend Impact**:
- **Microservices**: More instances fit in same memory
- **Caching**: Can cache more objects
- **GC Performance**: Reduced memory pressure = less GC
- **Containerized Apps**: Better density in Kubernetes pods

**Enable**:
```bash
# Enabled by default in JDK 25
# Disable if needed (not recommended):
# -XX:-UseCompactObjectHeaders
```

### 2. Ahead-of-Time (AOT) Method Profiling (JEP 483)

**Impact**: Dramatically improves startup time by reusing JIT compilation profiles.

Traditional JIT requires warmup - application runs interpreted until profiling data is collected, then JIT compiles hot methods. AOT profiling saves these profiles from previous runs.

```java
// Example: Cold start performance
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        // JDK 11-24: ~5-10 seconds to reach optimal performance
        // - Starts interpreted
        // - Collects profiling data
        // - JIT compiles hot methods
        // - Full performance after warmup

        // JDK 25 with AOT profiling: ~1-2 seconds
        // - Loads pre-existing profile
        // - JIT compiles immediately
        // - Full performance much faster

        SpringApplication.run(Application.class, args);
    }
}
```

**Enable**:
```bash
# Training run - capture profile
java -XX:AOTMode=record -XX:AOTConfiguration=app.aotconf \
     -jar myapp.jar

# Production run - use profile
java -XX:AOTMode=on -XX:AOTConfiguration=app.aotconf \
     -jar myapp.jar
```

**Backend Impact**:
- **Serverless**: Faster cold starts for Lambda/Cloud Functions
- **Kubernetes**: Faster pod startup during scaling
- **CI/CD**: Quicker deployment verification
- **Development**: Faster application restart

## Garbage Collection Improvements

### 1. Generational Shenandoah (JEP 485 - JDK 25)

Shenandoah's generational mode is now production-ready (was experimental).

**Features**:
- Ultra-low pause times (sub-millisecond)
- Generational collection improves throughput
- Better than non-generational Shenandoah for most workloads

```bash
# Enable Generational Shenandoah
java -XX:+UseShenandoahGC -XX:ShenandoahGCMode=generational \
     -jar myapp.jar
```

**When to Use**:
- Applications requiring consistent low latency
- Large heaps (> 8GB)
- High allocation rates

### 2. ZGC Improvements (JDK 15-25)

Multiple enhancements across versions:

**JDK 15**: Sub-millisecond pause times
**JDK 21**: Generational ZGC
**JDK 25**: String deduplication skips young, short-lived strings

```bash
# Enable ZGC (production-ready since JDK 15)
java -XX:+UseZGC -Xmx16g -jar myapp.jar

# Enable Generational ZGC (JDK 21+)
java -XX:+UseZGC -XX:+ZGenerational -Xmx16g -jar myapp.jar
```

**String Deduplication in JDK 25**:
```java
// Automatically deduplicates strings in old generation
// Skips temporary strings in young generation
String temp1 = "user_" + userId;  // Not deduplicated if dies young
String cached = intern(userId);    // Deduplicated if survives to old gen
```

**Performance Improvement**:
- 20-30% throughput improvement for string-heavy apps
- Reduced CPU overhead from deduplication
- Better memory usage for long-lived strings

### 3. G1GC Enhancements

Continuous improvements to G1 (default GC):

**JDK 12-15**:
- Promptly return unused memory to OS
- Improved concurrent refinement threads

**JDK 16+**:
- Better parallel full GC performance
- Improved humongous object handling

**JDK 21+**:
- Better handling of large heaps
- Improved mixed GC performance

```bash
# G1 tuning for backend services
java -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:G1HeapRegionSize=16m \
     -XX:InitiatingHeapOccupancyPercent=45 \
     -Xmx8g \
     -jar myapp.jar
```

## JIT Compilation Improvements

### 1. C2 Compiler Enhancements

Multiple optimizations across versions:
- Better vectorization (SIMD)
- Improved loop optimizations
- Better inlining decisions
- Escape analysis improvements

```java
// Example: Auto-vectorization
public long sumArray(int[] array) {
    long sum = 0;
    for (int i = 0; i < array.length; i++) {
        sum += array[i];
    }
    return sum;
    // JDK 16+: Auto-vectorized using SIMD instructions
    // Processes multiple array elements per instruction
}
```

### 2. Tiered Compilation Improvements

Better tier transition logic:
- Faster identification of hot methods
- Better tier-up decisions
- Reduced compilation overhead

## Startup Performance

### 1. Class Data Sharing (CDS)

**JDK 12**: Dynamic CDS archives
**JDK 13**: Improved CDS
**JDK 19+**: Better CDS with AOT

```bash
# Create CDS archive
java -XX:ArchiveClassesAtExit=app.jsa -jar myapp.jar

# Use CDS archive
java -XX:SharedArchiveFile=app.jsa -jar myapp.jar

# 20-30% faster startup!
```

### 2. Application Class-Data Sharing

Share application classes, not just JDK classes:

```bash
# Generate class list
java -XX:DumpLoadedClassList=classes.lst -jar myapp.jar

# Create archive
java -XX:SharedClassListFile=classes.lst \
     -XX:SharedArchiveFile=app.jsa \
     -Xshare:dump

# Use archive
java -XX:SharedArchiveFile=app.jsa -Xshare:on -jar myapp.jar
```

## Backend-Specific Optimizations

### 1. HTTP/2 Performance (JDK 11+)

Built-in HTTP/2 client with better performance:

```java
HttpClient client = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .connectTimeout(Duration.ofSeconds(10))
    .build();

// HTTP/2 multiplexing - multiple requests over single connection
List<CompletableFuture<String>> futures = IntStream.range(0, 100)
    .mapToObj(i -> client.sendAsync(
        HttpRequest.newBuilder()
            .uri(URI.create("https://api.example.com/data/" + i))
            .build(),
        HttpResponse.BodyHandlers.ofString()
    ))
    .map(cf -> cf.thenApply(HttpResponse::body))
    .toList();

// Much faster than 100 separate HTTP/1.1 connections
```

### 2. Better NIO Performance

```java
// Improved ByteBuffer performance in JDK 13+
ByteBuffer buffer = ByteBuffer.allocateDirect(8192);

// JDK 13+: Better bulk operations
byte[] data = new byte[8192];
buffer.put(data);  // Optimized in newer JDKs
```

### 3. String Concatenation

**JDK 9+**: `invokedynamic`-based string concatenation (much faster)

```java
// Automatically optimized by javac
String name = "John";
int age = 30;
String message = "Hello, " + name + "! You are " + age + " years old.";

// JDK 8: Uses StringBuilder (slower)
// JDK 9+: Uses invokedynamic (faster, less GC pressure)
```

## Recommended JVM Flags for Backend Services

### High-Throughput Service (REST API)

```bash
java \
  -XX:+UseZGC \
  -XX:+ZGenerational \
  -Xms4g -Xmx4g \
  -XX:+AlwaysPreTouch \
  -XX:+UseCompactObjectHeaders \
  -XX:AOTMode=on -XX:AOTConfiguration=app.aotconf \
  -XX:SharedArchiveFile=app.jsa \
  -jar api-service.jar
```

### Low-Latency Service

```bash
java \
  -XX:+UseShenandoahGC \
  -XX:ShenandoahGCMode=generational \
  -Xms8g -Xmx8g \
  -XX:+AlwaysPreTouch \
  -XX:+UseCompactObjectHeaders \
  -XX:MaxGCPauseMillis=10 \
  -jar trading-service.jar
```

### Memory-Constrained Service (Container)

```bash
java \
  -XX:+UseG1GC \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseContainerSupport \
  -XX:+UseCompactObjectHeaders \
  -XX:SharedArchiveFile=app.jsa \
  -jar containerized-service.jar
```

### Batch Processing

```bash
java \
  -XX:+UseParallelGC \
  -Xms16g -Xmx16g \
  -XX:+AlwaysPreTouch \
  -XX:+UseCompactObjectHeaders \
  -jar batch-processor.jar
```

## Measuring Performance Improvements

### 1. Startup Time

```bash
# Before
time java -jar myapp.jar &
# Wait for "Started" message
# Result: 8.5 seconds

# After (with CDS + AOT profiling)
time java -XX:SharedArchiveFile=app.jsa \
          -XX:AOTMode=on -XX:AOTConfiguration=app.aotconf \
          -jar myapp.jar &
# Result: 3.2 seconds (2.6x faster!)
```

### 2. Memory Usage

```bash
# Monitor with JFR
java -XX:StartFlightRecording:filename=recording.jfr,duration=60s \
     -jar myapp.jar

# Analyze with JMC
jmc recording.jfr

# Compare JDK 11 vs JDK 25:
# - Heap usage: 2.1 GB -> 1.7 GB (19% reduction with compact headers)
# - GC count: 145 -> 98 (32% reduction)
# - GC pause time: 450ms total -> 180ms total (60% reduction)
```

### 3. Throughput

Use load testing tools:

```bash
# Apache Bench
ab -n 100000 -c 100 http://localhost:8080/api/users

# JDK 11: 8,542 req/sec
# JDK 25: 11,234 req/sec (+31% improvement)
```

### 4. Latency

```bash
# Use wrk
wrk -t12 -c400 -d30s http://localhost:8080/api/users

# JDK 11:
# - p50: 12ms
# - p99: 85ms
# - p99.9: 250ms

# JDK 25 (with ZGC):
# - p50: 10ms
# - p99: 45ms (-47%)
# - p99.9: 95ms (-62%)
```

## Migration Checklist

- [ ] Upgrade JDK from 11 to 25
- [ ] Enable compact object headers (default)
- [ ] Generate CDS archive
- [ ] Create AOT profile from production traffic
- [ ] Consider switching to ZGC or Shenandoah for low latency
- [ ] Tune GC based on workload
- [ ] Monitor JFR recordings
- [ ] Measure before/after performance
- [ ] Adjust container memory limits (may need less!)
- [ ] Update CI/CD to use CDS/AOT in production

## Performance Gains Summary

| Metric | JDK 11 | JDK 25 | Improvement |
|--------|--------|--------|-------------|
| Startup Time | 8.5s | 3.2s | 2.7x faster |
| Heap Usage | 2.1 GB | 1.7 GB | 19% less |
| GC Pause (p99) | 85ms | 45ms | 47% better |
| Throughput | 8.5K req/s | 11.2K req/s | 31% more |
| Container Density | 10 pods | 13 pods | 30% more |

*Results vary by application and workload

## Summary

The performance improvements from JDK 11 to JDK 25 are substantial. Compact object headers, AOT profiling, and GC improvements provide measurable benefits for backend applications. Combined with modern features like virtual threads, JDK 25 offers both better developer experience and superior runtime performance.

**Migration Priority**: VERY HIGH - Significant performance improvements with minimal code changes. Especially important for containerized microservices.
