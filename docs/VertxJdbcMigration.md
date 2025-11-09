# VertxJdbcMigration

This OpenRewrite recipe suite provides comprehensive migration support for upgrading Vert.x JDBC client from version 3.9.16 to 5.0.5.

---

## Features

- Removes deprecated `vertx-sql-common` dependency (merged into `vertx-jdbc-client` in 4.x+)
- Updates `vertx-jdbc-client` dependency to version 5.0.5
- Migrates API usage from `JDBCClient` to `JDBCPool`
- Updates import statements and package references
- Transforms method invocations (`create()` → `pool()`)
- Handles type migrations (`SQLClient` → `JDBCPool`)

---

## Migration Overview

### Dependency Changes

**Before (3.9.16):**
```xml
<dependency>
    <groupId>io.vertx</groupId>
    <artifactId>vertx-sql-common</artifactId>
    <version>3.9.16</version>
</dependency>
<dependency>
    <groupId>io.vertx</groupId>
    <artifactId>vertx-jdbc-client</artifactId>
    <version>3.9.16</version>
</dependency>
```

**After (5.0.5):**
```xml
<dependency>
    <groupId>io.vertx</groupId>
    <artifactId>vertx-jdbc-client</artifactId>
    <version>5.0.5</version>
</dependency>
```

### API Changes

**Type Migrations:**
- `io.vertx.ext.jdbc.JDBCClient` → `io.vertx.jdbcclient.JDBCPool`
- `io.vertx.ext.sql.SQLClient` → `io.vertx.jdbcclient.JDBCPool`
- `io.vertx.ext.sql.SQLConnection` → `io.vertx.sqlclient.SqlConnection`

**Method Migrations:**
- `JDBCClient.create()` → `JDBCPool.pool()`

---

## How It Works

This migration consists of two main recipes:

### 1. VertxJdbcClientToPoolRecipe

A custom Java visitor that transforms:
- Variable type declarations from `JDBCClient`/`SQLClient` to `JDBCPool`
- Method invocations from `JDBCClient.create()` to `JDBCPool.pool()`
- Import statements (adds new, removes old when safe)

### 2. VertxJdbcImportMigration

Uses OpenRewrite's built-in `ChangeType` recipes to handle comprehensive type migrations across the codebase:
- Updates all type references
- Cleans up import statements
- Handles fully qualified type names

---

## Usage

### Using the Complete Migration Recipe

Apply the comprehensive migration recipe that handles both dependency updates and code transformations:

```yaml
recipeList:
  - com.rewrite.VertxJdbcMigration
```

### Using Individual Recipes

You can also apply individual recipes as needed:

```yaml
recipeList:
  - com.rewrite.VertxJdbcClientToPoolRecipe  # Code transformations only
  - com.rewrite.VertxJdbcImportMigration     # Import/type migrations only
```

---

## Example Transformations

### Example 1: Basic JDBCClient Migration

**Before:**
```java
package com.example;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;

public class DatabaseService {
    public void init(Vertx vertx, JsonObject config) {
        JDBCClient client = JDBCClient.create(vertx, config);
    }
}
```

**After:**
```java
package com.example;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;

public class DatabaseService {
    public void init(Vertx vertx, JsonObject config) {
        JDBCPool client = JDBCPool.pool(vertx, config);
    }
}
```

### Example 2: SQLClient Migration

**Before:**
```java
package com.example;

import io.vertx.core.Vertx;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.jdbc.JDBCClient;

public class Repository {
    private SQLClient client;

    public void setup(Vertx vertx) {
        client = JDBCClient.create(vertx, null);
    }
}
```

**After:**
```java
package com.example;

import io.vertx.core.Vertx;
import io.vertx.jdbcclient.JDBCPool;

public class Repository {
    private JDBCPool client;

    public void setup(Vertx vertx) {
        client = JDBCPool.pool(vertx, null);
    }
}
```

### Example 3: Multiple Client Instances

**Before:**
```java
package com.example;

import io.vertx.core.Vertx;
import io.vertx.ext.jdbc.JDBCClient;

public class MultiClientService {
    private JDBCClient primaryClient;
    private JDBCClient secondaryClient;

    public void init(Vertx vertx) {
        primaryClient = JDBCClient.create(vertx, null);
        secondaryClient = JDBCClient.create(vertx, null);
    }
}
```

**After:**
```java
package com.example;

import io.vertx.core.Vertx;
import io.vertx.jdbcclient.JDBCPool;

public class MultiClientService {
    private JDBCPool primaryClient;
    private JDBCPool secondaryClient;

    public void init(Vertx vertx) {
        primaryClient = JDBCPool.pool(vertx, null);
        secondaryClient = JDBCPool.pool(vertx, null);
    }
}
```

---

## Recipe Definition

The complete recipe is defined in `META-INF/rewrite/rewrite.yml`:

```yaml
type: specs.openrewrite.org/v1beta/recipe
name: com.rewrite.VertxJdbcMigration
displayName: Migrate Vert.x JDBC from 3.9.16 to 5.0.5
description: Comprehensive migration of Vert.x JDBC client from version 3.9.16 to 5.0.5, including dependency updates and API changes.
recipeList:
  # Remove vertx-sql-common dependency (merged into vertx-jdbc-client in 4.x+)
  - org.openrewrite.maven.RemoveDependency:
      groupId: io.vertx
      artifactId: vertx-sql-common
  # Update vertx-jdbc-client to 5.0.5
  - org.openrewrite.maven.UpgradeDependencyVersion:
      groupId: io.vertx
      artifactId: vertx-jdbc-client
      newVersion: 5.0.5
  # Apply code transformations for Vert.x JDBC migration
  - com.rewrite.VertxJdbcClientToPoolRecipe
  - com.rewrite.VertxJdbcImportMigration
```

---

## Testing

The recipe includes comprehensive tests in `VertxJdbcMigrationRecipeTest.java` that verify:
- Basic `JDBCClient` to `JDBCPool` transformations
- `SQLClient` to `JDBCPool` migrations
- Multiple client instance handling
- Unrelated code remains unchanged

Run tests with:
```bash
./gradlew test
```

---

## Implementation Details

### VertxJdbcClientToPoolRecipe

Located at: `src/main/java/com/rewrite/VertxJdbcClientToPoolRecipe.java`

Key transformations:
1. **Variable Declarations**: Updates type expressions from `JDBCClient`/`SQLClient` to `JDBCPool`
2. **Method Invocations**: Transforms `JDBCClient.create()` calls to `JDBCPool.pool()`
3. **Import Management**: Removes old imports and adds new ones as needed

### VertxJdbcImportMigration

Defined in: `src/main/resources/META-INF/rewrite/rewrite.yml`

Uses OpenRewrite's `ChangeType` recipe to handle:
- Complete package path updates
- Import statement cleanup
- Fully qualified type name migrations

---

## Notes

- The migration preserves all method arguments and configuration
- Import cleanup is handled automatically
- The recipe is safe to run multiple times (idempotent)
- No manual intervention required after running the recipe

---

## Version Compatibility

- **Source Version**: Vert.x JDBC 3.9.16
- **Target Version**: Vert.x JDBC 5.0.5
- **OpenRewrite Version**: 8.23.1+

---

## Related Documentation

- [Vert.x JDBC Client 5.x Documentation](https://vertx.io/docs/vertx-jdbc-client/java/)
- [OpenRewrite Documentation](https://docs.openrewrite.org/)
- [Vert.x Migration Guide](https://github.com/vert-x3/wiki/wiki/4.0.0-Deprecations-and-breaking-changes)
