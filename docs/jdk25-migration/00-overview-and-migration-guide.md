# JDK 11 to JDK 25 Migration Guide - Overview

## Executive Summary

JDK 25, released in September 2025, is a **Long-Term Support (LTS)** release and represents 7 years of evolution from JDK 11 (the previous LTS many teams are on). This migration brings transformative improvements:

- **Language Features**: Records, sealed classes, pattern matching, text blocks
- **Concurrency**: Virtual threads enabling million-request scalability
- **Performance**: 10-20% heap reduction, 2-3x faster startup, better GC
- **Native Interop**: Modern FFM API replacing JNI
- **Developer Experience**: Less boilerplate, more expressive code

**ROI**: Most teams see 20-40% performance improvements and 30-50% code reduction in key areas.

## What Changed: Quick Reference

### Language Features

| Feature | JDK Version | Impact | Priority |
|---------|-------------|--------|----------|
| [Records](./01-records.md) | 16 (final) | Eliminates DTO boilerplate | ⭐⭐⭐ HIGH |
| [Sealed Classes](./02-sealed-classes.md) | 17 (final) | Controlled inheritance, exhaustive switches | ⭐⭐ MEDIUM |
| [Pattern Matching instanceof](./03-pattern-matching-instanceof.md) | 16 (final) | Cleaner type checks | ⭐⭐⭐ HIGH |
| [Pattern Matching switch](./04-pattern-matching-switch.md) | 21 (final) | Powerful polymorphic dispatch | ⭐⭐⭐ HIGH |
| [Text Blocks](./05-text-blocks.md) | 15 (final) | Multi-line strings | ⭐⭐⭐ HIGH |
| [Flexible Constructor Bodies](./08-flexible-constructor-bodies.md) | 25 (final) | Validation before super() | ⭐ LOW |

### Concurrency & Performance

