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

#### Before (3.9.16)
```gradle
dependencies {
    implementation 'io.vertx:vertx-core:3.9.16'
    implementation 'io.vertx:vertx-web:3.9.16'
    implementation 'io.vertx:vertx-jdbc-client:3.9.16'
    implementation 'io.vertx:vertx-sql-common:3.9.16'
}
```

#### After (5.0.4)
```gradle
dependencies {
    implementation 'io.vertx:vertx-core:5.0.4'
    implementation 'io.vertx:vertx-web:5.0.4'
    implementation 'io.vertx:vertx-jdbc-client:5.0.4'
    // vertx-sql-common REMOVED - merged into vertx-jdbc-client in 4.x

    // Jackson Databind - now OPTIONAL (required in 3.x, optional in 4.x+)
    // Add explicitly if using JSON object mapping
    implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.2'
}
```

### 1.2 Removed Dependencies (Complete Removal)

These dependencies have been **completely removed** and have no direct replacement:

| Dependency | Removed In | Alternative |
|------------|------------|-------------|
| `io.vertx:vertx-sql-common` | **4.0** | Functionality merged into `vertx-jdbc-client` |
| `io.vertx:vertx-sync` | **5.0** | Use Virtual Threads (Java 21+) or stay with reactive patterns |
| `io.vertx:vertx-service-factory` | **5.0** | Use standard Verticle deployment |
| `io.vertx:vertx-maven-service-factory` | **5.0** | Use standard Maven dependency management |
| `io.vertx:vertx-http-service-factory` | **5.0** | Use standard HTTP-based deployment |
| Vert.x CLI (`vertx` command-line tool) | **5.0** | Use Maven/Gradle plugins or `VertxApplication` |

**Important:** Jackson Databind (`com.fasterxml.jackson.core:jackson-databind`) changed from:
- **3.x**: Transitive dependency (included automatically)
- **4.x+**: Optional dependency (must add explicitly if needed)

### 1.3 Deprecated/Sunset Components (Still Available but Discouraged)

These are still supported in 5.x but **discouraged** and may be removed in 6.x:

| Component | Status | Migration Path |
|-----------|--------|----------------|
| gRPC Netty (`vertx-grpc`) | **Sunset** | Migrate to Vert.x gRPC client/server |
| RxJava 2 (`vertx-rx-java2`) | **Sunset** | Migrate to Mutiny or RxJava 3 |
| OpenTracing (`vertx-opentracing`) | **Sunset** | Migrate to OpenTelemetry |
| Vert.x Unit (`vertx-unit`) | **Sunset** | Migrate to JUnit 5 with `vertx-junit5` |

### 1.4 Module Replacements (Different Artifact)

Modules that were renamed or completely rewritten:

| Old Module (3.x) | New Module (5.x) | Change Type |
|------------------|------------------|-------------|
| `vertx-web-api-contract` | `vertx-web-openapi` | Complete rewrite with new API |
| `vertx-rx-java` (RxJava 1) | `vertx-rx-java3` | RxJava 1 & 2 removed, only RxJava 3 supported |
| `vertx-lang-kotlin-coroutines` | Built-in Kotlin coroutines | Generated suspending extensions removed |

### 1.5 Updated Dependencies (Version Changes with Breaking Changes)

Third-party libraries that have **breaking changes** between versions:

| Library | 3.9.16 Version | 5.0.4 Version | Impact Level | Key Changes |
|---------|----------------|---------------|--------------|-------------|
| **Netty** | 4.1.x | 4.1.100+ | **LOW** | Mostly internal changes |
| **Jackson** | 2.11.x | 2.15.x | **MEDIUM** | Serialization behavior changes |
| **Hazelcast** | 3.x/4.x | 5.3.2+ | **HIGH** | **Requires Java 11+**, API changes |
| **MongoDB Driver** | 3.x/4.x | 5.x | **HIGH** | Complete API overhaul |
| **GraphQL-Java** | 15.x | 23.x | **HIGH** | Breaking changes in v20, v22, v23 |
| **Micrometer** | 1.x | 1.14+ | **MEDIUM** | Metric naming changes |
| **PostgreSQL JDBC** | External | Built-in SCRAM | **LOW** | SCRAM auth now built-in, remove `com.ongres.scram:client` |

---

## 2. Package & Import Changes

### 2.1 JDBC & SQL Client Packages

#### Before (3.9.16)
```java
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
```

#### After (5.0.4)
```java
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
// ResultSet and UpdateResult replaced by RowSet<Row>
```

### 2.2 Jackson JSON Package Changes

#### Before (3.9.16)
```java
import io.vertx.core.json.Json;

// Usage
ObjectMapper mapper = Json.mapper();
String json = Json.encode(object);
```

#### After (5.0.4)
```java
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.core.json.jackson.JacksonCodec;

// Usage
ObjectMapper mapper = DatabindCodec.mapper();
String json = JacksonCodec.encodeToString(object);
```

---

## 3. Core API Changes

### 3.1 Future & Async Handling

