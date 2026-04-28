# Repo Maintenance Recipes — Design

**Date:** 2026-04-28
**Status:** Approved (brainstorming complete)
**Source:** `newrequirement.text`

## 1. Goals

Add three independent OpenRewrite transformations that complement the existing JDK 25 upgrade, plus a top-level composite that bundles them with `UpgradeToJava25Recipe`:

1. Update specific keys/values in `lambda.json` configs and delete two obsolete keys.
2. Bump declared dependency and plugin versions in target Gradle projects.
3. Apply Checkstyle-derived autoformatting without leaving any Checkstyle config on disk in the target repo.

These features are **not** Java-25-specific. They live in a new package `com.rewrite.repoMaintenance` so the `jdk25Upgrade` package stays focused on the language upgrade.

## 2. Non-goals

- No XML file injection/deletion in the target repo (decided: encode rules as OpenRewrite recipes + a shipped `style.yml`).
- No naming-convention auto-fixes (renames are checks, not safe transforms — see §10).
- No line-length auto-wrapping in this scope (see §10).
- No runtime-overridable config (constants are Java code; edit + republish to change).

## 3. Architecture

### 3.1 Package layout

```
src/main/java/com/rewrite/
├── jdk25Upgrade/
│   └── UpgradeToJava25Recipe.java                    # existing — untouched
└── repoMaintenance/                                  # NEW
    ├── PostJava25UpgradeRecipe.java                  # top-level composite
    ├── lambdaJson/
    │   ├── LambdaJsonConstants.java
    │   └── UpdateLambdaJsonRecipe.java
    ├── dependencies/
    │   ├── DependencyVersionConstants.java
    │   └── UpgradeProjectDependenciesRecipe.java
    └── checkstyle/
        └── CheckstyleAutoFormatRecipe.java

src/main/resources/META-INF/rewrite/
├── rewrite.yml                                       # existing — extended with YAML mirror
└── style.yml                                         # NEW — code style for AutoFormat

src/test/java/com/rewrite/repoMaintenance/...         # mirrors main, tests per feature
```

### 3.2 Composition order in `PostJava25UpgradeRecipe`

1. `UpgradeToJava25Recipe` — JDK toolchain + workflow YAML
2. `UpdateLambdaJsonRecipe` — JSON edits
3. `UpgradeProjectDependenciesRecipe` — dependency/plugin bumps
4. `CheckstyleAutoFormatRecipe` — runs last so format covers anything upstream recipes touched

Each sub-recipe is also independently invokable so a target repo can opt into one leg.

### 3.3 YAML mirror

`com.recipies.yaml.PostJava25Upgrade` in `rewrite.yml` runs the same four legs but uses `com.recipies.yaml.UpgradeToJava25` for the JDK leg (which honors the rewrite-migrate-java Kotlin module preconditions).

## 4. Feature 1 — `UpdateLambdaJsonRecipe`

### 4.1 File pattern

`**/config/*/lambda.json`

Covers single-module layout (`config/{name}/lambda.json` at repo root) and monorepo layout (`{module}/config/{name}/lambda.json`).

### 4.2 Constants (`LambdaJsonConstants.java`)

| Key path | New value | Operation |
|---|---|---|
| `$.runtime` | `"java25"` | ChangeValue (string) |
| `$.handler` | `"com.example.Handler"` | ChangeValue (string) |
| `$.deploymentCordinates.region` | `"us-east-1"` | ChangeValue (string) |
| `$.functionVersion` | — | DeleteKey |
| `$.version` | — | DeleteKey |

> Values are placeholder examples; will be edited in-place in the constants file before publishing for real use.

### 4.3 Composition

Recipe extends `Recipe`, returns five sub-recipes from `getRecipeList()`:
- 3 × `org.openrewrite.json.ChangeValue`, each scoped to `FILE_PATTERN`
- 2 × `org.openrewrite.json.DeleteKey`, each scoped to `FILE_PATTERN`

### 4.4 Edge-case behavior

