# Checkstyle Formatting Recipe

## Overview

The **CheckstyleFormatting** recipe is a composite OpenRewrite recipe that applies comprehensive Checkstyle-compliant code formatting and style rules to Java source files. This recipe ensures consistency across codebases by enforcing standard Java formatting conventions.

**Recipe ID**: `com.recipies.yaml.CheckstyleFormatting`

## Features

The Checkstyle formatting recipe enforces the following rules:

### 1. **AutoFormat** (`org.openrewrite.java.format.AutoFormat`)
- Applies standard Java code formatting with proper indentation
- Normalizes whitespace and line breaks
- Ensures consistent code structure across the codebase

### 2. **Blank Lines** (`org.openrewrite.java.format.BlankLines`)
- **Configuration**: `maxConsecutiveBlankLines: 1`
- Reduces excessive consecutive blank lines to a maximum of 1
- Improves code readability and reduces file size
- Maintains single blank lines between logical sections

### 3. **Empty Newline at End of File** (`org.openrewrite.java.format.EmptyNewlineAtEndOfFile`)
- Ensures every Java file ends with a newline character
- Complies with POSIX text file standards
- Prevents issues with some build tools and version control systems

### 4. **Import Organization** (`org.openrewrite.java.OrderImports`)
- Organizes imports into logical groups:
  - **Group 1**: Wildcard imports (`*`)
  - **Group 2**: javax packages
  - **Group 3**: java packages
- **Additional Options**:
  - `separatedStaticGroups: true` - Separates static imports into their own section
  - `sortStaticImportsAlphabetically: true` - Sorts static imports alphabetically
  - `removeUnusedImports: true` - Removes unused import statements
  - `removeUnusedNamedImports: true` - Removes unused named imports

## Usage

### Standalone Recipe

Apply only the Checkstyle formatting rules to your codebase:

```bash
./gradlew rewriteRun --recipe=com.recipies.yaml.CheckstyleFormatting
```

### Part of AllMigrations

The CheckstyleFormatting recipe is automatically applied as part of the comprehensive **AllMigrations** recipe:

```bash
./gradlew rewriteRun --recipe=com.recipies.yaml.AllMigrations
```

This combines:
- Gradle upgrade (8.13)
- Java 21 migrations
- Vert.x JDBC migrations (3.9.16 → 5.0.5)
- **Checkstyle formatting** (applied last for consistency)

## Configuration in YAML

```yaml
---
type: specs.openrewrite.org/v1beta/recipe
name: com.recipies.yaml.CheckstyleFormatting
displayName: Apply Checkstyle formatting and Java code style rules
description: >-
  Comprehensive recipe for applying Checkstyle-compliant formatting to Java code.
  Enforces: no consecutive blank lines, proper indentation,
  proper import ordering, modifier order, and code style standards.
recipeList:
  # Automatic Java code formatting with proper indentation
  - org.openrewrite.java.format.AutoFormat
  # Ensure proper spacing in blank lines (max 1 consecutive blank line)
  - org.openrewrite.java.format.BlankLines:
      maxConsecutiveBlankLines: 1
  # Ensure empty newline at end of file
  - org.openrewrite.java.format.EmptyNewlineAtEndOfFile
  # Ensure proper import ordering (matches Checkstyle ImportOrder configuration)
  - org.openrewrite.java.OrderImports:
      groups:
        - "*"
        - "javax"
        - "java"
      separatedStaticGroups: true
      sortStaticImportsAlphabetically: true
      removeUnusedImports: true
      removeUnusedNamedImports: true
```

## Examples

### Example 1: Reducing Consecutive Blank Lines

**Before**:
```java
public class Example {
    private String field1;



    private String field2;

    public void method1() {
    }



    public void method2() {
    }
}
```

**After**:
```java
public class Example {
    private String field1;

    private String field2;

    public void method1() {
    }

    public void method2() {
    }
}
```

### Example 2: Organizing Imports

**Before**:
```java
import java.util.List;
import com.other.Utils;
import javax.swing.JFrame;
import java.io.File;
import static java.lang.Math.max;
import static java.lang.Math.min;
```

**After**:
```java
import com.other.Utils;

import javax.swing.JFrame;
import java.io.File;
import java.util.List;

import static java.lang.Math.max;
import static java.lang.Math.min;
```

### Example 3: Empty Newline at End of File

**Before**:
```java
public class Example {
    public void method() {
    }
}
```

**After**:
```java
public class Example {
    public void method() {
    }
}

```

(File ends with a newline character)

## Test Coverage

The recipe is validated through comprehensive test suites:

### CheckstyleFormattingRecipeTest
- Tests max consecutive blank lines enforcement
- Tests single blank line preservation
- Tests import organization by groups
- Tests static import separation and alphabetization
- Tests code formatting preservation

### CheckstyleWithAllMigrationsTest
- Integration tests for the complete AllMigrations recipe
- Validates Checkstyle formatting with other migrations
- Tests Vehicle→Car transformations with formatting
- Tests import organization with other transformations
- Tests static import separation in integrated scenarios

## OpenRewrite Gradle Plugin

The OpenRewrite Gradle Plugin is required to run recipes:

**Version**: 7.20.0

**Configuration in build.gradle**:
```gradle
plugins {
    id 'java'
    id 'maven-publish'
    alias libs.plugins.openrewrite
}

rewrite {
    activeRecipe(
        'com.recipies.yaml.CheckstyleFormatting',
        // ... other recipes
    )
}
```

### Available Tasks

```bash
# Discover and list all available recipes
./gradlew rewriteDiscover

# Apply recipes in dry-run mode
./gradlew rewriteRun

# Apply recipes (modifies source files)
./gradlew rewriteRun --recipe=com.recipies.yaml.CheckstyleFormatting
```

## Integration with AllMigrations

When used as part of AllMigrations, Checkstyle formatting is applied **last** in the recipe chain, ensuring that:
1. Code transformations (Vehicle→Car, Vert.x JDBC) are applied first
2. Gradle wrapper is updated
3. Formatting rules are applied to the final transformed code

This guarantees that all code changes are properly formatted according to Checkstyle standards.

## Benefits

✅ **Consistency**: Enforces uniform code style across the entire codebase

✅ **Readability**: Improves code readability through proper formatting

✅ **Standards Compliance**: Ensures compatibility with Checkstyle configuration

✅ **Automation**: Eliminates manual code formatting tasks

✅ **CI/CD Integration**: Can be integrated into build pipelines for automatic formatting checks

## See Also

- [AllMigrations Recipe](./AllMigrations.md)
- [GradleUpgradeTo8_14Recipe](./GradleUpgradeTo8_14Recipe.md)
- [VertxJdbcMigration](./VertxJdbcMigration.md)
- [OpenRewrite Documentation](https://docs.openrewrite.org/)

## Recipe List

All 363 available OpenRewrite recipes are documented in [recipes.txt](../recipes.txt)

### Key OpenRewrite Recipes Used

- `org.openrewrite.java.format.AutoFormat` - Automatic Java formatting
- `org.openrewrite.java.format.BlankLines` - Blank line management
- `org.openrewrite.java.format.EmptyNewlineAtEndOfFile` - EOF newline enforcement
- `org.openrewrite.java.OrderImports` - Import organization

## Related Recipes

- `com.recipies.yaml.AllMigrations` - Comprehensive migration including Checkstyle
- `com.recipies.yaml.VertxJdbcMigrations` - Vert.x JDBC migration (paired with Checkstyle)
- `com.rewrite.Java21MigrationRecipes` - Java 21 migrations (paired with Checkstyle)
