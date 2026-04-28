# UpgradeToJava25Recipe

Upgrades a target project to Java 25 by updating Gradle/Maven build files and GitHub Actions workflow YAML.

## What It Does

This composite recipe runs three transformations:

| # | Recipe | What it changes |
|---|--------|-----------------|
| 1 | `org.openrewrite.java.migrate.UpgradeJavaVersion(25)` | `toolchain.languageVersion`, `sourceCompatibility`, `targetCompatibility` in `build.gradle`, `build.gradle.kts`, `pom.xml` |
| 2 | (YAML variant only) `UpgradeBuildToJava25` + `UpgradeBuildToJava25ForKotlin` | Same as above, but with module-level Kotlin/non-Kotlin preconditions. Skips Kotlin modules below `kotlin-stdlib` 2.3. |
| 3 | `org.openrewrite.yaml.ChangePropertyValue` | `javaVersion: '21'` → `javaVersion: '25'` in `.github/workflows/*.yml` |

The Java entry point `com.rewrite.jdk25Upgrade.UpgradeToJava25Recipe` uses (1) and (3). The YAML entry point `com.recipies.yaml.UpgradeToJava25` uses (2) and (3).

## Source / Target Compatibility Edge Cases

The underlying `UpgradeJavaVersion` recipe handles these target build.gradle states:

### Only `sourceCompatibility` set
**Before:**
```gradle
java {
    sourceCompatibility = JavaVersion.VERSION_21
}
```
**After:**
```gradle
java {
    sourceCompatibility = JavaVersion.VERSION_25
}
```
`targetCompatibility` defaults to `sourceCompatibility`, so emitted bytecode targets Java 25.

### Only `targetCompatibility` set
**Before:**
```gradle
java {
    targetCompatibility = JavaVersion.VERSION_21
}
```
**After:**
```gradle
java {
    targetCompatibility = JavaVersion.VERSION_25
}
```
`sourceCompatibility` defaults to the JDK running Gradle. If that JDK is older than 25, compilation fails. **Mitigation:** ensure your toolchain or `sourceCompatibility` is also at 25. The recipe will not auto-add a missing field.

### Only `toolchain.languageVersion` set
**Before:**
```gradle
java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}
```
**After:**
```gradle
java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
}
```
Gradle infers `sourceCompatibility`/`targetCompatibility` from the toolchain. No source/target fields needed.

### Both source and target set
Both are updated to `25`.

## Workflow YAML Update

Looks for the `javaVersion` key at any nesting level inside `.github/workflows/*.yml`:

**Before:**
```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    javaVersion: '21'
```

**After:**
```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    javaVersion: '25'
```

YAML files outside `.github/workflows/` are not touched.

## Usage in a Target Repo

You don't need to edit the target repo's `build.gradle`. Use the included `init.gradle` script.

### Step 1: Publish this recipe locally
From this repo:
```bash
./gradlew publishToMavenLocal
```
Installs `com.rewrite:openrewrite-custom-recipes:1.0.0` into `~/.m2`.

### Step 2: Drop `init.gradle` into the target repo (or any path)
Copy `docs/init.gradle` from this repo. The script:
- Adds `mavenLocal()` to find this recipe jar.
- Applies the OpenRewrite Gradle plugin to all projects.
- Activates `com.rewrite.jdk25Upgrade.UpgradeToJava25Recipe` (or the YAML variant).

### Step 3: Run from the target repo
```bash
./gradlew --init-script /path/to/init.gradle rewriteRun
```

Or, if you placed `init.gradle` at `~/.gradle/init.d/init.gradle`, Gradle picks it up automatically and you just run:
```bash
./gradlew rewriteRun
```

### Step 4: Review and commit
Inspect the diff, run your tests, and commit.

## Switching Between Java vs YAML Entry Points

Edit the `activeRecipe(...)` line in `init.gradle`:

| Entry point | When to use |
|-------------|-------------|
| `com.rewrite.jdk25Upgrade.UpgradeToJava25Recipe` | Default. Single Java recipe, no preconditions. Good when you know your target is non-Kotlin or on Kotlin 2.3+. |
| `com.recipies.yaml.UpgradeToJava25` | Use when your target has mixed Kotlin/non-Kotlin modules, or has Kotlin `<` 2.3 modules you want to skip. Honors module-level preconditions from `rewrite-migrate-java`. |

## Testing

Run the test suite:
```bash
./gradlew test --tests 'com.rewrite.jdk25Upgrade.UpgradeToJava25RecipeTest'
```

Tests cover:
- Groovy `build.gradle` toolchain upgrade
- `sourceCompatibility` + `targetCompatibility` upgrade
- Source-only variant
- Kotlin DSL `build.gradle.kts` toolchain upgrade
- GitHub workflow `javaVersion` update
- No-op on YAML files outside `.github/workflows/`
- No-op on already-Java-25 builds
- Recipe metadata and composite structure

## References

- [UpgradeBuildToJava25](https://docs.openrewrite.org/recipes/java/migrate/upgradebuildtojava25)
- [UpgradeBuildToJava25ForKotlin](https://docs.openrewrite.org/recipes/java/migrate/upgradebuildtojava25forkotlin)
- [ChangePropertyValue](https://docs.openrewrite.org/recipes/yaml/changepropertyvalue)
- [Gradle init scripts](https://docs.gradle.org/current/userguide/init_scripts.html)