- File missing → no-op (matcher matches nothing).
- Target key absent → built-in recipe behavior is no-op for both `ChangeValue` and `DeleteKey`.
- `deploymentCordinates` absent → top-level updates still run; nested skipped silently.
- File outside `**/config/*/lambda.json` → not touched.

### 4.5 Tests (`UpdateLambdaJsonRecipeTest`)

1. Single-module: `config/myfunc/lambda.json` — all values updated, both deletes happen.
2. Monorepo: `services/api/config/api/lambda.json` — same.
3. JSON file outside the pattern is unchanged.
4. Already-correct values → no-op.
5. `deploymentCordinates` absent → top-level still updated, nested silently skipped.
6. Recipe metadata + composite size is 5.

## 5. Feature 2 — `UpgradeProjectDependenciesRecipe`

### 5.1 Constants (`DependencyVersionConstants.java`)

```java
public record Gav(String groupId, String artifactId, String newVersion) {}
public record PluginUpgrade(String pluginId, String newVersion) {}

public static final List<Gav> DEPENDENCIES = List.of(
    new Gav("io.vertx",            "vertx-core",  "5.0.5"),
    new Gav("org.springframework", "spring-core", "6.2.0")
);

public static final List<PluginUpgrade> PLUGINS = List.of(
    new PluginUpgrade("org.springframework.boot", "3.4.0")
);
```

> Placeholder list; edit before real-world use.

### 5.2 Composition

For each `Gav` → `new org.openrewrite.gradle.UpgradeDependencyVersion(groupId, artifactId, newVersion, null, null, null)`.
For each `PluginUpgrade` → `new org.openrewrite.gradle.plugins.UpgradePluginVersion(pluginId, newVersion, null, null)`.

### 5.3 Coverage of version sources

| Source location | Supported | Notes |
|---|---|---|
| Inline `build.gradle` (Groovy) | ✅ | First-class |
| Inline `build.gradle.kts` (Kotlin DSL) | ✅ | First-class |
| `gradle/libs.versions.toml` via `version.ref` | ✅ | Recipe walks the ref and updates `[versions]` |
| `settings.gradle` `gradle.ext.fooVersion` indirection | ⚠️ | Pinned by test; if it fails, **documented** as a limitation rather than fixed with a custom visitor (see CLAUDE.md §II — "use existing recipes first") |

### 5.4 Tests (`UpgradeProjectDependenciesRecipeTest`)

1. Inline `build.gradle` (Groovy) upgrade.
2. Inline `build.gradle.kts` (Kotlin DSL) upgrade.
3. `libs.versions.toml` `[versions]` update via `version.ref`.
4. Plugin upgrade in `plugins { id 'org.springframework.boot' version '3.3.0' }`.
5. `settings.gradle` `gradle.ext.springVersion` — pin behavior (pass or `@Disabled` with comment if unsupported).
6. No-op when dep is at or above target.
7. No-op when dep is absent.
8. Recipe metadata + composite size = `DEPENDENCIES.size() + PLUGINS.size()`.

## 6. Feature 3 — `CheckstyleAutoFormatRecipe` + `style.yml`

### 6.1 `style.yml` (shipped in recipe JAR)

```yaml
---
type: specs.openrewrite.org/v1beta/style
name: com.rewrite.repoMaintenance.checkstyle.RepoCheckstyleStyle
displayName: Repo checkstyle-derived style
description: Style settings derived from the project's Checkstyle configuration.
styleConfigs:
  - org.openrewrite.java.style.TabsAndIndentsStyle:
      useTabCharacter: false
      tabSize: 2
      indentSize: 2
      continuationIndent: 4
  - org.openrewrite.java.style.ImportLayoutStyle:
      classCountToUseStarImport: 999
      nameCountToUseStarImport: 999
      layout:
        - import all other imports
        - <blank line>
        - import javax.*
        - <blank line>
        - import java.*
        - <blank line>
        - import static all other imports
  - org.openrewrite.java.style.SpacesStyle: {}
  - org.openrewrite.java.style.BlankLinesStyle:
      keepMaximum:
        inDeclarations: 1
        inCode: 1
```

