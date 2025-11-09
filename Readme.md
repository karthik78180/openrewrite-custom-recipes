# OpenRewrite Custom Recipes for Java Migration

This repository contains custom OpenRewrite recipes designed to automate Java code migrations. OpenRewrite is a powerful tool for refactoring and migrating Java codebases. It provides a framework for defining recipes that can analyze and transform Java source code.

---

## Overview

The custom recipes in this repository are designed to:

1. **Replace Base Classes**: Update classes extending `Vehicle` to extend `Car` instead.
2. **Update Constant References**: Replace references to constants from `MicroConstant` to `ServerConstant` and update their imports.
3. **Gradle Upgrades**: Automate Gradle version upgrades from 7.5+ to 8.14, including wrapper updates and deprecated API replacements.
4. **Vert.x JDBC Migration**: Migrate Vert.x JDBC client from version 3.9.16 to 5.0.5, including dependency updates and API transformations.
5. **Composite Recipes**: Combine multiple recipes into a single migration process.

These recipes can be used to automate repetitive and error-prone code changes across large Java projects.

---

## How OpenRewrite Works

OpenRewrite operates by parsing Java source code into an abstract syntax tree (AST). Recipes define transformations that are applied to the AST, and the modified AST is written back to the source files.

### Key Components of OpenRewrite

1. **Recipe**: A unit of transformation logic. Recipes define what changes to make to the code.
2. **Visitor**: A visitor traverses the AST and applies transformations to specific nodes.
3. **ExecutionContext**: Provides context for the recipe execution, such as logging and configuration.

---

## Recipes in This Repository

### 1. **VehicleToCarRecipe**
This recipe replaces any class extending `Vehicle` with `Car`. It ensures that imports are updated accordingly.

- **File**: `VehicleToCarRecipe`
- **Test**: `VehicleToCarRecipeTest`

### 2. **ChangeConstantReference**
This recipe replaces references to `MicroConstant.APP_ID` with `ServerConstant.APP_ID` and updates the import statements.

- **File**: `ChangeConstantReference`
- **Test**: `ChangeConstantReferenceTest`

### 3. **ChangeConstantsReference**
This recipe generalizes constant replacement by using a mapping of old constants to new constants. It updates both references and imports.

- **File**: `ChangeConstantsReference`
- **Test**: `ChangeConstantsReferenceTest`

### 4. **GradleUpgradeTo8_14Recipe**
This recipe automates the upgrade of Gradle projects from version 7.5+ to version 8.14. It updates the Gradle wrapper, replaces deprecated dependency configurations (`compile` → `implementation`, `runtime` → `runtimeOnly`, etc.), and ensures plugin compatibility.

- **File**: `GradleUpgradeTo8_14Recipe`
- **Test**: `GradleUpgradeTo8_14RecipeTest`
- **Documentation**: [`docs/GradleUpgradeTo8_14Recipe.md`](docs/GradleUpgradeTo8_14Recipe.md)

**Key Features:**
- Updates Gradle wrapper to version 8.14
- Replaces deprecated configurations: `compile`, `testCompile`, `runtime`, `testRuntime`
- Updates common plugins to Gradle 8.14-compatible versions
- Handles both Groovy (`.gradle`) and Kotlin (`.gradle.kts`) build scripts

### 5. **VertxJdbcMigrationRecipe**
This recipe automates the migration of Vert.x JDBC client from version 3.9.16 to 5.0.5. It handles dependency updates, API transformations, and import statement changes.

- **File**: `VertxJdbcMigrationRecipe`
- **Test**: `VertxJdbcMigrationRecipeTest`

**Key Features:**
- Removes `vertx-sql-common` dependency (merged into `vertx-jdbc-client` in 4.x+)
- Updates `vertx-jdbc-client` to version 5.0.5
- Transforms `JDBCClient` to `JDBCPool` type references
- Converts `JDBCClient.create()` method calls to `JDBCPool.pool()`
- Updates `SQLClient` references to `JDBCPool`
- Migrates import statements from old packages to new packages

**Implementation Details:**

