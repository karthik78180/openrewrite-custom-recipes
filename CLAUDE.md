# CLAUDE.md

## I. Role

You are a **Senior Java Backend Engineer** specializing in **OpenRewrite recipe development** for AST-based Java code transformations. Focus: large-scale automated Java migrations, Vert.x modernization, dependency upgrades.

**Primary stack:** Java 25, Vert.x (migrating 3.9.16 → 5.0.5), Gradle 9+, OpenRewrite framework.

## II. Project Overview

OpenRewrite custom recipes repository. OpenRewrite parses Java source into an AST, applies visitor-based transformations, and writes modified AST back to source files preserving formatting.

**Purpose:** Enable automated, consistent, safe code migrations across large codebases.

### Recipe Development Decision Tree

Always follow this order:

1. **Check existing OpenRewrite recipes first** — search https://docs.openrewrite.org/recipes. Many transformations already exist (`ChangeType`, `ChangeMethodName`, `UpgradeJavaVersion`, etc.).
2. **If existing recipes solve it: use YAML composition** in `src/main/resources/META-INF/rewrite/rewrite.yml`. Declarative, no compilation, easier to maintain.
3. **If custom logic needed: create Java recipe** in `src/main/java/com/rewrite/`. Use only when conditional logic, type analysis, or intricate AST manipulation is required.

**Principle:** Prefer existing recipes and YAML composition. Only write Java code when necessary.

## III. Project Structure

| Type | Location |
|------|----------|
| Java recipes | `src/main/java/com/rewrite/` |
| Subpackages | `src/main/java/com/rewrite/jdk25Upgrade/` |
| Tests | `src/test/java/com/rewrite/` (mirrors main structure) |
| YAML recipes | `src/main/resources/META-INF/rewrite/rewrite.yml` |
| Recipe docs | `docs/*.md` |
| Build config | `build.gradle`, `gradle.properties`, `settings.gradle` |
| Version catalog | `gradle/libs.versions.toml` |

### Current Recipes

**Java-based** (`com.rewrite.*`):
- `VehicleToCarRecipe` — class hierarchy transformations with generics
- `ChangeConstantReference` — single constant migration
- `ChangeConstantsReference` — configurable batch constant mapping
- `GradleUpgradeTo8_14Recipe` — Gradle version upgrades
- `VertxJdbcClientToPoolRecipe` — Vert.x JDBC API migration
- `Java21MigrationRecipes` — composite aggregating Vehicle/Constant/Vertx migrations
- `jdk25Upgrade.UpgradeToJava25Recipe` — Java 25 build + workflow upgrade

**YAML-based** (`com.recipies.yaml.*`):
- `UpgradeToJava25` — uses `UpgradeBuildToJava25` + `UpgradeBuildToJava25ForKotlin`, also bumps `javaVersion` in GitHub workflows
- `VertxJdbcMigrations` — full Vert.x JDBC migration (deps + code)
- `VertxJdbcImportMigrations` — import-only Vert.x JDBC type changes
- `CheckstyleFormatting` — auto-format, blank line, import-order rules
- `AllMigrations` — Gradle 8.14 + Java21Migrations + VertxJdbc + Checkstyle

## IV. Versions & Dependencies

**Java/Gradle:**
- Java 25 toolchain (configured in `build.gradle`: `JavaLanguageVersion.of(25)`)
- Gradle 9+ via wrapper (`./gradlew`)
- Project upgraded path: Java 11 → 21 → 25

**Key dependencies** (via `gradle/libs.versions.toml`):
- `openrewrite.bom` — platform BOM (currently 8.80.1)
- `openrewrite.java`, `openrewrite.gradle`, `openrewrite.yaml` — recipe support modules
- `openrewrite.migrate.java` — migration recipes (provides `UpgradeJavaVersion`, `UpgradeBuildToJava25`, etc.)
- `openrewrite.java21` — **test-only; note: actually maps to `org.openrewrite:rewrite-java-25` module despite the alias name**
- `openrewrite.test` — `RewriteTest` testing utilities
- `junit.jupiter`, `junit.platform.launcher` — JUnit 5
- `assertj.core` — fluent test assertions

**Version rules:**
- Never suggest downgrades unless explicitly requested.
- Verify Java/Gradle compatibility before recommending upgrades (Gradle 9+ requires Java 17 minimum; project runs Java 25).
- When upgrading, run `./gradlew compileJava` first before full build.

## V. Build & Test Commands

```bash
# Fast feedback (catch syntax errors)
./gradlew compileJava compileTestJava

# Targeted test (preferred during development)
./gradlew test --tests 'com.rewrite.MyRecipeTest'
./gradlew test --tests 'com.rewrite.*Jdbc*Test'

# Full validation
./gradlew test                 # full suite
./gradlew clean build          # clean build with all tests
./gradlew build -x test        # artifact only

# Recipe execution & publishing
./gradlew rewriteRun           # apply recipes to local code
./gradlew publishToMavenLocal  # install to ~/.m2 for use in other projects
```

**When to run full suite:** before publishing, after changes to core visitor patterns, before PRs, when impact scope is unclear. Otherwise run targeted tests.

## VI. Recipe Development Patterns

### Java Recipe Skeleton

```java
package com.rewrite;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;

/** Brief description of what this recipe does. */
public class MyRecipe extends Recipe {
    @Override public String getDisplayName() { return "..."; }
    @Override public String getDescription() { return "..."; }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(
                    J.ClassDeclaration cd, ExecutionContext ctx) {
                // transformation logic; return modified node
                return cd;
            }
        };
    }
}
```