### 6.2 Recipe ordering inside `CheckstyleAutoFormatRecipe`

Structural cleanup first, formatting last so `AutoFormat` sees a clean tree.

| # | Recipe | Checkstyle module mapped |
|---|---|---|
| 1 | `org.openrewrite.java.OrderImports` (groups `*`, `javax`, `java`; remove unused; static separated; alphabetical) | `ImportOrder`, `RedundantImport`, `UnusedImports` |
| 2 | `org.openrewrite.java.format.RemoveTrailingWhitespace` | `RegexpSingleline` (trailing spaces) |
| 3 | `org.openrewrite.java.format.BlankLines` (max 1 consecutive) | `RegexpMultiline`, `EmptyLineSeparator` |
| 4 | `org.openrewrite.java.format.EmptyNewlineAtEndOfFile` | `NewlineAtEndOfFile` |
| 5 | `org.openrewrite.staticanalysis.ModifierOrder` | `ModifierOrder` |
| 6 | `org.openrewrite.staticanalysis.RedundantModifier` | `RedundantModifier` |
| 7 | `org.openrewrite.staticanalysis.SimplifyBooleanExpression` | `SimplifyBooleanExpression` |
| 8 | `org.openrewrite.staticanalysis.SimplifyBooleanReturn` | `SimplifyBooleanReturn` |
| 9 | `org.openrewrite.staticanalysis.EqualsAvoidsNull` | `EqualsAvoidNull` |
| 10 | `org.openrewrite.staticanalysis.EmptyBlock` | `EmptyStatement` (closest available) |
| 11 | `org.openrewrite.staticanalysis.UseJavaStyleArrayDeclarations` | `ArrayTypeStyle` |
| 12 | `org.openrewrite.java.format.AutoFormat` | `Indentation`, `LeftCurly`, `RightCurly`, `GenericWhitespace`, `OperatorWrap`, `ParenPad`, `MethodParamPad`, `WhitespaceAfter`, `WhitespaceAround`, `TypecastParenPad`, `EmptyForIteratorPad`, `NoWhitespaceAfter`, `NoWhitespaceBefore`, `SingleSpaceSeparator` |

### 6.3 Tests (`CheckstyleAutoFormatRecipeTest`)

1. Imports reordered into `*`, `javax`, `java`, then static.
2. Trailing whitespace removed.
3. Multiple blank lines collapsed to one.
4. Missing newline-at-EOF added.
5. Modifier order corrected (`static public final` → `public static final`).
6. Redundant `public` on interface method removed.
7. 4-space indentation reformatted to 2-space (proves `style.yml` is applied).
8. `if (x == true)` simplified to `if (x)`.
9. Already-formatted file → no-op.
10. Recipe metadata + composite size check.

## 7. Top-level composite (`PostJava25UpgradeRecipe`)

```java
public List<Recipe> getRecipeList() {
    return List.of(
        new UpgradeToJava25Recipe(),
        new UpdateLambdaJsonRecipe(),
        new UpgradeProjectDependenciesRecipe(),
        new CheckstyleAutoFormatRecipe()
    );
}
```

YAML mirror in `rewrite.yml`:

```yaml
---
type: specs.openrewrite.org/v1beta/recipe
name: com.recipies.yaml.PostJava25Upgrade
displayName: Java 25 upgrade + repo maintenance
description: Bundles the JDK 25 upgrade with lambda.json updates, dependency bumps, and Checkstyle autoformat.
recipeList:
  - com.recipies.yaml.UpgradeToJava25
  - com.rewrite.repoMaintenance.UpdateLambdaJsonRecipe
  - com.rewrite.repoMaintenance.UpgradeProjectDependenciesRecipe
  - com.rewrite.repoMaintenance.CheckstyleAutoFormatRecipe
```

## 8. Target-repo invocation

Existing `docs/init.gradle` activates `com.rewrite.jdk25Upgrade.UpgradeToJava25Recipe`. Updated entry-point matrix to ship with the docs:

| Entry point | What it runs |
|---|---|
| `com.rewrite.jdk25Upgrade.UpgradeToJava25Recipe` | Java 25 upgrade only (existing) |
| `com.rewrite.repoMaintenance.UpdateLambdaJsonRecipe` | lambda.json updates only |
| `com.rewrite.repoMaintenance.UpgradeProjectDependenciesRecipe` | dependency/plugin bumps only |
| `com.rewrite.repoMaintenance.CheckstyleAutoFormatRecipe` | format only |
| `com.rewrite.repoMaintenance.PostJava25UpgradeRecipe` | all four (recommended for full migration) |
| `com.recipies.yaml.PostJava25Upgrade` | all four, JDK leg uses Kotlin-precondition variant |

Run:
```bash
./gradlew --init-script /path/to/init.gradle rewriteRun
```

## 9. Build dependency change

`gradle/libs.versions.toml`:
```toml
openrewrite-static-analysis = { module = "org.openrewrite.recipe:rewrite-static-analysis", version = "<latest compatible with bom 8.80.1>" }
```

`build.gradle`:
```groovy
implementation libs.openrewrite.static.analysis
```

Exact version pinned during implementation; verify with `./gradlew dependencies`. No toolchain or BOM changes.

## 10. Future enhancements (out of scope)

- **Line-length enforcement (Checkstyle `LineLength max=120`).** No clean OpenRewrite mapping. A best-effort `WrapLongLinesRecipe` is feasible but high-effort and produces aesthetically variable output. Tracked.
- **Naming conventions.** `ConstantName`, `LocalFinalVariableName`, `LocalVariableName`, `MemberName`, `MethodName`, `ParameterName`, `StaticVariableName`, `TypeName`. These are *checks*, not safe auto-fixes; renames need import + usage updates with semantic care.
- **Other unmapped checks.** `IllegalImport`, `IllegalInstantiation`, `EqualsHashCode`, `CovariantEquals`, `MissingSwitchDefault`, `InterfaceIsType`, custom `AnnotationLocation` variant.
- **Externalize constants to YAML/properties resource files** for edit-without-recompile workflows (current design uses Java constants classes; resource-file variant tracked).

## 11. Risks and mitigations

| # | Risk | Mitigation |
|---|---|---|
| R1 | `gradle.ext.fooVersion` indirection from `settings.gradle` may not resolve via `UpgradeDependencyVersion` | Pinned in tests; document as known limitation if it fails. No custom visitor. |
| R2 | `rewrite-static-analysis` version mismatch with `openrewrite-bom 8.80.1` | Pin to a release published against the same BOM line; verify with `./gradlew dependencies`. |
| R3 | `style.yml` not picked up if recipe JAR isn't on the target's classpath | Already addressed: `init.gradle` adds `mavenLocal()` and the recipe JAR. Tests in this module exercise the same classpath. |
| R4 | `OrderImports` and `AutoFormat` interacting non-idempotently | Composite tests assert idempotence (second run → no diff). |
| R5 | Constants drift from real-world repos | Constants files are intentionally findable and editable; documented in the README for each recipe. |

## 12. Documentation deliverables

- `docs/PostJava25UpgradeRecipe.md`
- `docs/UpdateLambdaJsonRecipe.md`
- `docs/UpgradeProjectDependenciesRecipe.md`
- `docs/CheckstyleAutoFormatRecipe.md`

Each follows the structure of the existing `docs/UpgradeToJava25Recipe.md`: what it does, edge cases, usage, switching between Java vs YAML entry points, testing, references.

## 13. Pre-merge checklist

- [ ] `./gradlew test` green (full suite)
- [ ] `./gradlew clean build` green
- [ ] All four new recipes registered in `META-INF/rewrite/rewrite.yml` (where invoked by name)
- [ ] `style.yml` present at `META-INF/rewrite/style.yml` and on the JAR's classpath
- [ ] `gradle.ext` indirection test outcome documented in `UpgradeProjectDependenciesRecipe.md`
- [ ] Idempotence test for `CheckstyleAutoFormatRecipe` passes
- [ ] Per-recipe `docs/*.md` files added
