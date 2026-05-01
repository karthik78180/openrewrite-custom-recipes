# setup-java-version recipe + apply script — design

**Date:** 2026-05-01
**Branch:** `feature/setup-java-version-recipe`

## Problem

A target repo wants to bump the `java-version` key in its GitHub Actions `actions/setup-java` steps to `25` without editing its `build.gradle`. The recipe should be a dependency the target consumes via an `init.gradle`, and a helper script should apply it end-to-end.

## Scope

- One simple YAML recipe that updates `java-version` in workflow YAML.
- One bash script that drops an `init.gradle` into the target repo, runs `rewriteRun`, and cleans up.
- Documentation covering the script path and an equivalent manual path.

Out of scope: Maven targets, Windows-native shell (Windows uses Git Bash / WSL), version configurability (always sets to `25`).

## Design

### 1. YAML recipe

Append to `src/main/resources/META-INF/rewrite/rewrite.yml`:

```yaml
---
type: specs.openrewrite.org/v1beta/recipe
name: com.recipies.yaml.SetupJavaVersionTo25
displayName: Set GitHub Actions setup-java java-version to 25
description: >-
  Updates the java-version key in actions/setup-java steps inside GitHub Actions
  workflow files to 25, regardless of current value.
recipeList:
  - org.openrewrite.yaml.ChangePropertyValue:
      propertyKey: "**.java-version"
      newValue: "25"
      filePattern: "**/.github/workflows/*.yml"
```

`oldValue` omitted → matches any current value. `filePattern` confines the change to workflow YAML so unrelated `java-version` keys are untouched.

### 2. `apply-recipe.sh`

Bash script at the repo root. Behavior:

- `./apply-recipe.sh [target-repo-path]` — defaults to `pwd`.
- Validates target exists and contains `gradlew`. Bails clearly otherwise.
- Writes `init.gradle` into target root via heredoc (`cat <<'INIT' > "$TARGET/init.gradle"`).
- Registers `trap 'rm -f "$TARGET/init.gradle"' EXIT` so the file is removed on success, failure, or interrupt.
- Runs `(cd "$TARGET" && ./gradlew --init-script init.gradle rewriteRun)`.

The heredoc body mirrors the pattern in `docs/init.gradle` (already proven in this repo) but activates the new recipe:

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

### 3. Documentation

`docs/setup-java-version-recipe.md`:

1. **Quickstart** — publish recipe locally, run script.
2. **Manual path** — copy/paste the `init.gradle` snippet, run `./gradlew --init-script init.gradle rewriteRun` directly.
3. **Windows note** — script runs in Git Bash or WSL; manual path also works in plain PowerShell since the commands are pure Gradle.
4. **Verify** — `git diff` on workflow files.

### 4. Test

Unit test covering: workflow YAML with `java-version: '21'` → becomes `'25'`; non-workflow YAML with `java-version` is untouched.

## Risks / open items

- `**.java-version` is broad. Mitigated by `filePattern` confining matches to `.github/workflows/*.yml`.
- Plugin version `7.23.0` is hardcoded in the heredoc — fine for now, mirrors existing `docs/init.gradle`.
- `*.yaml` extension (vs `*.yml`) is not matched. If we hit such a repo we'll widen `filePattern`. YAGNI for now.