#### Before (3.9.16)
```java
// Using setHandler
Future<String> future = someAsyncOperation();
future.setHandler(ar -> {
    if (ar.succeeded()) {
        String result = ar.result();
    } else {
        Throwable cause = ar.cause();
    }
});

// Using completer
Promise<String> promise = Promise.promise();
Handler<AsyncResult<String>> handler = promise.completer();
someMethodWithCallback(handler);
```

#### After (5.0.4)
```java
// setHandler REMOVED - use onComplete, onSuccess, onFailure
Future<String> future = someAsyncOperation();
future.onComplete(ar -> {
    if (ar.succeeded()) {
        String result = ar.result();
    } else {
        Throwable cause = ar.cause();
    }
});

// Separate success/failure handlers
future
    .onSuccess(result -> System.out.println("Success: " + result))
    .onFailure(err -> System.err.println("Failed: " + err.getMessage()));

// completer() REMOVED - Future implements Handler directly
Promise<String> promise = Promise.promise();
someMethodWithCallback(promise);  // Future extends Handler<AsyncResult<T>>
```

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

#### Before (3.9.16)
```java
vertx.executeBlocking(promise -> {
    // Blocking code
    String result = blockingOperation();
    promise.complete(result);
}, res -> {
    if (res.succeeded()) {
        String result = res.result();
    }
});

// Ordered execution (default in 3.x)
vertx.executeBlocking(promise -> {
    promise.complete(result);
}, true, handler);  // ordered=true
```

#### After (5.0.4)
```java
// Uses Callable instead of Promise
vertx.<String>executeBlocking(() -> {
    // Blocking code
    return blockingOperation();  // Direct return
}).onSuccess(result -> {
    // Handle result
});

// Ordered vs unordered execution
vertx.<String>executeBlocking(() -> blockingOperation(), true)  // ordered
    .onSuccess(result -> {});

vertx.<String>executeBlocking(() -> blockingOperation(), false)  // unordered
    .onSuccess(result -> {});
```

---

## 4. HTTP Client & Server Changes

### 4.1 HTTP Client Request Pattern

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

### 5.1 JDBC Client - Complete Overhaul

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

### 6.2 Future.eventually()

#### Before (3.9.16 - not available)
```java
// Not available in 3.x
```

#### Introduced in 4.x, Changed in 5.x
```java
// 4.x - Function parameter
future.eventually(v -> {
    return cleanupOperation();  // Takes parameter (even if unused)
});

// 5.0.4 - Supplier parameter
future.eventually(() -> {
    return cleanupOperation();  // No parameter
});
```

### 6.3 Promise Pattern

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

### 7.4 MQTT Client

#### Before (3.9.16)
```java
import io.vertx.mqtt.MqttClient;

MqttClient client = MqttClient.create(vertx);

// Fluent methods
client.connect(1883, "mqtt-broker")
    .publish("topic", Buffer.buffer("message"), 0, false, false)
    .disconnect();
```

#### After (5.0.4)
```java
import io.vertx.mqtt.MqttClient;

MqttClient client = MqttClient.create(vertx);

// Methods return Future - not fluent
client.connect(1883, "mqtt-broker")
    .compose(v -> client.publish("topic", Buffer.buffer("message"), 0, false, false))
    .compose(v -> client.disconnect())
    .onSuccess(v -> {
        // All operations complete
    });
```

### 7.5 Redis Client

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

### 7.6 RxJava Support

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

### 7.7 gRPC

#### Before (3.9.16)
```java
// Used Vert.x fork of gRPC compiler
// protoc-gen-grpc-java

// Generated code: GreeterGrpc
public class MyService extends GreeterGrpc.GreeterImplBase {
    public void sayHello(HelloRequest request, Promise<HelloReply> promise) {
        promise.complete(HelloReply.newBuilder()
            .setMessage("Hello " + request.getName())
            .build());
    }
}
```

#### After (5.0.4)
```java
// Use official gRPC compiler + Vert.x plugin
// io.grpc:protoc-gen-grpc-java + vertx-grpc-protoc-plugin

// Generated code: VertxGreeterGrpc (note Vertx prefix)
public class MyService extends VertxGreeterGrpc.GreeterImplBase {
    public Future<HelloReply> sayHello(HelloRequest request) {
        return Future.succeededFuture(
            HelloReply.newBuilder()
                .setMessage("Hello " + request.getName())
                .build()
        );
    }
}
```

### 7.8 Health Checks

#### Before (3.9.16 & 4.x)
```java
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;

HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx);

healthCheckHandler.register("database", promise -> {
    // Check database
    if (dbHealthy) {
        promise.complete(Status.OK());
    } else {
        promise.complete(Status.KO());
    }
});
```

#### After (5.0.4)
```java
// Package changed!
import io.vertx.ext.web.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.Status;

HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx);

healthCheckHandler.register("database", promise -> {
    // Check database
    if (dbHealthy) {
        promise.complete(Status.OK());
    } else {
        promise.complete(Status.KO());
    }
});
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
