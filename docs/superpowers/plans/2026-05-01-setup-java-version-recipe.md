# SetupJavaVersionTo25 Recipe Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a simple YAML-only OpenRewrite recipe that updates `java-version` (any → `25`) in GitHub Actions workflow files, plus a bash script that drops an `init.gradle` into a target repo, runs `rewriteRun`, and cleans up.

**Architecture:** One YAML recipe entry in `META-INF/rewrite/rewrite.yml` (uses `org.openrewrite.yaml.ChangePropertyValue` scoped via `filePattern`). One bash script (`apply-recipe.sh`) inlines the `init.gradle` body via heredoc, copies it to `$TARGET/init.gradle`, runs `./gradlew --init-script init.gradle rewriteRun`, removes the file via `trap ... EXIT`. One markdown doc covering script + manual usage.

**Tech Stack:** OpenRewrite 8.80.1 (rewrite-yaml `ChangePropertyValue`), JUnit 5 + `RewriteTest`, AssertJ, Bash, Gradle 9 init scripts, OpenRewrite Gradle plugin 7.23.0.

---

## File Structure

| Action | Path | Purpose |
|---|---|---|
| Modify | `src/main/resources/META-INF/rewrite/rewrite.yml` | Append the new YAML recipe `com.recipies.yaml.SetupJavaVersionTo25`. |
| Create | `src/test/java/com/rewrite/SetupJavaVersionTo25Test.java` | Verify the recipe rewrites `java-version` in workflow YAML and ignores other YAML. |
| Create | `apply-recipe.sh` (repo root) | End-to-end script: writes `init.gradle` to target, runs `rewriteRun`, cleans up. |
| Create | `docs/SetupJavaVersionTo25Recipe.md` | Quickstart, manual path, Windows note, verify steps. |

---

### Task 1: Add the YAML recipe entry

**Files:**
- Modify: `src/main/resources/META-INF/rewrite/rewrite.yml` (append at end)

- [ ] **Step 1: Append the recipe entry**

Append the following YAML block to the end of `src/main/resources/META-INF/rewrite/rewrite.yml` (note the leading `---` separator, matching existing style):

```yaml
---
type: specs.openrewrite.org/v1beta/recipe
name: com.recipies.yaml.SetupJavaVersionTo25
displayName: Set GitHub Actions setup-java java-version to 25
description: >-
  Updates the java-version key in actions/setup-java steps inside GitHub Actions
  workflow files to 25, regardless of the current value.
recipeList:
  - org.openrewrite.yaml.ChangePropertyValue:
      propertyKey: "**.java-version"
      newValue: "25"
      filePattern: "**/.github/workflows/*.yml"
```

- [ ] **Step 2: Verify the YAML still parses**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

(If the YAML is malformed OpenRewrite resource scanning will fail at recipe-load time, not compile time, so this is a sanity check only — the real verification happens in Task 2.)

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/META-INF/rewrite/rewrite.yml
git commit -m "feat: add SetupJavaVersionTo25 yaml recipe"
```

---

### Task 2: Add the recipe test

**Files:**
- Create: `src/test/java/com/rewrite/SetupJavaVersionTo25Test.java`

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/rewrite/SetupJavaVersionTo25Test.java` with the following content. The test loads the YAML recipe by name via `Environment.scanRuntimeClasspath()` (same pattern used in existing `RewriteYamlFileTest.java`).

