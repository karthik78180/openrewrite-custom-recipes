# VertxFutureModelMigration

This OpenRewrite recipe provides migration support for embracing the future-only model introduced in Vert.x 5, migrating from Vert.x 3.9.16 to 5.0.4.

---

## Features

- Transforms `vertx.executeBlocking()` from promise-based to `Callable`-based
- Migrates `future.eventually()` from `Function` to `Supplier`
- Removes boilerplate promise manipulation code
- Handles both expression and block lambda bodies
- Converts `promise.complete()` to `return` statements
- Converts `promise.fail()` to `throw` statements

---

## Migration Overview

### Background

Vert.x 5 shifts from a hybrid callback/future model to a "future-only" approach. The migration removes callback-based methods that were maintained for backward compatibility with Vert.x 3, consolidating the API surface and encouraging composable async patterns through standard futures.

### API Changes

**executeBlocking Transformation:**
- **Old**: `vertx.executeBlocking(promise -> promise.complete(result))`
- **New**: `vertx.executeBlocking(() -> result)`

**eventually Transformation:**
- **Old**: `future.eventually(v -> someFuture())`
- **New**: `future.eventually(() -> someFuture())`

---

## How It Works

This migration uses the `VertxFutureModelRecipe` Java visitor that:

1. **Detects executeBlocking calls** with lambda expressions
2. **Transforms promise-based lambdas** to Callable-based lambdas
3. **Removes promise parameters** and replaces promise methods with direct returns
4. **Transforms eventually calls** by removing unused lambda parameters
5. **Handles both expression and block bodies** for comprehensive coverage

---

## Usage

### Using the Future Model Migration Recipe

Apply the future model migration recipe:

```yaml
recipeList:
  - com.recipies.yaml.VertxFutureModelMigration
```

### Using the Complete Vert.x 5 Migration

For a comprehensive migration from Vert.x 3.9.16 to 5.0.4:

```yaml
recipeList:
  - com.recipies.yaml.Vertx5CompleteMigration
```

This includes:
- Dependency updates to 5.0.4
- JDBC client migration
- Future model transformation

---

## Example Transformations

### Example 1: executeBlocking with Expression Body

**Before:**
```java
package com.example;

import io.vertx.core.Vertx;
import io.vertx.core.Future;

public class BlockingService {
    public Future<String> getData(Vertx vertx) {
        return vertx.executeBlocking(promise -> promise.complete("result"));
    }
}
```

**After:**
```java
package com.example;

import io.vertx.core.Vertx;
import io.vertx.core.Future;

public class BlockingService {
    public Future<String> getData(Vertx vertx) {
        return vertx.executeBlocking(() -> "result");
    }
}
```

### Example 2: executeBlocking with Block Body

**Before:**
```java
package com.example;

import io.vertx.core.Vertx;
import io.vertx.core.Future;

public class BlockingService {
    public Future<Integer> compute(Vertx vertx) {
        return vertx.executeBlocking(promise -> {
            int result = 42 + 8;
            promise.complete(result);
        });
    }
}
```

**After:**
```java
package com.example;

import io.vertx.core.Vertx;
import io.vertx.core.Future;

public class BlockingService {
    public Future<Integer> compute(Vertx vertx) {
        return vertx.executeBlocking(() -> {
            int result = 42 + 8;
            return result;
        });
    }
}
```

### Example 3: executeBlocking with Error Handling

**Before:**
```java
package com.example;

import io.vertx.core.Vertx;
import io.vertx.core.Future;

public class BlockingService {
    public Future<String> riskyOperation(Vertx vertx) {
        return vertx.executeBlocking(promise -> {
            if (Math.random() > 0.5) {
                promise.fail(new RuntimeException("Random failure"));
            } else {
                promise.complete("success");
            }
        });
    }
}
```

**After:**
```java
package com.example;

import io.vertx.core.Vertx;
import io.vertx.core.Future;

public class BlockingService {
    public Future<String> riskyOperation(Vertx vertx) {
        return vertx.executeBlocking(() -> {
            if (Math.random() > 0.5) {
                throw new RuntimeException("Random failure");
            } else {
                return "success";
            }
        });
    }
}
```

### Example 4: eventually Transformation

**Before:**
```java
package com.example;

import io.vertx.core.Future;

public class FutureService {
    public Future<String> chainOperations(Future<String> future) {
        return future.eventually(v -> cleanup());
    }

    private Future<String> cleanup() {
        return null;
    }
}
```

**After:**
```java
package com.example;

import io.vertx.core.Future;

public class FutureService {
    public Future<String> chainOperations(Future<String> future) {
        return future.eventually(() -> cleanup());
    }

    private Future<String> cleanup() {
        return null;
    }
}
```

### Example 5: Multiple Transformations

