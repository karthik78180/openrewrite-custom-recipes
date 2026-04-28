# UpgradeToJava25Recipe — Design Spec

**Date:** 2026-04-27
**Status:** Approved

## Goal

Provide a single composite OpenRewrite recipe that upgrades a target Java/Kotlin Gradle (or Maven) project to Java 25, including:

1. JVM toolchain / `sourceCompatibility` / `targetCompatibility` to Java 25.
2. Kotlin JVM toolchain to 25 when present.
3. GitHub Actions workflow `javaVersion` field from `21` to `25`.

Intended invocation: applied from a target repo via an `init.gradle` script that registers this recipe without modifying the target's `build.gradle`.

## Architecture

A single Java composite recipe in package `com.rewrite.jdk25Upgrade`:

- **Class:** `com.rewrite.jdk25Upgrade.UpgradeToJava25Recipe`
- **Type:** `Recipe` with `getRecipeList()` returning the three delegate recipes below.

### Delegate Recipes

| Recipe | Purpose | Module |
|--------|---------|--------|
| `org.openrewrite.java.migrate.UpgradeBuildToJava25` | Java toolchain + source/target compat in `build.gradle`/`pom.xml` | `rewrite-migrate-java` |
| `org.openrewrite.java.migrate.UpgradeBuildToJava25ForKotlin` | Kotlin `jvmToolchain` updates | `rewrite-migrate-java` |
| `org.openrewrite.yaml.ChangePropertyValue` | `javaVersion: 21` → `javaVersion: 25`, scoped to `.github/workflows/**/*.yml` | `rewrite-yaml` |

The Kotlin recipe is safe to run on non-Kotlin projects (no-op).

## Source/Target Compat Edge Cases

The `UpgradeBuildToJava25` recipe handles the following target build.gradle states:

- **Only `sourceCompatibility = JavaVersion.VERSION_21`** → updated to `VERSION_25`. `targetCompatibility` defaults to source, so bytecode also targets 25.
- **Only `targetCompatibility` set** → updated to `VERSION_25`. The recipe does not auto-add a missing `sourceCompatibility`; user should verify both are present to avoid compile failures.
- **Only `toolchain.languageVersion = 21`** → updated to `25`. Gradle derives source/target from the toolchain.
- **Both set** → both updated.

This behavior is documented in the recipe docs.

## Files Created / Modified

### New Files
- `src/main/java/com/rewrite/jdk25Upgrade/UpgradeToJava25Recipe.java`
- `src/test/java/com/rewrite/jdk25Upgrade/UpgradeToJava25RecipeTest.java`
- `docs/UpgradeToJava25Recipe.md`
- `docs/init.gradle` (sample init script for target repos)

### Modified Files
- `gradle/libs.versions.toml` — add `rewrite-migrate-java`, `rewrite-yaml` library entries
- `build.gradle` — declare new implementation deps
- `src/main/resources/META-INF/rewrite/rewrite.yml` — register new recipe inside `AllMigrations` is OUT OF SCOPE; recipe is standalone
- `README.md` — add new entry

## Tests

Test class extends `RewriteTest`. Covers:

1. **Groovy `build.gradle`** — toolchain `21` → `25`.
2. **Groovy `build.gradle`** — `sourceCompatibility`/`targetCompatibility` `21` → `25`.
3. **Source-only** — only `sourceCompatibility` set, becomes `25`.
4. **Kotlin DSL `build.gradle.kts`** — `jvmToolchain(21)` → `jvmToolchain(25)`.
5. **GitHub workflow YAML** — `javaVersion: 21` → `javaVersion: 25`.
6. **No-op test** — already-Java-25 build is unchanged.
7. **Recipe metadata** — display name, description, recipe list size.

YAML tests use `org.openrewrite.yaml.Assertions.yaml()` with file matchers for `.github/workflows/*.yml`.

## init.gradle Usage (target repo)

The init script lets a target repo apply this recipe without editing its own `build.gradle`:

```groovy
// init.gradle in target repo
initscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        classpath "org.openrewrite:plugin:7.23.0"
        classpath "com.rewrite:openrewrite-custom-recipes:1.0.0"
    }
}

allprojects {
    apply plugin: org.openrewrite.gradle.RewritePlugin

    afterEvaluate {
        if (project.extensions.findByName('rewrite')) {
            rewrite {
                activeRecipe('com.rewrite.jdk25Upgrade.UpgradeToJava25Recipe')
            }
        }
    }
}
```

Invocation:
```bash
./gradlew --init-script init.gradle rewriteRun
```

Prerequisite: `./gradlew publishToMavenLocal` must be run from this repo first so the recipe jar is in `~/.m2`.

## Out of Scope

- Adding `UpgradeToJava25Recipe` to `Java21MigrationRecipes` composite (different domain).
- Adding to `com.recipies.yaml.AllMigrations` YAML composite.
- Bumping the project's own toolchain — already on Java 25.
- Mocking absent fields (e.g., adding `sourceCompatibility` if missing).