```java
package com.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.yaml.Assertions.yaml;

/**
 * Tests for the YAML recipe com.recipies.yaml.SetupJavaVersionTo25.
 *
 * Verifies that java-version in GitHub workflow files is rewritten to 25, and that
 * java-version keys in unrelated YAML files are not touched.
 */
class SetupJavaVersionTo25Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
                Environment.builder()
                        .scanRuntimeClasspath()
                        .build()
                        .activateRecipes("com.recipies.yaml.SetupJavaVersionTo25")
        );
    }

    @Test
    void rewritesJavaVersionInWorkflowYaml() {
        rewriteRun(
                yaml(
                        """
                        name: CI
                        on: [push]
                        jobs:
                          build:
                            runs-on: ubuntu-latest
                            steps:
                              - uses: actions/checkout@v4
                              - uses: actions/setup-java@v4
                                with:
                                  distribution: 'temurin'
                                  java-version: '21'
                        """,
                        """
                        name: CI
                        on: [push]
                        jobs:
                          build:
                            runs-on: ubuntu-latest
                            steps:
                              - uses: actions/checkout@v4
                              - uses: actions/setup-java@v4
                                with:
                                  distribution: 'temurin'
                                  java-version: '25'
                        """,
                        spec -> spec.path(".github/workflows/ci.yml")
                )
        );
    }

    @Test
    void rewritesAnyValueToJava25() {
        rewriteRun(
                yaml(
                        """
                        jobs:
                          build:
                            steps:
                              - uses: actions/setup-java@v4
                                with:
                                  java-version: '11'
                        """,
                        """
                        jobs:
                          build:
                            steps:
                              - uses: actions/setup-java@v4
                                with:
                                  java-version: '25'
                        """,
                        spec -> spec.path(".github/workflows/build.yml")
                )
        );
    }

    @Test
    void doesNotTouchYamlOutsideWorkflowsDir() {
        rewriteRun(
                yaml(
                        """
                        config:
                          java-version: '21'
                        """,
                        spec -> spec.path("config/app.yml")
                )
        );
    }
}
```

- [ ] **Step 2: Run the test — expect failures (recipe not yet found OR no rewrite happens)**

Run: `./gradlew test --tests 'com.rewrite.SetupJavaVersionTo25Test'`

Expected one of two outcomes BEFORE the recipe entry from Task 1 is in place:
- Test fails with `RecipeException` / "could not find recipe com.recipies.yaml.SetupJavaVersionTo25", **OR**
- If Task 1 was already done, all three tests should PASS.

**If Task 1 is already complete, this step is the green run; proceed to Step 3.** If Task 1 was somehow skipped, go back and complete it.

- [ ] **Step 3: Confirm all three tests pass**

Run: `./gradlew test --tests 'com.rewrite.SetupJavaVersionTo25Test'`
Expected: BUILD SUCCESSFUL, 3 tests, 0 failures.

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/rewrite/SetupJavaVersionTo25Test.java
git commit -m "test: cover SetupJavaVersionTo25 recipe behavior"
```

---

### Task 3: Implement `apply-recipe.sh`

**Files:**
- Create: `apply-recipe.sh` (repo root)

- [ ] **Step 1: Create the script**

Create `apply-recipe.sh` at the repo root with the following content:

```bash
#!/usr/bin/env bash
#
# apply-recipe.sh — applies com.recipies.yaml.SetupJavaVersionTo25 to a target
# Gradle repo by writing init.gradle into it, running rewriteRun, and cleaning up.
#
# Usage:
#   ./apply-recipe.sh                     # operates on $PWD
#   ./apply-recipe.sh /path/to/target     # operates on the given path
#
# Prerequisite (run once, in this recipes repo):
#   ./gradlew publishToMavenLocal
#
# Compatibility:
#   - macOS / Linux: native bash.
#   - Windows: run inside Git Bash or WSL.

set -euo pipefail

TARGET="${1:-$PWD}"

if [ ! -d "$TARGET" ]; then
  echo "error: target directory does not exist: $TARGET" >&2
  exit 1
fi

if [ ! -x "$TARGET/gradlew" ]; then
  echo "error: $TARGET does not contain an executable gradlew. This script only supports Gradle projects." >&2
  exit 1
fi

INIT_FILE="$TARGET/init.gradle"

if [ -e "$INIT_FILE" ]; then
  echo "error: $INIT_FILE already exists. Refusing to overwrite. Move or delete it and re-run." >&2
  exit 1
fi

cleanup() {
  rm -f "$INIT_FILE"
}
trap cleanup EXIT

cat <<'INIT' > "$INIT_FILE"
// Auto-generated by apply-recipe.sh — do not edit, will be removed on script exit.
initscript {
    repositories {
        mavenLocal()
        mavenCentral()
        maven { url 'https://plugins.gradle.org/m2/' }
    }
    dependencies {
        classpath 'org.openrewrite:plugin:7.23.0'
        classpath 'com.rewrite:openrewrite-custom-recipes:1.0.0'
    }
}