| Feature | JDK Version | Impact | Priority |
|---------|-------------|--------|----------|
| [Virtual Threads](./06-virtual-threads.md) | 21 (final) | Million-request scalability | ⭐⭐⭐ VERY HIGH |
| [Structured Concurrency](./06-virtual-threads.md#structured-concurrency) | Preview | Safer concurrent code | ⭐⭐ MEDIUM |
| [Compact Object Headers](./09-performance-improvements.md#compact-object-headers) | 25 | 10-20% heap reduction | ⭐⭐⭐ VERY HIGH |
| [AOT Method Profiling](./09-performance-improvements.md#aot-method-profiling) | 25 | 2-3x faster startup | ⭐⭐⭐ VERY HIGH |
| [Generational ZGC/Shenandoah](./09-performance-improvements.md#garbage-collection) | 21-25 | Sub-millisecond GC pauses | ⭐⭐⭐ HIGH |

### APIs & Libraries

| Feature | JDK Version | Impact | Priority |
|---------|-------------|--------|----------|
| [Foreign Function & Memory API](./07-foreign-function-memory-api.md) | 22 (final) | Modern native interop | ⭐⭐ MEDIUM* |
| HTTP/2 Client | 11 | Better API performance | ⭐⭐⭐ HIGH |
| Vector API | Incubator | SIMD operations | ⭐ LOW* |

*Only if you need native interop or SIMD

## Migration Phases

### Phase 1: Foundation (Weeks 1-2)

**Goal**: Upgrade JDK, measure baseline, fix breaking changes

1. **Upgrade Development Environments**
   ```bash
   # Download JDK 25
   sdk install java 25-open
   sdk use java 25-open
   ```

2. **Update Build Configuration**
   ```gradle
   // build.gradle
   java {
       toolchain {
           languageVersion = JavaLanguageVersion.of(25)
       }
   }
   ```

   ```xml
   <!-- pom.xml -->
   <properties>
       <maven.compiler.source>25</maven.compiler.source>
       <maven.compiler.target>25</maven.compiler.target>
   </properties>
   ```

3. **Fix Deprecated API Usage**
   - Review deprecation warnings
   - Update library dependencies
   - Check for removed APIs

4. **Establish Baseline Metrics**
   ```bash
   # Measure before migration
   - Startup time
   - Memory usage (heap, metaspace)
   - Throughput (requests/sec)
   - Latency (p50, p95, p99)
   - GC behavior (count, pause times)
   ```

### Phase 2: Low-Hanging Fruit (Weeks 3-4)

**Goal**: Apply safe, high-value improvements

1. **Text Blocks** (automated with IDE)
   - Convert SQL queries
   - Convert JSON/XML templates
   - Convert multi-line strings

2. **Pattern Matching instanceof** (automated with IDE)
   - IDE can auto-convert
   - Review and test

3. **Records for DTOs**
   - Start with API request/response objects
   - Convert simple value objects
   - Update tests

4. **Enable Performance Features**
   ```bash
   # Compact Object Headers (automatic)
   # Create CDS archive
   java -XX:ArchiveClassesAtExit=app.jsa -jar app.jar

   # Generate AOT profile
   java -XX:AOTMode=record -XX:AOTConfiguration=app.aotconf -jar app.jar

   # Use in production
   java -XX:SharedArchiveFile=app.jsa \
        -XX:AOTMode=on -XX:AOTConfiguration=app.aotconf \
        -jar app.jar
   ```

**Expected Gains**:
- 30% less boilerplate code
- 20-30% faster startup
- 10-15% heap reduction

### Phase 3: Virtual Threads (Weeks 5-6)

**Goal**: Enable virtual threads for I/O-heavy operations

1. **Spring Boot Configuration**
   ```properties
   # application.properties
   spring.threads.virtual.enabled=true
   ```

2. **Update Executors**
   ```java
   // Before
   ExecutorService executor = Executors.newFixedThreadPool(200);

   // After
   ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
   ```

3. **Adjust Connection Pools**
   ```properties
   # Can increase with virtual threads
   spring.datasource.hikari.maximum-pool-size=500
   ```

4. **Replace synchronized with ReentrantLock**
   ```java
   // Before
   synchronized(lock) {
       // blocking I/O
   }

   // After
   Lock lock = new ReentrantLock();
   lock.lock();
   try {
       // blocking I/O
   } finally {
       lock.unlock();
   }
   ```

5. **Monitor Thread Pinning**
   ```bash
   java -Djdk.tracePinnedThreads=full -jar app.jar
   ```

**Expected Gains**:
- 5-10x more concurrent requests
- 50-70% better resource utilization
- Simpler code (no reactive needed)

### Phase 4: Advanced Features (Weeks 7-8)

**Goal**: Leverage pattern matching and sealed types

1. **Pattern Matching switch**
   - Refactor if-else chains
   - Use with sealed types
   - Implement exhaustive switches

2. **Sealed Classes**
   - Model domain with sealed hierarchies
   - API response types
   - Command/Event patterns

3. **Combine Records + Sealed + Pattern Matching**
   ```java
   public sealed interface ApiResponse<T> permits Success, Error {}
   public record Success<T>(T data) implements ApiResponse<T> {}
   public record Error<T>(String message, int code) implements ApiResponse<T> {}

   public ResponseEntity<?> handle(ApiResponse<User> response) {
       return switch (response) {
           case Success<User>(User user) -> ResponseEntity.ok(user);
           case Error<User>(String msg, int code) ->
               ResponseEntity.status(code).body(Map.of("error", msg));
       };
   }
   ```

**Expected Gains**:
- 40% less error-prone code
- Compiler-verified exhaustiveness
- Better domain modeling

### Phase 5: GC Optimization (Week 9)

**Goal**: Tune garbage collection for workload

1. **Choose GC Based on Requirements**

   **High Throughput** (batch processing):
   ```bash
   -XX:+UseParallelGC
   ```

   **Low Latency** (trading, real-time):
   ```bash
   -XX:+UseShenandoahGC -XX:ShenandoahGCMode=generational
   # OR
   -XX:+UseZGC -XX:+ZGenerational
   ```

   **Balanced** (most web services):
   ```bash
   -XX:+UseG1GC  # Default, usually best
   ```

2. **Tune Settings**
   ```bash
   # Example: Low-latency service
   java -XX:+UseZGC \
        -XX:+ZGenerational \
        -Xms8g -Xmx8g \
        -XX:+AlwaysPreTouch \
        -jar app.jar
   ```

3. **Monitor with JFR**
   ```bash
   java -XX:StartFlightRecording:filename=recording.jfr,duration=300s \
        -jar app.jar
   ```

**Expected Gains**:
- 50-70% lower GC pause times
- 20-30% better throughput
- More consistent latency

### Phase 6: Production Rollout (Week 10+)

**Goal**: Safe production deployment

1. **Canary Deployment**
   - Deploy to 5% of traffic
   - Monitor metrics
   - Gradually increase

2. **Monitor Key Metrics**
   - JVM metrics (heap, GC, threads)
   - Application metrics (latency, throughput, errors)
   - System metrics (CPU, memory, network)

3. **Rollback Plan**
   - Keep JDK 11 deployment ready
   - Quick rollback procedure
   - Clear rollback criteria

4. **Optimize Based on Production Data**
   - Collect JFR recordings
   - Analyze bottlenecks
   - Fine-tune JVM flags

## Breaking Changes to Watch

### 1. Removed APIs

- **Nashorn JavaScript Engine** (removed JDK 15)
  - Migration: Use GraalVM JavaScript

- **Pack200 Tools** (removed JDK 14)
  - Migration: Use standard JAR compression

- **32-bit x86 Port** (removed JDK 25)
  - Migration: Use 64-bit systems

- **Graal JIT Compiler** (removed JDK 25)
  - Migration: Use default JIT (C2)

### 2. Deprecated Features

- **Finalization** (deprecated for removal)
  ```java
  // ✗ BAD - Don't use
  @Override
  protected void finalize() throws Throwable {
      cleanup();
  }

  // ✓ GOOD - Use try-with-resources
  try (Resource resource = acquireResource()) {
      // use resource
  }
  ```

- **Security Manager** (deprecated for removal)
  - Migration: Use OS-level security, containers

### 3. Behavior Changes

- **Stronger Encapsulation**
  - Internal JDK APIs hidden by default
  - Use `--add-opens` if absolutely necessary (not recommended)

- **Module System**
  - Some libraries may need updates
  - Check compatibility

## Framework Compatibility

### Spring Boot

- **Minimum Version**: 3.2+ for full JDK 21-25 support
- **Virtual Threads**: 3.2+ with `spring.threads.virtual.enabled=true`
- **Records**: Full support
- **Sealed Classes**: Full support

```gradle
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-parent:3.3.0'
```

### Jakarta EE

- **Minimum Version**: Jakarta EE 10+
- **Application Servers**:
  - WildFly 27+
  - Payara 6+
  - TomEE 9+

### Micronaut

- **Minimum Version**: 4.0+ for JDK 21-25
- **Virtual Threads**: Native support in 4.0+

### Quarkus

- **Minimum Version**: 3.2+ for JDK 21-25
- **Virtual Threads**: `quarkus.virtual-threads.enabled=true`

## Testing Strategy

### 1. Unit Tests

- Run full test suite on JDK 25
- Fix any compilation errors
- Verify behavior unchanged

### 2. Integration Tests

- Test database connections
- Test external API calls
- Test message queues
- Test caching

### 3. Performance Tests

```bash
# Load testing
ab -n 100000 -c 100 http://localhost:8080/api/endpoint

# Stress testing
wrk -t12 -c400 -d30s http://localhost:8080/api/endpoint

# Endurance testing
# Run for 24+ hours monitoring memory/GC
```

### 4. Compatibility Tests

- Test all dependencies
- Verify framework compatibility
- Check library versions

## Code Examples: Before & After

### Simple DTO

**Before (JDK 11)**:
```java
public class UserResponse {
    private final Long id;
    private final String username;
    private final String email;

    public UserResponse(Long id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserResponse that = (UserResponse) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(username, that.username) &&
               Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, email);
    }

    @Override
    public String toString() {
        return "UserResponse{id=" + id + ", username='" + username +
               "', email='" + email + "'}";
    }
}
```

**After (JDK 25)**:
```java
public record UserResponse(Long id, String username, String email) {}
```

**Result**: 95% less code, same functionality!

### Polymorphic API Response

**Before (JDK 11)**:
```java
public ResponseEntity<?> getUser(Long id) {
    try {
        User user = userService.findById(id);
        if (user != null) {
            return ResponseEntity.ok(toDto(user));
        } else {
            return ResponseEntity.notFound().build();
        }
    } catch (DatabaseException e) {
        return ResponseEntity.status(503)
            .body(Map.of("error", "Database unavailable"));
    } catch (Exception e) {
        return ResponseEntity.status(500)
            .body(Map.of("error", "Internal error"));
    }
}
```

**After (JDK 25)**:
```java
public sealed interface QueryResult<T> permits Found, NotFound, Error {}
public record Found<T>(T value) implements QueryResult<T> {}
public record NotFound<T>() implements QueryResult<T> {}
public record Error<T>(String message, int code) implements QueryResult<T> {}

public ResponseEntity<?> getUser(Long id) {
    QueryResult<User> result = userService.findById(id);

    return switch (result) {
        case Found<User>(User user) -> ResponseEntity.ok(toDto(user));
        case NotFound<User>() -> ResponseEntity.notFound().build();
        case Error<User>(String msg, int code) ->
            ResponseEntity.status(code).body(Map.of("error", msg));
    };
}
```

**Result**: Type-safe, exhaustive, no missed error cases!

### Concurrent I/O

**Before (JDK 11)**:
```java
ExecutorService executor = Executors.newFixedThreadPool(100);

public UserData fetchUserData(String userId) {
    Future<Profile> profileFuture = executor.submit(() ->
        profileService.fetch(userId));
    Future<Orders> ordersFuture = executor.submit(() ->
        orderService.fetch(userId));
    Future<Payments> paymentsFuture = executor.submit(() ->
        paymentService.fetch(userId));

    try {
        return new UserData(
            profileFuture.get(),
            ordersFuture.get(),
            paymentsFuture.get()
        );
    } catch (Exception e) {
        throw new RuntimeException(e);
    }
}
```

**After (JDK 25 with Virtual Threads)**:
```java
public UserData fetchUserData(String userId) throws InterruptedException {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        Future<Profile> profileFuture = executor.submit(() ->
            profileService.fetch(userId));
        Future<Orders> ordersFuture = executor.submit(() ->
            orderService.fetch(userId));
        Future<Payments> paymentsFuture = executor.submit(() ->
            paymentService.fetch(userId));

        return new UserData(
            profileFuture.get(),
            ordersFuture.get(),
            paymentsFuture.get()
        );
    } catch (ExecutionException e) {
        throw new RuntimeException(e);
    }
}
```

**Result**: Can handle millions of concurrent operations!

## ROI Estimation

Based on typical backend application:

| Metric | JDK 11 | JDK 25 | Improvement | Business Impact |
|--------|--------|--------|-------------|-----------------|
| Lines of Code | 100,000 | 70,000 | -30% | Faster development, less bugs |
| Startup Time | 12s | 4s | 3x faster | Faster deployments, better CI/CD |
| Memory Usage | 4GB | 3.2GB | -20% | 25% more pods in same cluster |
| Throughput | 10K req/s | 13K req/s | +30% | Handle more traffic, delay scaling |
| p99 Latency | 200ms | 100ms | -50% | Better user experience |
| Infrastructure Cost | $10K/mo | $7.5K/mo | -25% | Direct cost savings |

**Total Estimated Savings**: 20-30% infrastructure cost + improved developer productivity

## Common Pitfalls

1. **Not testing thoroughly** - Always test in staging first
2. **Ignoring thread pinning** - Monitor with `-Djdk.tracePinnedThreads=full`
3. **Over-using ThreadLocal** - Expensive with virtual threads
4. **Not generating CDS/AOT** - Missing major performance gains
5. **Wrong GC for workload** - Profile and choose appropriately
6. **Forgetting dependency updates** - Update frameworks/libraries

## Resources

- [JDK 25 Release Notes](https://www.oracle.com/java/technologies/javase/25-relnote-issues.html)
- [OpenJDK](https://openjdk.org/)
- [Inside Java Podcast](https://inside.java/)
- [Java Enhancement Proposals](https://openjdk.org/jeps/0)

## Summary

Migrating from JDK 11 to JDK 25 is a high-value investment:

✅ **Performance**: 20-40% improvements across the board
✅ **Productivity**: 30-50% less boilerplate code
✅ **Scalability**: Virtual threads enable million-request apps
✅ **Cost**: 20-30% infrastructure savings
✅ **Future-Proof**: LTS support through 2033

**Recommended Timeline**: 10-12 weeks for full migration and optimization

**Start Now**: Begin with Phase 1 (foundation) this sprint!

---

**Next Steps**:
1. Read detailed feature guides in this directory
2. Set up JDK 25 development environment
3. Create migration plan for your team
4. Start with Phase 1: Foundation
