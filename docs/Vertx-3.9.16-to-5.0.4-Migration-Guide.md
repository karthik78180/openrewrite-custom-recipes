# Vert.x 3.9.16 to 5.0.4 Comprehensive Migration Guide

## Executive Summary

This document provides a detailed migration guide for upgrading from **Vert.x 3.9.16 to 5.0.4**. This is a **major version migration** spanning two breaking releases (3.x → 4.x → 5.x), requiring significant code changes and architectural updates.

### Migration Complexity: HIGH

**Estimated Effort:**
- Small projects (< 10k LOC): 2-4 weeks
- Medium projects (10k-50k LOC): 1-2 months
- Large projects (> 50k LOC): 2-4 months

**Risk Level:** HIGH - Breaking changes across all core modules

**Recommended Approach:** Incremental migration (3.x → 4.x → 5.x) with comprehensive testing at each stage.

---

## Table of Contents

1. [Dependency Changes](#1-dependency-changes)
2. [Package & Import Changes](#2-package--import-changes)
3. [Core API Changes](#3-core-api-changes)
4. [HTTP Client & Server Changes](#4-http-client--server-changes)
5. [Database Client Changes](#5-database-client-changes)
6. [Async Programming Model Changes](#6-async-programming-model-changes)
7. [Module-Specific Changes](#7-module-specific-changes)
8. [Migration Strategy](#8-migration-strategy)
9. [Testing & Validation](#9-testing--validation)
10. [Breaking Changes Quick Reference](#10-breaking-changes-quick-reference)

---

## 1. Dependency Changes

### 1.1 Core Dependencies (Gradle)

The dependency structure in Vert.x has undergone significant changes between versions 3.9.16 and 5.0.4. The most notable change is the consolidation of SQL-related dependencies and the shift of Jackson from a required to an optional dependency.

#### Before (3.9.16)
```gradle
dependencies {
    // Core Vert.x runtime and event loop
    implementation 'io.vertx:vertx-core:3.9.16'

    // Web framework for HTTP servers and routing
    implementation 'io.vertx:vertx-web:3.9.16'

    // JDBC client for database connectivity
    implementation 'io.vertx:vertx-jdbc-client:3.9.16'

    // Common SQL interfaces (REQUIRED in 3.x)
    implementation 'io.vertx:vertx-sql-common:3.9.16'

    // Jackson is automatically included as a transitive dependency
    // No need to explicitly declare it
}
```

#### After (5.0.4)
```gradle
dependencies {
    // Core Vert.x runtime with updated async model
    implementation 'io.vertx:vertx-core:5.0.4'

    // Web framework with improved security and performance
    implementation 'io.vertx:vertx-web:5.0.4'

    // JDBC client - now includes SQL common interfaces
    implementation 'io.vertx:vertx-jdbc-client:5.0.4'

    // vertx-sql-common REMOVED - merged into vertx-jdbc-client in 4.x
    // All SQL common interfaces are now part of vertx-sqlclient

    // Jackson Databind - now OPTIONAL (required in 3.x, optional in 4.x+)
    // MUST add explicitly if using JsonObject.mapFrom() or JsonObject.mapTo()
    // or if using JSON serialization/deserialization
    implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.2'
}
```

**Key Changes Explained:**

1. **vertx-sql-common removal**: In Vert.x 4.x, the SQL common interfaces were consolidated into the new `vertx-sqlclient` module. The old `vertx-sql-common` is no longer needed and will cause conflicts if included.

2. **Jackson Databind now optional**: Vert.x 4.x+ made Jackson optional to:
   - Reduce the core dependency footprint
   - Allow users to choose alternative JSON libraries (Gson, Moshi, etc.)
   - Improve startup time for applications not using JSON mapping
   - **Important**: If you use `JsonObject.mapFrom()`, `JsonObject.mapTo()`, or any JSON serialization, you MUST explicitly add Jackson to your dependencies.

3. **Version alignment**: Ensure all Vert.x modules use the same version (5.0.4) to avoid compatibility issues.

### 1.2 Removed Dependencies (Complete Removal)

These dependencies have been **completely removed** from Vert.x and have no direct replacement. Understanding why they were removed helps plan your migration strategy.

| Dependency | Removed In | Alternative | Migration Effort |
|------------|------------|-------------|------------------|
| `io.vertx:vertx-sql-common` | **4.0** | Functionality merged into `vertx-jdbc-client` | **LOW** - Automatic via dependency updates |
| `io.vertx:vertx-sync` | **5.0** | Use Virtual Threads (Java 21+) or stay with reactive patterns | **HIGH** - Requires code rewrite |
| `io.vertx:vertx-service-factory` | **5.0** | Use standard Verticle deployment | **MEDIUM** - Change deployment patterns |
| `io.vertx:vertx-maven-service-factory` | **5.0** | Use standard Maven dependency management | **LOW** - Update build configuration |
| `io.vertx:vertx-http-service-factory` | **5.0** | Use standard HTTP-based deployment | **MEDIUM** - Change deployment approach |
| Vert.x CLI (`vertx` command-line tool) | **5.0** | Use Maven/Gradle plugins or `VertxApplication` | **LOW** - Update scripts |

**Detailed Explanations:**

1. **vertx-sql-common (Removed in 4.0)**:
   - **Why**: SQL interfaces were split across `vertx-sql-common` (callbacks) and new `vertx-sqlclient` (futures). To reduce confusion, common interfaces were consolidated.
   - **Impact**: If you're using `SQLClient`, `SQLConnection`, or `ResultSet`, these types have moved to new packages.
   - **Action**: Update imports from `io.vertx.ext.sql.*` to `io.vertx.sqlclient.*`

2. **vertx-sync (Removed in 5.0)**:
   - **Why**: Vert.x Sync provided fiber-based synchronous-looking code using Quasar. With Java 21's Virtual Threads (Project Loom), fibers are obsolete.
   - **Impact**: If you used `Sync.awaitResult()` or `Sync.awaitEvent()`, you need to rewrite using async patterns or Virtual Threads.
   - **Action**:
     - **Option 1** (Java 21+): Use Virtual Threads with `Future.await()` (requires Vert.x 5.x)
     - **Option 2**: Rewrite using reactive Future composition patterns
   - **Example Migration**:
     ```java
     // Before (3.x with vertx-sync)
     String result = Sync.awaitResult(asyncOperation());

     // After (5.x with Virtual Threads - Java 21+)
     String result = asyncOperation().await();

     // After (5.x with reactive patterns)
     asyncOperation()
         .onSuccess(result -> {
             // Use result
         });
     ```

3. **Service Factories (Removed in 5.0)**:
   - **Why**: Service factories added complexity without significant benefits. Modern build tools and container orchestration (Docker, Kubernetes) provide better solutions.
   - **Impact**: If you deployed verticles using `vertx:` prefix URIs, change to standard deployment.
   - **Action**: Use `Vertx.deployVerticle(new MyVerticle())` or deployment descriptors.

4. **Vert.x CLI (Removed in 5.0)**:
   - **Why**: The `vertx` command-line tool was rarely used and difficult to maintain. Build plugins provide better integration.
   - **Impact**: If you used `vertx run` command, change to Gradle/Maven plugins or programmatic deployment.
   - **Action**: Use Gradle application plugin or Maven exec plugin.

**Important:** Jackson Databind (`com.fasterxml.jackson.core:jackson-databind`) changed from:
- **3.x**: Transitive dependency (included automatically)
- **4.x+**: Optional dependency (must add explicitly if needed)
- **Why**: Allows users to choose alternative JSON libraries and reduces core dependencies
- **Impact**: If you use `JsonObject.mapFrom()` or `JsonObject.mapTo()`, you'll get `NoClassDefFoundError` without explicit Jackson dependency
- **Action**: Add `implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.2'` to your build.gradle

### 1.3 Deprecated/Sunset Components (Still Available but Discouraged)

These components are still supported in 5.x but are **actively discouraged** and will likely be removed in Vert.x 6.x. Plan your migration now to avoid breaking changes in the future.

| Component | Status | Migration Path | Urgency | Complexity |
|-----------|--------|----------------|---------|------------|
| gRPC Netty (`vertx-grpc`) | **Sunset** | Migrate to Vert.x gRPC client/server | **HIGH** | **MEDIUM** |
| RxJava 2 (`vertx-rx-java2`) | **Sunset** | Migrate to Mutiny or RxJava 3 | **MEDIUM** | **LOW** |
| OpenTracing (`vertx-opentracing`) | **Sunset** | Migrate to OpenTelemetry | **HIGH** | **MEDIUM** |
| Vert.x Unit (`vertx-unit`) | **Sunset** | Migrate to JUnit 5 with `vertx-junit5` | **LOW** | **LOW** |

**Detailed Migration Guidance:**

1. **gRPC Netty (vertx-grpc) - HIGH URGENCY**:
   - **Why Deprecated**: The old `vertx-grpc` used a fork of gRPC compiler. Vert.x now supports official gRPC with better integration.
   - **Timeline**: Expected removal in Vert.x 6.0 (2025-2026)
   - **Migration Steps**:
     1. Replace `io.vertx:vertx-grpc` with `io.vertx:vertx-grpc-client` and `io.vertx:vertx-grpc-server`
     2. Update protobuf compiler to use official `io.grpc:protoc-gen-grpc-java` + `vertx-grpc-protoc-plugin`
     3. Update service implementations from `Promise<T>` to `Future<T>` return types
     4. Update generated code references (classes now have `Vertx` prefix)
   - **Benefits**: Better performance, official gRPC compatibility, streaming support

2. **RxJava 2 (vertx-rx-java2) - MEDIUM URGENCY**:
   - **Why Deprecated**: RxJava 2 reached end-of-life. RxJava 3 is the current version, and Mutiny is the recommended reactive library for Vert.x.
   - **Timeline**: Expected removal in Vert.x 6.0
   - **Migration Options**:
     - **Option A** (Recommended): Migrate to **Mutiny** (`smallrye-mutiny-vertx-bindings`)
       - Modern reactive library designed for Vert.x
       - Better integration with Quarkus and MicroProfile
       - Simpler API than RxJava
     - **Option B**: Migrate to **RxJava 3** (`vertx-rx-java3`)
       - If you have extensive RxJava experience
       - Similar API to RxJava 2 (easier migration)
   - **Migration Effort**: LOW - Most operators have direct equivalents
   - **Example**:
     ```java
     // Before (RxJava 2)
     import io.vertx.reactivex.core.Vertx;

     // After (RxJava 3)
     import io.vertx.rxjava3.core.Vertx;

     // After (Mutiny - recommended)
     import io.smallrye.mutiny.vertx.core.AbstractVerticle;
     ```

3. **OpenTracing (vertx-opentracing) - HIGH URGENCY**:
   - **Why Deprecated**: OpenTracing merged with OpenCensus to form OpenTelemetry. OpenTracing is no longer maintained.
   - **Timeline**: OpenTracing project archived. Expected removal from Vert.x in 6.0
   - **Migration Steps**:
     1. Replace `io.vertx:vertx-opentracing` with `io.vertx:vertx-opentelemetry`
     2. Update tracing configuration from OpenTracing API to OpenTelemetry SDK
     3. Update span creation and context propagation
     4. Update exporters (Jaeger, Zipkin now support OpenTelemetry)
   - **Benefits**: Active development, better performance, standardized across cloud-native ecosystem
   - **Impact**: MEDIUM - API changes require code updates but concepts are similar

4. **Vert.x Unit (vertx-unit) - LOW URGENCY**:
   - **Why Deprecated**: JUnit 5 with `vertx-junit5` provides better integration and more features
   - **Timeline**: Still functional, no hard deadline for removal
   - **Migration Steps**:
     1. Replace `io.vertx:vertx-unit` with `io.vertx:vertx-junit5`
     2. Change from `@RunWith(VertxUnitRunner.class)` to `@ExtendWith(VertxExtension.class)`
     3. Replace `TestContext` with `VertxTestContext`
     4. Update async test patterns to use `testContext.succeedingThenComplete()`
   - **Benefits**: Better async test support, checkpoint system, cleaner API
   - **Migration Effort**: LOW - Straightforward API mapping

### 1.4 Module Replacements (Different Artifact)

These modules were renamed or completely rewritten with different Maven/Gradle coordinates. You must change your dependency declarations and update your code.

| Old Module (3.x) | New Module (5.x) | Change Type | Migration Effort |
|------------------|------------------|-------------|------------------|
| `vertx-web-api-contract` | `vertx-web-openapi` | Complete rewrite with new API | **HIGH** |
| `vertx-rx-java` (RxJava 1) | `vertx-rx-java3` | RxJava 1 & 2 removed, only RxJava 3 supported | **LOW** |
| `vertx-lang-kotlin-coroutines` | Built-in Kotlin coroutines | Generated suspending extensions removed | **MEDIUM** |

**Detailed Migration for Each Module:**

1. **vertx-web-api-contract → vertx-web-openapi**:
   - **What Changed**: Complete rewrite to support OpenAPI 3.x specification (3.x only supported Swagger 2.0)
   - **Dependency Change**:
     ```gradle
     // Before
     implementation 'io.vertx:vertx-web-api-contract:3.9.16'

     // After
     implementation 'io.vertx:vertx-web-openapi:5.0.4'
     ```
   - **API Changes**:
     - `OpenAPI3RouterFactory` replaces `RouterFactory`
     - New validation model
     - Improved request/response validation
   - **Migration Effort**: HIGH - Requires rewriting OpenAPI integration code

2. **vertx-rx-java (RxJava 1) → vertx-rx-java3**:
   - **What Changed**: RxJava 1 and 2 are no longer supported. Only RxJava 3 is available.
   - **Dependency Change**:
     ```gradle
     // Before
     implementation 'io.vertx:vertx-rx-java:3.9.16'  // RxJava 1

     // After
     implementation 'io.vertx:vertx-rx-java3:5.0.4'  // RxJava 3
     ```
   - **Package Changes**: `io.vertx.reactivex.*` → `io.vertx.rxjava3.*`
   - **Migration Effort**: LOW - API is similar, mostly import changes

3. **vertx-lang-kotlin-coroutines Changes**:
   - **What Changed**: Generated suspending extension functions removed. Use Kotlin's built-in coroutine support.
   - **Before (3.x)**:
     ```kotlin
     // Used generated `xxxAwait()` functions
     val result = someAsyncOperation().await()
     ```
   - **After (5.x)**:
     ```kotlin
     // Use Future.await() directly with Kotlin coroutines
     val result = someAsyncOperation().await()
     ```
   - **Migration Effort**: MEDIUM - Update all `xxxAwait()` calls to use Future.await()

### 1.5 Updated Dependencies (Version Changes with Breaking Changes)

Third-party libraries that Vert.x depends on have been updated with **breaking changes**. Even if your code doesn't directly use these libraries, you may be affected through Vert.x's usage.

| Library | 3.9.16 Version | 5.0.4 Version | Impact Level | Key Changes | Action Required |
|---------|----------------|---------------|--------------|-------------|-----------------|
| **Netty** | 4.1.x | 4.1.100+ | **LOW** | Mostly internal changes | Review if using Netty directly |
| **Jackson** | 2.11.x | 2.15.x | **MEDIUM** | Serialization behavior changes | Test JSON serialization thoroughly |
| **Hazelcast** | 3.x/4.x | 5.3.2+ | **HIGH** | **Requires Java 11+**, API changes | Update Hazelcast configuration |
| **MongoDB Driver** | 3.x/4.x | 5.x | **HIGH** | Complete API overhaul | Rewrite MongoDB client code |
| **GraphQL-Java** | 15.x | 23.x | **HIGH** | Breaking changes in v20, v22, v23 | Update GraphQL schemas and resolvers |
| **Micrometer** | 1.x | 1.14+ | **MEDIUM** | Metric naming changes | Update metric names in dashboards |
| **PostgreSQL JDBC** | External | Built-in SCRAM | **LOW** | SCRAM auth now built-in | Remove `com.ongres.scram:client` dependency |

**Detailed Impact Analysis:**

1. **Netty 4.1.100+ (LOW Impact)**:
   - **What Changed**: Internal buffer management improvements, HTTP/2 enhancements
   - **Impact**: Minimal unless you use Netty API directly
   - **Action**: If you create `ByteBuf` or use Netty channels directly, test thoroughly

2. **Jackson 2.15.x (MEDIUM Impact)**:
   - **What Changed**:
     - Stricter deserialization security
     - Changed default behavior for polymorphic types
     - Updated date/time handling
   - **Impact**: JSON serialization may behave differently
   - **Actions**:
     - Test all `JsonObject.mapFrom()` and `JsonObject.mapTo()` calls
     - Review custom Jackson modules and deserializers
     - Check date/time serialization formats
   - **Common Issues**:
     ```java
     // May need explicit configuration for polymorphic types
     @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
     public abstract class BaseClass { }
     ```

3. **Hazelcast 5.3.2+ (HIGH Impact)**:
   - **What Changed**: Complete API redesign, Java 11+ required
   - **Impact**: If using `vertx-hazelcast` for clustering, significant changes required
   - **Actions**:
     1. Update Hazelcast configuration XML/YAML
     2. Review cluster discovery settings
     3. Update serialization configuration
     4. Test cluster formation and failover
   - **Migration Effort**: 2-4 weeks for complex deployments

4. **MongoDB Driver 5.x (HIGH Impact)**:
   - **What Changed**: Reactive Streams API, new connection string format
   - **Impact**: All MongoDB queries need review
   - **Actions**:
     1. Update connection strings (mongodb:// format changes)
     2. Review reactive operations
     3. Update aggregation pipelines
     4. Test transactions
   - **Migration Effort**: 1-3 weeks depending on MongoDB usage

5. **GraphQL-Java 23.x (HIGH Impact)**:
   - **What Changed**: DataLoader improvements, schema validation changes
   - **Impact**: GraphQL schema and resolver updates needed
   - **Actions**:
     1. Update schema definitions
     2. Review data fetchers
     3. Update DataLoader patterns
     4. Test all GraphQL queries
   - **Breaking Changes**: Some deprecated APIs removed

6. **Micrometer 1.14+ (MEDIUM Impact)**:
   - **What Changed**: Metric naming conventions, registry changes
   - **Impact**: Metric names in monitoring dashboards may need updates
   - **Actions**:
     1. Review metric naming (may have different prefixes)
     2. Update Grafana/Prometheus dashboards
     3. Test metric collection
   - **Migration Effort**: Few hours to update dashboards

7. **PostgreSQL JDBC - SCRAM Auth (LOW Impact)**:
   - **What Changed**: SCRAM-SHA-256 authentication now built-in
   - **Impact**: Can remove external SCRAM dependency
   - **Action**:
     ```gradle
     // Before - needed external dependency
     implementation 'com.ongres.scram:client:2.1'

     // After - SCRAM built-in, remove dependency
     // (no dependency needed)
     ```

---

## 2. Package & Import Changes

Major package reorganizations occurred between Vert.x 3.x and 5.x. These changes reflect architectural improvements and consolidation of functionality.

### 2.1 JDBC & SQL Client Packages

The SQL client architecture was completely redesigned in Vert.x 4.x to provide a unified reactive SQL client API. This is one of the most significant package changes you'll encounter.

#### Before (3.9.16)
```java
// Old callback-based JDBC client
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;

// Example usage
JDBCClient client = JDBCClient.createShared(vertx, config);
client.getConnection(ar -> {
    if (ar.succeeded()) {
        SQLConnection connection = ar.result();
        connection.query("SELECT * FROM users", res -> {
            if (res.succeeded()) {
                ResultSet rs = res.result();
                List<JsonObject> rows = rs.getRows();
            }
        });
    }
});
```

#### After (5.0.4)
```java
// New future-based SQL client with connection pooling
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
// ResultSet and UpdateResult replaced by RowSet<Row>

// Example usage
JDBCPool pool = JDBCPool.pool(vertx, connectOptions, poolOptions);
pool.query("SELECT * FROM users")
    .execute()
    .onSuccess(rows -> {
        for (Row row : rows) {
            String username = row.getString("username");
            Integer age = row.getInteger("age");
        }
    });
```

**Key Package Changes:**

| Old Package (3.x) | New Package (5.x) | Reason for Change |
|-------------------|-------------------|-------------------|
| `io.vertx.ext.jdbc.JDBCClient` | `io.vertx.jdbcclient.JDBCPool` | Unified SQL client API with connection pooling |
| `io.vertx.ext.sql.SQLClient` | `io.vertx.sqlclient.Pool` | Common interface for all SQL databases |
| `io.vertx.ext.sql.SQLConnection` | `io.vertx.sqlclient.SqlConnection` | Reactive connection API |
| `io.vertx.ext.sql.ResultSet` | `io.vertx.sqlclient.RowSet<Row>` | Type-safe row iteration |
| `io.vertx.ext.sql.UpdateResult` | `io.vertx.sqlclient.RowSet<Row>` | Unified result type |

**Migration Tips:**
1. **ResultSet → RowSet**: The new `RowSet` is iterable and provides type-safe column access
2. **getRows() → iterate**: Instead of `resultSet.getRows()`, iterate directly: `for (Row row : rowSet)`
3. **Connection pooling**: `JDBCPool` manages connections automatically - no need for manual `getConnection()`
4. **Prepared statements**: Use `pool.preparedQuery()` instead of creating prepared statements manually

### 2.2 Jackson JSON Package Changes

Jackson integration was refactored in Vert.x 4.x to make it pluggable and optional.

#### Before (3.9.16)
```java
import io.vertx.core.json.Json;
import com.fasterxml.jackson.databind.ObjectMapper;

// Accessing the ObjectMapper
ObjectMapper mapper = Json.mapper();
ObjectMapper prettyMapper = Json.prettyMapper();

// Encoding/decoding
String json = Json.encode(object);
MyObject obj = Json.decodeValue(json, MyObject.class);
```

#### After (5.0.4)
```java
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.core.json.jackson.JacksonCodec;
import com.fasterxml.jackson.databind.ObjectMapper;

// Accessing the ObjectMapper
ObjectMapper mapper = DatabindCodec.mapper();
ObjectMapper prettyMapper = DatabindCodec.prettyMapper();

// Encoding/decoding - NEW API
String json = JacksonCodec.encodeToString(object);
MyObject obj = JacksonCodec.decodeValue(json, MyObject.class);

// Or use JsonObject methods (still works)
JsonObject jsonObj = JsonObject.mapFrom(object);  // Requires Jackson dependency
MyObject obj = jsonObj.mapTo(MyObject.class);
```

**Key Changes:**

| Old API (3.x) | New API (5.x) | Notes |
|---------------|---------------|-------|
| `Json.mapper()` | `DatabindCodec.mapper()` | Access to ObjectMapper |
| `Json.encode()` | `JacksonCodec.encodeToString()` | Explicit codec name |
| `Json.decode()` | `JacksonCodec.decodeValue()` | Explicit codec name |
| `JsonObject.mapFrom()` | Still works | Requires explicit Jackson dependency |

**Why This Changed:**
- **Modularity**: Jackson is now pluggable - you can replace it with other JSON libraries
- **Clarity**: Explicit codec names make it clear which JSON library is being used
- **Optional dependency**: Jackson is no longer a required transitive dependency

**Migration Strategy:**
1. **Option 1** (Simple): Just update imports from `Json` to `DatabindCodec`/`JacksonCodec`
2. **Option 2** (Recommended): Use `JsonObject.mapFrom()` and `JsonObject.mapTo()` for better integration with Vert.x
3. Ensure Jackson dependency is explicitly declared in build.gradle:
   ```gradle
   implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.2'
   ```

---

## 3. Core API Changes

This section covers the most impactful changes in the Vert.x migration: the async programming model. Understanding these changes is critical for a successful migration.

### 3.1 Future & Async Handling

The async handling API underwent major improvements in Vert.x 4.x, removing deprecated methods and introducing more ergonomic patterns.

**Key Changes:**
- `Future.setHandler()` → **REMOVED** (use `onComplete()`, `onSuccess()`, `onFailure()`)
- `Future.completer()` → **REMOVED** (Future now implements `Handler<AsyncResult<T>>` directly)
- New methods: `onSuccess()`, `onFailure()` for cleaner error handling
- Better Future composition with `compose()`, `map()`, `flatMap()`

#### Before (3.9.16)
```java
// Using setHandler (REMOVED in 4.x)
Future<String> future = someAsyncOperation();
future.setHandler(ar -> {
    if (ar.succeeded()) {
        String result = ar.result();
        System.out.println("Got: " + result);
    } else {
        Throwable cause = ar.cause();
        System.err.println("Failed: " + cause.getMessage());
    }
});

// Using completer (REMOVED in 4.x)
Promise<String> promise = Promise.promise();
Handler<AsyncResult<String>> handler = promise.completer();
someMethodWithCallback(handler);

// Chaining with setHandler
future1.setHandler(ar1 -> {
    if (ar1.succeeded()) {
        future2.setHandler(ar2 -> {
            if (ar2.succeeded()) {
                // Nested callbacks (callback hell)
            }
        });
    }
});
```

#### After (5.0.4)
```java
// setHandler REMOVED - use onComplete for same behavior
Future<String> future = someAsyncOperation();
future.onComplete(ar -> {
    if (ar.succeeded()) {
        String result = ar.result();
        System.out.println("Got: " + result);
    } else {
        Throwable cause = ar.cause();
        System.err.println("Failed: " + cause.getMessage());
    }
});

// BETTER: Separate success/failure handlers (recommended)
future
    .onSuccess(result -> {
        System.out.println("Success: " + result);
    })
    .onFailure(err -> {
        System.err.println("Failed: " + err.getMessage());
    });

// completer() REMOVED - Promise implements Handler directly
Promise<String> promise = Promise.promise();
someMethodWithCallback(promise);  // Pass promise directly!

// Modern Future composition (NO callback hell)
future1
    .compose(result1 -> future2)
    .compose(result2 -> future3)
    .onSuccess(finalResult -> {
        // Clean, linear flow
    })
    .onFailure(err -> {
        // Single error handler for entire chain
    });
```

**Migration Strategy:**

1. **Global Find/Replace** (use with caution):
   ```
   Find: .setHandler(
   Replace: .onComplete(
   ```

2. **Recommended Approach** - Split into success/failure:
   ```java
   // Before
   future.setHandler(ar -> {
       if (ar.succeeded()) {
           // success logic
       } else {
           // error logic
       }
   });

   // After (cleaner)
   future
       .onSuccess(result -> {
           // success logic
       })
       .onFailure(err -> {
           // error logic
       });
   ```

3. **Update completer() usage**:
   ```java
   // Before
   Promise<String> promise = Promise.promise();
   legacyMethod(promise.completer());

   // After
   Promise<String> promise = Promise.promise();
   legacyMethod(promise);  // Promise IS a Handler now
   ```

**Why These Changes?**
- **Clarity**: `onSuccess()` and `onFailure()` are more explicit than checking `ar.succeeded()`
- **Composition**: Encourages functional composition over nested callbacks
- **Simplicity**: Fewer API methods to remember
- **Java 8+ alignment**: Matches modern Java async patterns (CompletableFuture)

### 3.2 CompositeFuture

#### Before (3.9.16)
```java
List<Future> futures = Arrays.asList(future1, future2, future3);
CompositeFuture.all((List<Future>)futures).setHandler(ar -> {
    if (ar.succeeded()) {
        // All succeeded
    }
});
```

#### After (5.0.4)
```java
// Fully generic with wildcard types
List<Future<?>> futures = Arrays.asList(future1, future2, future3);
Future.all(futures).onComplete(ar -> {
    if (ar.succeeded()) {
        // All succeeded
    }
});

// Or using varargs
Future.all(future1, future2, future3)
    .onSuccess(cf -> {
        // All succeeded
    });
```

### 3.3 Execute Blocking

The `executeBlocking()` API changed from using Promise to using Callable, making it more aligned with standard Java concurrency patterns.

**Key Changes:**
- Signature changed from `Handler<Promise<T>>` to `Callable<T>` (or `Supplier<T>`)
- Direct return instead of `promise.complete()`
- Explicit ordered/unordered execution control
- Returns `Future<T>` instead of requiring a callback

#### Before (3.9.16)
```java
// Promise-based blocking execution
vertx.executeBlocking(promise -> {
    try {
        // Blocking operation
        String result = performDatabaseQuery();  // Blocks thread
        promise.complete(result);
    } catch (Exception e) {
        promise.fail(e);
    }
}, res -> {
    if (res.succeeded()) {
        String result = res.result();
        System.out.println("Result: " + result);
    } else {
        System.err.println("Failed: " + res.cause());
    }
});

// Ordered execution (executes sequentially in order submitted)
vertx.executeBlocking(promise -> {
    promise.complete(blockingOp());
}, true, handler);  // ordered=true (default)

// Unordered execution (can run in parallel)
vertx.executeBlocking(promise -> {
    promise.complete(blockingOp());
}, false, handler);  // ordered=false
```

#### After (5.0.4)
```java
// Callable-based blocking execution - cleaner!
vertx.<String>executeBlocking(() -> {
    // Blocking operation
    return performDatabaseQuery();  // Direct return!
    // Exceptions automatically fail the Future
}).onSuccess(result -> {
    System.out.println("Result: " + result);
}).onFailure(err -> {
    System.err.println("Failed: " + err);
});

// Ordered execution (default: true)
vertx.<String>executeBlocking(() -> blockingOp(), true)
    .onSuccess(result -> {
        // Executes in submission order
    });

// Unordered execution (parallel)
vertx.<String>executeBlocking(() -> blockingOp(), false)
    .onSuccess(result -> {
        // Can run in parallel with other unordered tasks
    });

// Complex example: Exception handling
vertx.<User>executeBlocking(() -> {
    // Exceptions are automatically converted to failed Future
    User user = database.findUserById(123);  // May throw SQLException
    if (user == null) {
        throw new NotFoundException("User not found");
    }
    return user;
}).onSuccess(user -> {
    logger.info("Found user: {}", user.getName());
}).onFailure(err -> {
    if (err instanceof NotFoundException) {
        logger.warn("User not found");
    } else {
        logger.error("Database error", err);
    }
});
```

**Migration Tips:**

1. **Simple conversion:**
   ```java
   // Before
   vertx.executeBlocking(promise -> {
       String result = blockingOp();
       promise.complete(result);
   }, handler);

   // After
   vertx.<String>executeBlocking(() -> {
       return blockingOp();
   }).onComplete(handler);
   ```

2. **Error handling is automatic:**
   ```java
   // Before - manual exception handling
   vertx.executeBlocking(promise -> {
       try {
           String result = riskyOperation();
           promise.complete(result);
       } catch (Exception e) {
           promise.fail(e);
       }
   }, handler);

   // After - exceptions automatically fail Future
   vertx.<String>executeBlocking(() -> {
       return riskyOperation();  // Exceptions handled automatically
   }).onComplete(handler);
   ```

3. **Ordered vs Unordered:**
   - **Ordered (true)**: Tasks execute sequentially in submission order. Use for tasks that must not overlap.
   - **Unordered (false)**: Tasks can run in parallel. Use for independent blocking operations.

**Why This Changed:**
- **Standard Java**: `Callable<T>` is a standard Java interface, more familiar to Java developers
- **Cleaner code**: Direct return is simpler than `promise.complete()`
- **Better exception handling**: Exceptions automatically convert to failed Future
- **Type safety**: Better type inference with explicit type parameter

---

## 4. HTTP Client & Server Changes

The HTTP client API underwent a complete redesign in Vert.x 4.x, moving from a synchronous-style chaining API to a fully async Future-based API. This is one of the most visible changes you'll encounter.

**Major Breaking Changes:**
- `client.getNow()`, `client.get()`, `client.post()` → **REMOVED**
- New pattern: `client.request()` → Future → `send()` → Future
- WebSocket methods moved to separate `WebSocketClient`
- All operations return `Future` instead of using callbacks
- HttpMethod changed from enum to interface

### 4.1 HTTP Client Request Pattern

This is perhaps the most frequently used API in Vert.x applications and has changed significantly.

#### Before (3.9.16)
```java
HttpClient client = vertx.createHttpClient();

// Simple GET request
client.getNow(80, "example.com", "/api/data", response -> {
    response.bodyHandler(buffer -> {
        System.out.println("Response: " + buffer.toString());
    });
});

// POST request with chaining
client.post(80, "example.com", "/api/users")
    .exceptionHandler(err -> err.printStackTrace())
    .handler(response -> {
        response.bodyHandler(System.out::println);
    })
    .end(jsonData);
```

#### After (5.0.4)
```java
HttpClient client = vertx.createHttpClient();

// getNow REMOVED - use request().send()
client.request(HttpMethod.GET, 80, "example.com", "/api/data")
    .compose(HttpClientRequest::send)
    .compose(HttpClientResponse::body)
    .onSuccess(buffer -> {
        System.out.println("Response: " + buffer.toString());
    })
    .onFailure(Throwable::printStackTrace);

// POST request - separated request creation from sending
client.request(HttpMethod.POST, 80, "example.com", "/api/users")
    .onSuccess(request -> {
        request.send(jsonData)
            .compose(HttpClientResponse::body)
            .onSuccess(System.out::println)
            .onFailure(Throwable::printStackTrace);
    });

// Alternative: Using RequestOptions
RequestOptions options = new RequestOptions()
    .setHost("example.com")
    .setPort(80)
    .setURI("/api/data");

client.request(options)
    .compose(req -> req.send())
    .compose(resp -> resp.body())
    .onSuccess(buffer -> System.out.println(buffer));
```

### 4.2 WebSocket Client

#### Before (3.9.16)
```java
HttpClient client = vertx.createHttpClient();

// WebSocket connection
client.webSocket(80, "example.com", "/socket", ws -> {
    ws.textMessageHandler(message -> {
        System.out.println("Received: " + message);
    });
    ws.writeTextMessage("Hello");
});

// Synchronous upgrade
HttpClientRequest request = client.request(HttpMethod.GET, "/");
WebSocket ws = request.upgrade();
```

#### After (5.0.4)
```java
// WebSocket methods moved to WebSocketClient interface
WebSocketClient wsClient = vertx.createWebSocketClient();

// Async WebSocket connection
wsClient.connect(80, "example.com", "/socket")
    .onSuccess(ws -> {
        ws.textMessageHandler(message -> {
            System.out.println("Received: " + message);
        });
        ws.writeTextMessage("Hello");
    });

// HTTP request upgrade - async
HttpClient client = vertx.createHttpClient();
client.request(HttpMethod.GET, 80, "example.com", "/")
    .compose(HttpClientRequest::send)
    .compose(response -> response.request().toWebSocket())
    .onSuccess(ws -> {
        // Use WebSocket
    });
```

### 4.3 HTTP Server WebSocket

#### Before (3.9.16)
```java
HttpServer server = vertx.createHttpServer();

server.websocketHandler(ws -> {
    System.out.println("WebSocket connected: " + ws.path());
    ws.textMessageHandler(msg -> {
        ws.writeTextMessage("Echo: " + msg);
    });
});

// Synchronous upgrade in request handler
server.requestHandler(req -> {
    if (req.path().equals("/socket")) {
        WebSocket ws = req.upgrade();  // Synchronous
        ws.textMessageHandler(System.out::println);
    }
});
```

#### After (5.0.4)
```java
HttpServer server = vertx.createHttpServer();

// webSocketHandler (camelCase change)
server.webSocketHandler(ws -> {
    System.out.println("WebSocket connected: " + ws.path());
    ws.textMessageHandler(msg -> {
        ws.writeTextMessage("Echo: " + msg);
    });
});

// Async upgrade in request handler
server.requestHandler(req -> {
    if (req.path().equals("/socket")) {
        req.toWebSocket()  // Returns Future<ServerWebSocket>
            .onSuccess(ws -> {
                ws.textMessageHandler(System.out::println);
            })
            .onFailure(err -> {
                req.response().setStatusCode(400).end();
            });
    }
});
```

### 4.4 HTTP Method Handling

#### Before (3.9.16)
```java
// HttpMethod is enum
switch (request.method()) {
    case GET:
        // Handle GET
        break;
    case POST:
        // Handle POST
        break;
    case OTHER:
        // Custom method
        String customMethod = request.rawMethod();
        break;
}

// Custom HTTP methods
HttpClientRequest req = client.request(HttpMethod.OTHER, 80, "host", "/");
req.setRawName("PROPFIND");
```

#### After (5.0.4)
```java
// HttpMethod is interface - switch with caution
HttpMethod method = request.method();
if (method == HttpMethod.GET) {
    // Handle GET
} else if (method == HttpMethod.POST) {
    // Handle POST
} else {
    // Custom method
    String customMethod = method.name();
}

// Custom HTTP methods - direct valueOf
HttpClientRequest req = client.request(
    HttpMethod.valueOf("PROPFIND"),
    80,
    "host",
    "/"
);
```

### 4.5 Response Body Handling

#### Before (3.9.16)
```java
HttpClientResponse response = ...;

// Body handler pattern
response.bodyHandler(buffer -> {
    System.out.println(buffer.toString());
});

// End handler
response.endHandler(v -> {
    System.out.println("Response complete");
});
```

#### After (5.0.4)
```java
HttpClientResponse response = ...;

// Use body() method returning Future
response.body().onSuccess(buffer -> {
    System.out.println(buffer.toString());
});

// Use end() method returning Future
response.end().onSuccess(v -> {
    System.out.println("Response complete");
});

// bodyHandler and endHandler still available but deprecated
```

### 4.6 HTTP Connection Pooling

#### Before (3.9.16)
```java
HttpClientOptions options = new HttpClientOptions()
    .setMaxPoolSize(10)
    .setMaxWebSockets(4);  // Default 4 WebSocket connections per endpoint
```

#### After (5.0.4)
```java
// Pool configuration moved to PoolOptions in 5.x
PoolOptions poolOptions = new PoolOptions()
    .setHttp1MaxSize(10)
    .setHttp2MaxSize(5);

HttpClientOptions clientOptions = new HttpClientOptions()
    .setPoolOptions(poolOptions)
    .setMaxWebSockets(50);  // Default changed to 50
```

### 4.7 HTTP/2 Shutdown

#### Before (4.x)
```java
HttpConnection connection = ...;
connection.shutdown();  // No parameters
```

#### After (5.0.4)
```java
HttpConnection connection = ...;
connection.shutdown(5, TimeUnit.SECONDS);  // Requires timeout parameter
```

---

## 5. Database Client Changes

The database client layer was completely rewritten in Vert.x 4.x with a unified reactive SQL client API. This is one of the most significant architectural changes in the migration.

**Critical Changes:**
- `JDBCClient` → `JDBCPool` (completely new API)
- `SQLClient` → `Pool` interface
- `ResultSet` → `RowSet<Row>` (type-safe row access)
- Configuration: `JsonObject` → `JDBCConnectOptions` + `PoolOptions`
- Queries: `queryWithParams()` → `preparedQuery().execute(Tuple)`
- Transactions: Manual commit/rollback → Transaction API with `begin()`/`commit()`
- Connection pooling: Built-in and automatic

**Why This Changed:**
- Unified API across all SQL databases (Postgres, MySQL, JDBC, etc.)
- Better performance with native drivers
- Type-safe column access (no more string-based `row.getString("col")` errors at runtime)
- Future-based (no callbacks)
- Automatic connection pooling

### 5.1 JDBC Client - Complete Overhaul

This is the most significant change for applications using databases.

#### Before (3.9.16)
```java
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.ResultSet;

// Configuration
JsonObject config = new JsonObject()
    .put("url", "jdbc:postgresql://localhost/mydb")
    .put("driver_class", "org.postgresql.Driver")
    .put("user", "dbuser")
    .put("password", "secret");

// Create client
SQLClient client = JDBCClient.create(vertx, config);

// Execute query
client.query("SELECT * FROM users", res -> {
    if (res.succeeded()) {
        ResultSet resultSet = res.result();
        List<JsonObject> rows = resultSet.getRows();
        for (JsonObject row : rows) {
            System.out.println(row.getString("username"));
        }
    }
});

// Get connection for transactions
client.getConnection(connRes -> {
    if (connRes.succeeded()) {
        SQLConnection conn = connRes.result();

        conn.setAutoCommit(false, autoRes -> {
            conn.query("SELECT * FROM users", queryRes -> {
                // Use results
                conn.commit(commitRes -> {
                    conn.close();
                });
            });
        });
    }
});

// Parameterized query
JsonArray params = new JsonArray()
    .add("john@example.com");
client.queryWithParams(
    "SELECT * FROM users WHERE email = ?",
    params,
    res -> {
        // Handle results
    }
);
```

#### After (5.0.4)
```java
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

// Configuration using JDBCConnectOptions
JDBCConnectOptions connectOptions = new JDBCConnectOptions()
    .setJdbcUrl("jdbc:postgresql://localhost/mydb")
    .setUser("dbuser")
    .setPassword("secret");

PoolOptions poolOptions = new PoolOptions()
    .setMaxSize(10);

// Create pool (replaces client)
Pool pool = JDBCPool.pool(vertx, connectOptions, poolOptions);

// Execute query - NEW API
pool.preparedQuery("SELECT * FROM users")
    .execute()
    .onSuccess(rows -> {
        for (Row row : rows) {
            System.out.println(row.getString("username"));
        }
    })
    .onFailure(Throwable::printStackTrace);

// Get connection for transactions
pool.getConnection()
    .onSuccess(conn -> {
        conn.begin()
            .compose(tx -> conn.query("SELECT * FROM users")
                .execute()
                .onSuccess(rows -> {
                    // Use results
                })
                .compose(v -> tx.commit())
            )
            .eventually(() -> {
                conn.close();
                return Future.succeededFuture();
            });
    });

// Parameterized query with Tuple
pool.preparedQuery("SELECT * FROM users WHERE email = ?")
    .execute(Tuple.of("john@example.com"))
    .onSuccess(rows -> {
        // Handle results
    });
```

### 5.2 SQL Client - Type Changes

#### Before (3.9.16)
```java
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;

// Query results
ResultSet resultSet = ...;
List<JsonObject> rows = resultSet.getRows();
List<String> columnNames = resultSet.getColumnNames();
int numRows = resultSet.getNumRows();

// Update results
UpdateResult updateResult = ...;
int updated = updateResult.getUpdated();
JsonArray keys = updateResult.getKeys();
```

#### After (5.0.4)
```java
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;

// Query results - Iterator pattern
RowSet<Row> rows = ...;
for (Row row : rows) {
    String username = row.getString("username");
    Integer age = row.getInteger("age");
    // Type-safe column access
}

// Column names
List<String> columnNames = rows.columnsNames();

// Size
int size = rows.size();

// Update results - same RowSet type
RowSet<Row> updateResult = ...;
int rowsAffected = updateResult.rowCount();
```

### 5.3 PostgreSQL Reactive Client

#### Before (3.9.16 - did not exist in this form)
```java
// No reactive PostgreSQL client in 3.x
// Had to use JDBC client
```

#### After (5.0.4)
```java
import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

// Builder pattern for pool creation
PgConnectOptions connectOptions = new PgConnectOptions()
    .setHost("localhost")
    .setPort(5432)
    .setDatabase("mydb")
    .setUser("dbuser")
    .setPassword("secret");

PoolOptions poolOptions = new PoolOptions()
    .setMaxSize(10);

Pool pool = PgBuilder.pool()
    .with(poolOptions)
    .connectingTo(connectOptions)
    .using(vertx)
    .build();

// Use same API as JDBC pool
pool.preparedQuery("SELECT * FROM users")
    .execute()
    .onSuccess(rows -> {
        for (Row row : rows) {
            System.out.println(row.getString("username"));
        }
    });
```

### 5.4 Database Migration Tips

**Step-by-Step Migration Guide:**

1. **Update Dependencies First:**
   ```gradle
   // Remove
   implementation 'io.vertx:vertx-sql-common:3.9.16'

   // Update
   implementation 'io.vertx:vertx-jdbc-client:5.0.4'
   ```

2. **Replace JDBCClient with JDBCPool:**
   ```java
   // Before: JsonObject config
   JsonObject config = new JsonObject()
       .put("url", "jdbc:postgresql://localhost/db")
       .put("user", "user")
       .put("password", "pass");
   JDBCClient client = JDBCClient.create(vertx, config);

   // After: Typed configuration
   JDBCConnectOptions connectOptions = new JDBCConnectOptions()
       .setJdbcUrl("jdbc:postgresql://localhost/db")
       .setUser("user")
       .setPassword("pass");

   PoolOptions poolOptions = new PoolOptions().setMaxSize(10);
   Pool pool = JDBCPool.pool(vertx, connectOptions, poolOptions);
   ```

3. **Update Query Patterns:**
   ```java
   // Before: callback with ResultSet
   client.query("SELECT * FROM users", res -> {
       if (res.succeeded()) {
           ResultSet rs = res.result();
           for (JsonObject row : rs.getRows()) {
               String name = row.getString("name");
           }
       }
   });

   // After: Future with RowSet
   pool.query("SELECT * FROM users")
       .execute()
       .onSuccess(rows -> {
           for (Row row : rows) {
               String name = row.getString("name");
           }
       });
   ```

4. **Migrate Parameterized Queries:**
   ```java
   // Before: JsonArray params
   JsonArray params = new JsonArray().add("john@example.com");
   client.queryWithParams("SELECT * FROM users WHERE email = ?", params, handler);

   // After: Tuple params
   pool.preparedQuery("SELECT * FROM users WHERE email = ?")
       .execute(Tuple.of("john@example.com"))
       .onSuccess(rows -> { ... });
   ```

5. **Update Transaction Handling:**
   ```java
   // Before: Nested callbacks with manual commit
   client.getConnection(connRes -> {
       SQLConnection conn = connRes.result();
       conn.setAutoCommit(false, ar -> {
           conn.query("...", queryRes -> {
               conn.commit(commitRes -> {
                   conn.close();
               });
           });
       });
   });

   // After: Future composition with Transaction API
   pool.getConnection()
       .compose(conn ->
           conn.begin()
               .compose(tx ->
                   conn.query("...").execute()
                       .compose(rows -> tx.commit())
               )
               .eventually(() -> {
                   conn.close();
                   return Future.succeededFuture();
               })
       );
   ```

**Common Pitfalls:**

1. **Row Access Pattern Changed:**
   ```java
   // WRONG (3.x pattern doesn't work)
   List<JsonObject> rows = rowSet.getRows();  // Method doesn't exist!

   // CORRECT (5.x pattern)
   for (Row row : rowSet) {
       // Process row
   }
   ```

2. **Column Names Method:**
   ```java
   // WRONG
   List<String> cols = rowSet.getColumnNames();

   // CORRECT
   List<String> cols = rowSet.columnsNames();  // Note: different method name
   ```

3. **Connection Pooling:**
   - In 3.x: `JDBCClient` internally pooled connections (hidden)
   - In 5.x: `JDBCPool` explicitly manages pool (visible configuration)
   - **Action**: Configure pool size explicitly with `PoolOptions`

4. **Batch Operations:**
   ```java
   // Before (3.x)
   client.batch(Arrays.asList("INSERT...", "UPDATE..."), handler);

   // After (5.x) - Use prepared query with list of tuples
   List<Tuple> batch = Arrays.asList(
       Tuple.of("user1", "email1"),
       Tuple.of("user2", "email2")
   );
   pool.preparedQuery("INSERT INTO users(name, email) VALUES (?, ?)")
       .executeBatch(batch)
       .onSuccess(rows -> { ... });
   ```

**Performance Benefits in 5.x:**
- Connection pooling is more efficient
- Prepared statements are cached
- Less object allocation (Row vs JsonObject)
- Native database drivers available (PgClient, MySQLClient)

---

## 6. Async Programming Model Changes

### 6.1 Future Composition

#### Before (3.9.16)
```java
Future<String> future1 = loadUser();
Future<List<String>> future2 = loadPermissions();

// Compose futures
future1.compose(user -> {
    return loadUserData(user);
}).compose(userData -> {
    return processData(userData);
}).setHandler(ar -> {
    if (ar.succeeded()) {
        // Final result
    }
});
```

#### After (5.0.4)
```java
Future<String> future1 = loadUser();
Future<List<String>> future2 = loadPermissions();

// Compose futures (similar but using onComplete/onSuccess)
future1
    .compose(user -> loadUserData(user))
    .compose(userData -> processData(userData))
    .onSuccess(result -> {
        // Final result
    })
    .onFailure(err -> {
        // Handle error
    });
```

### 6.2 Promise Pattern

#### Before (3.9.16)
```java
public Future<String> asyncOperation() {
    Promise<String> promise = Promise.promise();

    vertx.setTimer(1000, id -> {
        if (success) {
            promise.complete("result");
        } else {
            promise.fail("error");
        }
    });

    return promise.future();
}

// Using completer
public void legacyMethod(Handler<AsyncResult<String>> handler) {
    Promise<String> promise = Promise.promise();
    promise.future().setHandler(handler);

    // Async work
    promise.complete("result");
}
```

#### After (5.0.4)
```java
public Future<String> asyncOperation() {
    Promise<String> promise = Promise.promise();

    vertx.setTimer(1000, id -> {
        if (success) {
            promise.complete("result");
        } else {
            promise.fail("error");
        }
    });

    return promise.future();
}

// completer() REMOVED - Future implements Handler<AsyncResult<T>>
public void modernMethod(Handler<AsyncResult<String>> handler) {
    Promise<String> promise = Promise.promise();
    promise.future().onComplete(handler);

    // Or pass promise directly as it implements Handler
    legacyAsyncMethod(promise);
}
```

---

## 7. Module-Specific Changes

### 7.1 Circuit Breaker

#### Before (3.9.16)
```java
import io.vertx.circuitbreaker.CircuitBreaker;

CircuitBreaker breaker = CircuitBreaker.create("my-circuit", vertx);

// Execute command
breaker.executeCommand(promise -> {
    someAsyncOperation().setHandler(promise);
}, res -> {
    if (res.succeeded()) {
        // Success
    }
});

// With fallback
breaker.executeCommandWithFallback(
    promise -> someAsyncOperation().setHandler(promise),
    throwable -> "fallback value",
    res -> {
        // Handle result or fallback
    }
);
```

#### After (5.0.4)
```java
import io.vertx.circuitbreaker.CircuitBreaker;

CircuitBreaker breaker = CircuitBreaker.create("my-circuit", vertx);

// Method renamed: executeCommand → execute
breaker.execute(promise -> {
    someAsyncOperation().onComplete(promise);
}).onSuccess(result -> {
    // Success
}).onFailure(err -> {
    // Failure
});

// With fallback: executeCommandWithFallback → executeWithFallback
breaker.executeWithFallback(
    promise -> someAsyncOperation().onComplete(promise),
    throwable -> "fallback value"
).onSuccess(result -> {
    // Result or fallback
});
```

### 7.2 Service Discovery

#### Before (3.9.16)
```java
import io.vertx.servicediscovery.ServiceDiscovery;

// Create with handler
ServiceDiscovery.create(vertx, res -> {
    if (res.succeeded()) {
        ServiceDiscovery discovery = res.result();
    }
});

// Register importer with handler
discovery.registerServiceImporter(importer, config, res -> {
    // Registered
});
```

#### After (5.0.4)
```java
import io.vertx.servicediscovery.ServiceDiscovery;

// Handler-based creation REMOVED
ServiceDiscovery discovery = ServiceDiscovery.create(vertx);

// Or with options
ServiceDiscoveryOptions options = new ServiceDiscoveryOptions();
ServiceDiscovery discovery = ServiceDiscovery.create(vertx, options);

// Register importer - returns Future
discovery.registerServiceImporter(importer, config)
    .onSuccess(v -> {
        // Registered
    });
```

### 7.3 Kafka Client

#### Before (3.9.16)
```java
import io.vertx.kafka.admin.AdminUtils;
import io.vertx.kafka.client.producer.KafkaProducer;

// AdminUtils for topic management
AdminUtils.createTopic(client, "topic-name", 3, 1);

// Producer flush
KafkaProducer<String, String> producer = ...;
producer.flush();  // No handler
```

#### After (5.0.4)
```java
import io.vertx.kafka.admin.KafkaAdminClient;
import io.vertx.kafka.client.producer.KafkaProducer;

// AdminUtils REMOVED - use KafkaAdminClient
KafkaAdminClient adminClient = KafkaAdminClient.create(vertx, config);
NewTopic topic = new NewTopic("topic-name", 3, (short) 1);
adminClient.createTopics(Arrays.asList(topic))
    .onSuccess(v -> {
        // Topic created
    });

// Producer flush with handler
KafkaProducer<String, String> producer = ...;
producer.flush(ar -> {
    if (ar.succeeded()) {
        // Flushed
    }
});
```

### 7.4 Redis Client

#### Before (3.9.16)
```java
import io.vertx.redis.RedisClient;

RedisClient redis = RedisClient.create(vertx);

redis.get("key", res -> {
    if (res.succeeded()) {
        String value = res.result();
    }
});
```

#### After (5.0.4)
```java
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;

Redis redis = Redis.createClient(vertx, "redis://localhost");

RedisAPI api = RedisAPI.api(redis);

api.get("key")
    .onSuccess(response -> {
        String value = response.toString();
    });

// close() now returns Future
redis.close()
    .onSuccess(v -> {
        // Closed
    });
```

### 7.5 RxJava Support

#### Before (3.9.16)
```java
// RxJava 1 support
import io.vertx.rxjava.core.Vertx;

Vertx rxVertx = Vertx.vertx();
```

#### After (5.0.4)
```java
// RxJava 1 & 2 REMOVED - use RxJava 3 or Mutiny
import io.vertx.rxjava3.core.Vertx;

Vertx rxVertx = Vertx.vertx();

// Or use Mutiny (recommended)
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
```

---

## 8. Migration Strategy

### 8.1 Recommended Approach

**Step 1: Prepare (1-2 weeks)**
1. Audit current Vert.x usage across codebase
2. Review all deprecated API usage
3. Update to latest 3.9.x version (3.9.16)
4. Ensure comprehensive test coverage (target: >80%)
5. Document all custom Vert.x extensions

**Step 2: Migrate to 4.x (2-4 weeks)**
1. Update dependencies to Vert.x 4.5.10 (latest 4.x)
2. Fix compilation errors (handlers → futures)
3. Update authentication/authorization (split providers)
4. Migrate JDBC client to pool pattern
5. Run full test suite
6. Performance testing

**Step 3: Stabilize on 4.x (1-2 weeks)**
1. Fix all failing tests
2. Performance regression testing
3. Load testing
4. Production canary deployment

**Step 4: Migrate to 5.x (2-4 weeks)**
1. Update dependencies to Vert.x 5.0.4
2. Update to builder patterns
3. Fix WebSocket client usage
4. Update pool configurations
5. Fix health check imports
6. Run full test suite

**Step 5: Production Rollout (2-4 weeks)**
1. Canary deployment (5% traffic)
2. Monitor metrics and errors
3. Gradual rollout (20% → 50% → 100%)
4. Full production deployment

### 8.2 Direct Migration (3.9.16 → 5.0.4)

**NOT RECOMMENDED** but possible for small projects:

1. Create feature branch
2. Update all dependencies to 5.0.4
3. Fix ALL compilation errors (expect 100s-1000s)
4. Update ALL async patterns
5. Extensive testing required

**Risk:** Very high - too many breaking changes at once

### 8.3 OpenRewrite Migration Recipe

Use the existing OpenRewrite recipes in this project:

```yaml
# Apply Vert.x JDBC migration
recipeList:
  - com.recipies.yaml.VertxJdbcMigrations
```

**What it handles:**
- JDBC client → Pool migration
- Import updates
- Method call transformations
- Dependency updates

**What it doesn't handle (requires manual migration):**
- Future/Handler API changes
- Authentication provider split
- HTTP client pattern changes
- WebSocket API changes
- Event bus changes

---

## 9. Testing & Validation

### 9.1 Unit Testing Changes

#### Before (3.9.16)
```java
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class MyTest {
    @Test
    public void testAsync(TestContext context) {
        Async async = context.async();

        vertx.setTimer(100, id -> {
            context.assertTrue(true);
            async.complete();
        });
    }
}
```

#### After (5.0.4)
```java
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class MyTest {
    @Test
    public void testAsync(Vertx vertx, VertxTestContext testContext) {
        vertx.setTimer(100, id -> {
            testContext.verify(() -> {
                // Assertions
            });
            testContext.completeNow();
        });
    }
}
```

### 9.2 Test Success Handlers

#### Before (4.x)
```java
someAsyncOperation()
    .onComplete(testContext.succeeding());
```

#### After (5.0.4)
```java
someAsyncOperation()
    .onComplete(testContext.succeedingThenComplete());
```

### 9.3 Recommended Test Strategy

1. **Unit Tests:**
   - Test all Vert.x API usage
   - Mock external dependencies
   - Focus on async behavior

2. **Integration Tests:**
   - Test HTTP endpoints
   - Test database operations
   - Test event bus communication

3. **Performance Tests:**
   - Baseline on 3.9.16
   - Compare with 4.x
   - Compare with 5.0.4
   - Watch for regressions

4. **Load Tests:**
   - Simulate production traffic
   - Monitor memory usage
   - Monitor CPU usage
   - Check for connection leaks

---

## 10. Breaking Changes Quick Reference

### 10.1 Critical Breaking Changes

| Category | Change | Impact | Effort |
|----------|--------|--------|--------|
| **Async Model** | `setHandler()` → `onComplete()/onSuccess()/onFailure()` | HIGH | HIGH |
| **JDBC Client** | `JDBCClient` → `JDBCPool` | HIGH | MEDIUM |
| **Auth** | `AuthProvider` split into Authentication/Authorization | HIGH | HIGH |
| **HTTP Client** | Request/response pattern change | HIGH | HIGH |
| **WebSocket** | Moved to `WebSocketClient` | MEDIUM | MEDIUM |
| **Event Bus** | `send(handler)` → `request()` | HIGH | LOW |
| **Worker** | `setWorker()` → `setThreadingModel()` | MEDIUM | LOW |
| **Execute Blocking** | Promise → Callable | MEDIUM | LOW |

### 10.2 Removed APIs

| API | Removed In | Replacement |
|-----|-----------|-------------|
| `Future.setHandler()` | 4.0 | `onComplete()`, `onSuccess()`, `onFailure()` |
| `Future.completer()` | 4.0 | Future implements Handler directly |
| `HttpClient.getNow()` | 4.0 | `request().send()` |
| `EventBus.send(handler)` | 4.0 | `request()` |
| `vertx-sql-common` | 4.0 | Merged into `vertx-jdbc-client` |
| `UserSessionHandler` | 4.0 | Merged into `SessionHandler` |
| `vertx-sync` | 5.0 | Use Virtual Threads or reactive |
| `HttpClient.webSocket()` | 5.0 | `WebSocketClient.connect()` |
| `FileSystem.deleteRecursive(path, boolean)` | 5.0 | `delete()` or `deleteRecursive()` |
| CLI framework | 5.0 | Use Picocli or similar |

### 10.3 Package Moves

| Old Package | New Package | Version |
|-------------|-------------|---------|
| `io.vertx.ext.jdbc.JDBCClient` | `io.vertx.jdbcclient.JDBCPool` | 4.0+ |
| `io.vertx.ext.sql.*` | `io.vertx.sqlclient.*` | 4.0+ |
| `io.vertx.ext.web.Cookie` | `io.vertx.core.http.Cookie` | 4.0+ |
| `io.vertx.ext.web.Locale` | `io.vertx.ext.web.LanguageHeader` | 4.0+ |
| `io.vertx.ext.auth.jwt.JWTOptions` | `io.vertx.ext.jwt.JWTOptions` | 4.0+ |
| `io.vertx.ext.healthchecks.HealthCheckHandler` | `io.vertx.ext.web.healthchecks.HealthCheckHandler` | 5.0+ |

---

## Conclusion

Migrating from Vert.x 3.9.16 to 5.0.4 is a **significant undertaking** that requires:

1. **Thorough Planning:** Understand all breaking changes
2. **Incremental Approach:** Migrate 3.x → 4.x → 5.x
3. **Comprehensive Testing:** Unit, integration, performance, load
4. **Team Training:** Educate team on new patterns
5. **Monitoring:** Watch for regressions in production

**Estimated Timeline:**
- Small projects: 4-8 weeks
- Medium projects: 2-4 months
- Large projects: 4-6 months

**Key Success Factors:**
- Strong test coverage before starting
- Incremental migration with validation at each step
- Performance baseline and regression testing
- Gradual production rollout

**Resources:**
- [Official Vert.x 4 Migration Guide](https://vertx.io/docs/guides/vertx-4-migration-guide/)
- [Official Vert.x 5 Migration Guide](https://vertx.io/docs/guides/vertx-5-migration-guide/)
- [Vert.x 4.0.0 Breaking Changes](https://github.com/vert-x3/wiki/wiki/4.0.0-Deprecations-and-breaking-changes)
- [Vert.x 5.0.0 Breaking Changes](https://github.com/vert-x3/wiki/wiki/5.0.0-Deprecations-and-breaking-changes)
- [Vert.x Documentation](https://vertx.io/docs/)

---

**Document Version:** 1.0
**Last Updated:** 2025-11-14
**Target Versions:** 3.9.16 → 5.0.4