**Before:**
```java
package com.example;

import io.vertx.core.Vertx;
import io.vertx.core.Future;

public class ComplexService {
    public Future<String> processData(Vertx vertx) {
        Future<String> step1 = vertx.executeBlocking(promise -> promise.complete("data"));
        return step1.eventually(v -> cleanup());
    }

    public Future<Integer> computeValue(Vertx vertx) {
        return vertx.executeBlocking(promise -> {
            int value = 100;
            promise.complete(value);
        });
    }

    private Future<String> cleanup() {
        return null;
    }
}
```

**After:**
```java
package com.example;

import io.vertx.core.Vertx;
import io.vertx.core.Future;

public class ComplexService {
    public Future<String> processData(Vertx vertx) {
        Future<String> step1 = vertx.executeBlocking(() -> "data");
        return step1.eventually(() -> cleanup());
    }

    public Future<Integer> computeValue(Vertx vertx) {
        return vertx.executeBlocking(() -> {
            int value = 100;
            return value;
        });
    }

    private Future<String> cleanup() {
        return null;
    }
}
```

---

## Recipe Definition

The recipe is defined in `META-INF/rewrite/rewrite.yml`:

```yaml
type: specs.openrewrite.org/v1beta/recipe
name: com.recipies.yaml.VertxFutureModelMigration
displayName: Migrate to Vert.x 5 Future Model
description: Transforms Vert.x code to embrace the future-only model introduced in Vert.x 5. Converts executeBlocking from promise-based to Callable-based, and eventually from Function to Supplier.
recipeList:
  - com.rewrite.VertxFutureModelRecipe
```

Complete migration recipe:

```yaml
type: specs.openrewrite.org/v1beta/recipe
name: com.recipies.yaml.Vertx5CompleteMigration
displayName: Complete Vert.x 3.9.16 to 5.0.4 Migration
description: Comprehensive migration from Vert.x 3.9.16 to 5.0.4, including JDBC client updates, dependency changes, and future model transformation.
recipeList:
  - org.openrewrite.gradle.UpgradeDependencyVersion:
      groupId: io.vertx
      artifactId: vertx-core
      newVersion: 5.0.4
  - com.recipies.yaml.VertxJdbcMigrations
  - com.recipies.yaml.VertxFutureModelMigration
```

---

## Testing

The recipe includes comprehensive tests in `VertxFutureModelRecipeTest.java` that verify:
- executeBlocking with expression body transformations
- executeBlocking with block body transformations
- executeBlocking with error handling (fail → throw)
- eventually transformations with unused parameters
- Multiple transformations in the same file
- Unrelated code remains unchanged
- eventually with used parameters remains unchanged

Run tests with:
```bash
./gradlew test --tests "com.rewrite.VertxFutureModelRecipeTest"
```

---

## Implementation Details

### VertxFutureModelRecipe

Located at: `src/main/java/com/rewrite/VertxFutureModelRecipe.java`

Key transformations:

1. **executeBlocking Expression Body**:
   - Detects: `promise -> promise.complete(value)`
   - Transforms to: `() -> value`
   - Removes promise parameter and extracts the value

2. **executeBlocking Block Body**:
   - Detects: `promise -> { statements; promise.complete(value); }`
   - Transforms to: `() -> { statements; return value; }`
   - Replaces `promise.complete()` with `return`
   - Replaces `promise.fail()` with `throw`

3. **eventually Transformation**:
   - Detects: `v -> someFuture()` where `v` is unused
   - Transforms to: `() -> someFuture()`
   - Only transforms if parameter is not used in body

### Visitor Pattern

The recipe uses `JavaIsoVisitor` with:
- `visitMethodInvocation()` to detect and transform method calls
- AST manipulation to modify lambda expressions
- Intelligent parameter usage detection for `eventually`

---

## Notes

- The recipe only transforms lambdas that match the promise-based pattern
- Parameter usage is detected heuristically for `eventually` transformations
- The recipe is safe to run multiple times (idempotent)
- Preserves all code logic and functionality
- No manual intervention required after running the recipe

---

## Version Compatibility

- **Source Version**: Vert.x 3.9.16
- **Target Version**: Vert.x 5.0.4
- **OpenRewrite Version**: 8.23.1+
- **Java Version**: 21+

---

## Benefits

1. **Cleaner Code**: Removes boilerplate promise manipulation
2. **Standard Java**: Uses `java.util.concurrent.Callable` instead of Vert.x-specific Promise
3. **Better Readability**: Simpler lambda expressions
4. **Future Proof**: Aligns with Vert.x 5+ API design
5. **Type Safety**: Maintains full type safety throughout transformations

---

## Related Documentation

- [Vert.x 5 Migration Guide](https://vertx.io/docs/guides/vertx-5-migration-guide/)
- [Vert.x Future Documentation](https://vertx.io/docs/apidocs/io/vertx/core/Future.html)
- [OpenRewrite Documentation](https://docs.openrewrite.org/)
- [Vert.x JDBC Migration](VertxJdbcMigration.md)