The recipe was built by analyzing the [Vert.x 4 Migration Guide](https://vertx.io/docs/guides/vertx-4-migration-guide/#changes-in-vertx-jdbc-client_changes-in-client-components), which documents the following key changes:

1. **Dependency Consolidation**: The SQL common module merged into JDBC client, eliminating the need for separate `vertx-sql-common` dependency
2. **API Changes**: `JDBCClient.create()` replaced with `JDBCPool.pool()` for connection pooling
3. **Package Changes**: Classes moved from `io.vertx.ext.jdbc.*` and `io.vertx.ext.sql.*` to `io.vertx.jdbcclient.*` and `io.vertx.sqlclient.*`

The recipe automates these transformations using OpenRewrite's AST-based code transformation capabilities.

### 6. **Java21MigrationRecipes**
This is a composite recipe that combines all the above recipes into a single migration process. Use this recipe to apply all migrations at once.

- **File**: `Java21MigrationRecipes`
- **Test**: `Java21MigrationRecipesTest`

**Includes:**
- VehicleToCarRecipe
- ChangeConstantReference
- ChangeConstantsReference
- VertxJdbcMigrationRecipe

**Usage:** Run `com.rewrite.Java21MigrationRecipes` to execute all migrations in a single command.

---

## How to Use These Recipes in Your Project

### Prerequisites

1. **Java 21**: This project is configured to use Java 21.
2. **Gradle**: Ensure Gradle is installed or use the provided Gradle wrapper (`gradlew`).
3. **OpenRewrite Dependencies**: The project uses OpenRewrite 8.23.1.

---

### Using the OpenRewrite Gradle Plugin

The easiest way to apply these recipes is by using the OpenRewrite Gradle plugin.

#### 1. Add the Plugin to Your Project

Add the following to your `build.gradle` file:

```gradle
plugins {
    id 'org.openrewrite.rewrite' version '7.16.0'
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    rewrite 'com.rewrite:openrewrite-custom-recipes:1.0.0'
}
```

#### 2. Run All Migrations with Single Composite Recipe

To apply **all migrations at once** (recommended), use the composite recipe:

```gradle
rewrite {
    activeRecipe 'com.rewrite.Java21MigrationRecipes'
}
```

Then execute:
```bash
./gradlew rewriteRun
```

This will run all migrations including:
- Base class transformations
- Constant reference updates
- Vert.x JDBC migration (3.9.16 to 5.0.5)

#### 3. Run Individual Recipes

To run only specific migrations, specify the recipe name:

```gradle
rewrite {
    activeRecipe 'com.rewrite.VertxJdbcMigrationRecipe'
}
```

Available individual recipes:
- `com.rewrite.VehicleToCarRecipe`
- `com.rewrite.ChangeConstantReference`
- `com.rewrite.ChangeConstantsReference`
- `com.rewrite.VertxJdbcMigrationRecipe`
- `com.rewrite.GradleUpgradeTo8_14Recipe`

#### 4. Using YAML-Based Recipes

OpenRewrite supports declarative recipes defined in YAML format. This repository includes YAML-based recipes that can be used in addition to or instead of the Java-based recipes.

**YAML Recipe Benefits:**
- Declarative configuration (easier to read and understand)
- Composability - combine multiple recipes without coding
- Reusable across projects
- No need to recompile for recipe changes

**Using YAML Recipes:**

```gradle
rewrite {
    activeRecipe 'com.recipies.yaml.VertxJdbcMigrations'
}
```

**Available YAML Recipes:**

1. **com.recipies.yaml.VertxJdbcMigrations**
   - Comprehensive Vert.x JDBC migration (3.9.16 to 5.0.5)
   - Includes dependency updates and code transformations

2. **com.recipies.yaml.VertxJdbcImportMigrations**
   - Import statement migrations only
   - Type transformations for Vert.x JDBC classes

3. **com.recipies.yaml.AllMigrations**
   - Composite recipe combining:
     - Gradle upgrade to 8.14
     - Base class transformations (Vehicle → Car)
     - Constant reference updates
     - Vert.x JDBC migration

**YAML Recipe Definitions:**

YAML recipes are defined in `src/main/resources/META-INF/rewrite/rewrite.yml`. Example structure:

```yaml
---
type: specs.openrewrite.org/v1beta/recipe
name: com.recipies.yaml.VertxJdbcMigrations
displayName: Migrate Vert.x JDBC from 3.9.16 to 5.0.5
description: Comprehensive migration including dependency and code changes
recipeList:
  - org.openrewrite.gradle.RemoveDependency:
      groupId: io.vertx
      artifactId: vertx-sql-common
  - org.openrewrite.gradle.UpgradeDependencyVersion:
      groupId: io.vertx
      artifactId: vertx-jdbc-client
      newVersion: 5.0.5
  - com.rewrite.VertxJdbcClientToPoolRecipe
  - com.recipies.yaml.VertxJdbcImportMigrations
```

**Comparing Java vs YAML Recipes:**

| Aspect | Java Recipes | YAML Recipes |
|--------|--------------|--------------|
| **Definition** | Java classes extending `Recipe` | YAML files in `rewrite.yml` |
| **Composition** | Implement `getRecipeList()` | List recipes in `recipeList` |
| **Complexity** | Better for complex logic | Better for simple compositions |
| **Performance** | Compiled, potentially faster | Interpreted at runtime |
| **Maintenance** | Requires code changes + rebuild | Only file edits needed |

**Example: Using YAML Recipes in Your Project**

```gradle
plugins {
    id 'org.openrewrite.rewrite' version '7.16.0'
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    rewrite 'com.rewrite:openrewrite-custom-recipes:1.0.0'
}

rewrite {
    // Use YAML-based composite recipe for all migrations
    activeRecipe 'com.recipies.yaml.AllMigrations'
}
```

Then run:
```bash
./gradlew rewriteRun
```