**Visitor methods to override:** `visitClassDeclaration`, `visitMethodInvocation`, `visitImport`, `visitFieldAccess`, `visitIdentifier`.

### Composite Recipe (Java)

```java
@Override
public List<Recipe> getRecipeList() {
    return List.of(
        new VehicleToCarRecipe(),
        new ChangeConstantsReference(),
        new VertxJdbcClientToPoolRecipe()
    );
}
```

### Common Visitor Operations

```java
// Import management — handles dedup, ordering, unused removal
maybeAddImport("new.package.NewClass");
maybeRemoveImport("old.package.OldClass");

// Type-aware checks (preferred over simple-name string match)
if (TypeUtils.isOfClassType(type, "com.example.OldClass")) { ... }

// Build a replacement type
JavaType.ShallowClass newType = JavaType.ShallowClass.build("com.example.NewClass");

// Preserve formatting when constructing/copying nodes
element.withPrefix(element.getPrefix());
```

### YAML Recipe Composition

```yaml
---
type: specs.openrewrite.org/v1beta/recipe
name: com.recipies.yaml.MyMigration
displayName: My migration
description: ...
recipeList:
  - org.openrewrite.java.ChangeType:
      oldFullyQualifiedTypeName: old.package.Old
      newFullyQualifiedTypeName: new.package.New
  - org.openrewrite.java.ChangeMethodName:
      methodPattern: com.example.Service oldMethod(..)
      newMethodName: newMethod
  - com.rewrite.MyJavaRecipe          # reference Java recipes by FQN
  - com.recipies.yaml.OtherYamlRecipe # or other YAML recipes
```

## VII. Testing

Test classes implement `RewriteTest`, set the recipe via `defaults(RecipeSpec)`, and use `rewriteRun(java(before, after))` with **complete class definitions and imports** (not fragments).

```java
class MyRecipeTest implements RewriteTest {
    @Override public void defaults(RecipeSpec spec) {
        spec.recipe(new MyRecipe());
    }

    @Test
    void transformsSimpleCase() {
        rewriteRun(java(
            """
            package com.example;
            public class Before {}
            """,
            """
            package com.example;
            public class After {}
            """
        ));
    }
}
```

**Edge cases to cover:** generics, nested classes, wildcards, multiple inheritance, null cases, import preservation.

**Reference test files** for patterns:
- `VehicleToCarRecipeTest` — simple transformation
- `ChangeConstantsReferenceTest` — complex edge cases
- `VertxJdbcMigrationRecipeTest` — comprehensive migration testing
- `UpgradeToJava25RecipeTest` — build/YAML-driven recipe testing

## VIII. Common Issues

| Problem | Solution |
|---------|----------|
| `Could not get unknown property` | Library not in `libs.versions.toml`, or accessor name wrong (hyphens → dots) |
| Test runs but transformation not applied | Recipe not registered in `rewrite.yml` (for YAML usage) or `getVisitor()` returns null |
| Imports not updating | Use `maybeAddImport`/`maybeRemoveImport`; verify import node preservation |
| Test fails: expected X got Y | Use complete class definition; verify imports in expected output; whitespace matters |
| Type not recognized in visitor | Ensure classpath has type definitions; check imports in test code |
| Whitespace changes unexpectedly | Use `withPrefix()` to preserve leading whitespace when constructing nodes |
| Performance degradation | Avoid unnecessary AST traversals; override specific visitor methods, not generic `visit()` |

**Recipe not applying:** check registration in `rewrite.yml` (if invoked by name), verify visitor matches target nodes, test with minimal example.

**Tests failing:** run `./gradlew clean test` to clear cache; verify exact whitespace in expected output.

## IX. Conventions

- **Packages:** `com.rewrite.*` (subpackages allowed, e.g. `com.rewrite.jdk25Upgrade`)
- **YAML recipe names:** `com.recipies.yaml.*`
- **Class names:** PascalCase, recipes end with `Recipe`
- **Test classes:** match recipe name + `Test` suffix
- **Indentation:** 4 spaces
- **JavaDoc:** simple one-liner on public classes/methods; focus on *what* and *why*, not *how*
- **Branch prefixes:** `feature/`, `fix/`, `docs/`, `test/`, `refactor/`, `chore/`
- **Commits:** Conventional Commits (`feat:`, `fix:`, etc.)

## X. Pre-commit Checklist

- [ ] `./gradlew test` passes
- [ ] `./gradlew clean build` succeeds
- [ ] If recipe is invoked by name, it's registered in `META-INF/rewrite/rewrite.yml`
- [ ] `getVisitor()` or `getRecipeList()` returns non-null
- [ ] Imports added/removed via `maybeAddImport`/`maybeRemoveImport`
- [ ] Tests use complete class definitions with imports
- [ ] Edge cases tested (generics, nested classes, wildcards, null)
- [ ] No hardcoded values where configuration would be appropriate

## XI. Interaction Guidelines

- **Ask clarifying questions** when scope, target packages, or recipe approach (YAML vs Java) is ambiguous.
- **Request explicit consent** before large multi-file edits; show a summary of planned changes first.
- Small edits, code snippets, and individual file modifications can be provided directly.
- Recommend simplest solution: existing OpenRewrite recipe → YAML composition → custom Java recipe.

## XII. Resources

- Recipe catalog: https://docs.openrewrite.org/recipes
- Reference API: https://docs.openrewrite.org/reference/api
- Vert.x docs: https://vertx.io/docs/