allprojects {
    apply plugin: org.openrewrite.gradle.RewritePlugin

    repositories {
        mavenLocal()
        mavenCentral()
    }

    afterEvaluate {
        if (project.extensions.findByName('rewrite')) {
            rewrite {
                activeRecipe('com.recipies.yaml.SetupJavaVersionTo25')
            }
            dependencies {
                rewrite('com.rewrite:openrewrite-custom-recipes:1.0.0')
            }
        }
    }
}
INIT

echo "==> Running rewriteRun in $TARGET"
(
  cd "$TARGET"
  ./gradlew --init-script init.gradle rewriteRun
)
echo "==> Done. init.gradle has been removed from the target."
```

- [ ] **Step 2: Make the script executable**

Run: `chmod +x apply-recipe.sh`
Expected: no output.

- [ ] **Step 3: Smoke-test the preflight (no published recipe yet, no target needed)**

Run: `./apply-recipe.sh /nonexistent/path/abc123`
Expected: prints `error: target directory does not exist: /nonexistent/path/abc123`, exit code 1.

Run: `./apply-recipe.sh "$PWD"`
Expected: this repo has its own `gradlew`, so the preflight passes and the script will attempt `rewriteRun`. Either let it run to completion or `Ctrl-C`. After exit, verify the cleanup trap removed `init.gradle`:

Run: `ls init.gradle 2>/dev/null && echo "LEAKED" || echo "cleaned up"`
Expected: `cleaned up`.

- [ ] **Step 4: Commit**

```bash
git add apply-recipe.sh
git commit -m "feat: add apply-recipe.sh for SetupJavaVersionTo25"
```

---

### Task 4: Write usage doc

**Files:**
- Create: `docs/SetupJavaVersionTo25Recipe.md`

- [ ] **Step 1: Create the doc**

Create `docs/SetupJavaVersionTo25Recipe.md` with the following content:

````markdown
# SetupJavaVersionTo25 — usage guide

Updates the `java-version` key in `actions/setup-java` steps inside GitHub Actions workflow files (`.github/workflows/*.yml`) to `25`. Any current value is replaced. Other YAML files are untouched.

Recipe name: `com.recipies.yaml.SetupJavaVersionTo25`

## Quickstart (script path)

1. Publish the recipe JAR locally — once, from this repo:

   ```bash
   ./gradlew publishToMavenLocal
   ```

2. Apply it to a target Gradle repo:

   ```bash
   # Either pass the target path:
   /path/to/openrewrite-custom-recipes/apply-recipe.sh /path/to/target-repo

   # Or cd into the target first and run with no args:
   cd /path/to/target-repo
   /path/to/openrewrite-custom-recipes/apply-recipe.sh
   ```

3. The script writes a temporary `init.gradle` in the target, runs `./gradlew --init-script init.gradle rewriteRun`, then removes `init.gradle` on exit (success, failure, or interrupt).

4. Verify:

   ```bash
   git -C /path/to/target-repo diff .github/workflows
   ```

## Manual path (no script)

Useful on Windows without Git Bash, or for inspecting/customizing the init script:

1. In this repo: `./gradlew publishToMavenLocal`.

2. In the target repo, create `init.gradle` at the root with:

   ```gradle
   initscript {
       repositories {
           mavenLocal()
           mavenCentral()
           maven { url 'https://plugins.gradle.org/m2/' }
       }
       dependencies {
           classpath 'org.openrewrite:plugin:7.23.0'
           classpath 'com.rewrite:openrewrite-custom-recipes:1.0.0'
       }
   }

   allprojects {
       apply plugin: org.openrewrite.gradle.RewritePlugin

       repositories {
           mavenLocal()
           mavenCentral()
       }

       afterEvaluate {
           if (project.extensions.findByName('rewrite')) {
               rewrite {
                   activeRecipe('com.recipies.yaml.SetupJavaVersionTo25')
               }
               dependencies {
                   rewrite('com.rewrite:openrewrite-custom-recipes:1.0.0')
               }
           }
       }
   }
   ```

3. From the target repo root:

   ```bash
   ./gradlew --init-script init.gradle rewriteRun
   ```

4. Inspect the diff, then delete `init.gradle` when you're done.

## Windows

- The script (`apply-recipe.sh`) requires bash. Use **Git Bash** (ships with Git for Windows) or **WSL**. Steps are otherwise identical.
- The **manual path** works in plain PowerShell — it's just `gradlew` commands.

## Troubleshooting

- **"could not resolve com.rewrite:openrewrite-custom-recipes:1.0.0"** — you forgot `./gradlew publishToMavenLocal` in the recipes repo.
- **"could not find recipe com.recipies.yaml.SetupJavaVersionTo25"** — your locally published JAR is stale; re-run `publishToMavenLocal` after pulling latest.
- **Workflow YAML using `.yaml` extension** — the recipe targets `*.yml` only. If you need `.yaml`, widen `filePattern` in the recipe.
````

- [ ] **Step 2: Commit**

```bash
git add docs/SetupJavaVersionTo25Recipe.md
git commit -m "docs: add SetupJavaVersionTo25 usage guide"
```

---

### Task 5: End-to-end verification

**Files:** none (sandbox is created in `/tmp` and discarded).

- [ ] **Step 1: Publish the recipe JAR locally**

Run: `./gradlew publishToMavenLocal`
Expected: BUILD SUCCESSFUL. JAR appears under `~/.m2/repository/com/rewrite/openrewrite-custom-recipes/1.0.0/`.

- [ ] **Step 2: Build a sandbox target repo**

Run:

```bash
SANDBOX=$(mktemp -d -t setup-java-recipe-sandbox-XXXX)
cd "$SANDBOX"
mkdir -p .github/workflows
cat > .github/workflows/ci.yml <<'YML'
name: CI
on: [push]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
YML
cat > settings.gradle <<'EOF'
rootProject.name = 'sandbox'
EOF
cat > build.gradle <<'EOF'
plugins { id 'java' }
repositories { mavenCentral() }
EOF
gradle wrapper --gradle-version 9.0 --distribution-type bin
echo "Sandbox: $SANDBOX"
```

Expected: a Gradle wrapper exists in `$SANDBOX`. The pre-existing `gradle` CLI on PATH is used to bootstrap the wrapper. If `gradle` is not on PATH, install it (`brew install gradle`) or copy the wrapper from this repo: `cp -r /path/to/openrewrite-custom-recipes/gradle . && cp /path/to/openrewrite-custom-recipes/gradlew .`.

- [ ] **Step 3: Run the script against the sandbox**

Run: `/path/to/openrewrite-custom-recipes/apply-recipe.sh "$SANDBOX"`
Expected: Gradle resolves the locally published recipe, runs `rewriteRun`, and prints a summary listing one changed file: `.github/workflows/ci.yml`.

- [ ] **Step 4: Verify the workflow was rewritten**

Run: `grep 'java-version' "$SANDBOX/.github/workflows/ci.yml"`
Expected: a single line containing `java-version: '25'`.

Run: `ls "$SANDBOX/init.gradle" 2>/dev/null && echo "LEAKED" || echo "cleaned up"`
Expected: `cleaned up`.

- [ ] **Step 5: Clean up the sandbox**

Run: `rm -rf "$SANDBOX"`
Expected: no output.

- [ ] **Step 6: Final full-suite test run**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL. The new `SetupJavaVersionTo25Test` is included; no existing tests regress.

- [ ] **Step 7: No commit**

This task only verifies behavior; no source changes were produced.

---

## Self-review

- **Spec coverage:** Recipe entry (Task 1), recipe test (Task 2), `apply-recipe.sh` (Task 3), docs covering script + manual + Windows (Task 4), end-to-end verification including a real sandbox repo (Task 5). All four files in the spec's File Structure are accounted for. ✅
- **Placeholders:** none. All code is fully written; commands are concrete; expected outputs are specified. ✅
- **Type/name consistency:** Recipe name `com.recipies.yaml.SetupJavaVersionTo25` is used identically in the YAML, the test, the script, and the docs. JAR coordinates `com.rewrite:openrewrite-custom-recipes:1.0.0` match `build.gradle`. Plugin coordinate `org.openrewrite:plugin:7.23.0` matches `gradle/libs.versions.toml`. ✅
- **TDD:** Task 2 writes the test against the recipe added in Task 1. Order is acceptable because the recipe is YAML data, not implementation logic — the "implementation" is the data file itself, and the test exercises it via classpath scanning. ✅
- **Scope:** Single subsystem, no decomposition needed. ✅
