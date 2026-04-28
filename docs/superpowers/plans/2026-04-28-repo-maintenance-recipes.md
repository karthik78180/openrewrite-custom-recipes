# Repo Maintenance Recipes — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add three OpenRewrite recipes (`UpdateLambdaJsonRecipe`, `UpgradeProjectDependenciesRecipe`, `CheckstyleAutoFormatRecipe`) under a new `com.rewrite.repoMaintenance` package, plus a top-level `PostJava25UpgradeRecipe` composite that bundles them with the existing `UpgradeToJava25Recipe`.

**Architecture:** Each new recipe is a thin Java composite of built-in OpenRewrite recipes; "what to change" is held in dedicated Java constants classes; checkstyle-derived formatting is encoded as recipes plus a `META-INF/rewrite/style.yml` shipped in the recipe JAR. No XML files are written into target repos.

**Tech Stack:** Java 25, Gradle 9+, OpenRewrite 8.80.1 (`rewrite-java`, `rewrite-gradle`, `rewrite-yaml`, `rewrite-json`, `rewrite-migrate-java`, `rewrite-static-analysis`), JUnit 5, AssertJ.

**Reference spec:** [`docs/superpowers/specs/2026-04-28-repo-maintenance-recipes-design.md`](../specs/2026-04-28-repo-maintenance-recipes-design.md)

---

## File Structure

**New Java sources (`src/main/java/com/rewrite/repoMaintenance/`):**
| File | Responsibility |
|---|---|
| `PostJava25UpgradeRecipe.java` | Top-level composite: JDK25 + 3 maintenance recipes |
| `lambdaJson/LambdaJsonConstants.java` | JSON-path strings, target string values, file pattern, delete-key paths |
| `lambdaJson/UpdateLambdaJsonRecipe.java` | Composite of 3×`ChangeValue` + 2×`DeleteKey` |
| `dependencies/DependencyVersionConstants.java` | `Gav`, `PluginUpgrade` records and lists |
| `dependencies/UpgradeProjectDependenciesRecipe.java` | Composite of `UpgradeDependencyVersion` × N + `UpgradePluginVersion` × M |
| `checkstyle/CheckstyleAutoFormatRecipe.java` | Composite of formatting + static-analysis recipes |

**New test sources (`src/test/java/com/rewrite/repoMaintenance/`):** mirror main, one test class per recipe.

**New resource:** `src/main/resources/META-INF/rewrite/style.yml`

**Modified:** `src/main/resources/META-INF/rewrite/rewrite.yml`, `gradle/libs.versions.toml`, `build.gradle`.

**New docs:** `docs/PostJava25UpgradeRecipe.md`, `docs/UpdateLambdaJsonRecipe.md`, `docs/UpgradeProjectDependenciesRecipe.md`, `docs/CheckstyleAutoFormatRecipe.md`.

---

## Task 1: Add `rewrite-static-analysis` dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle`

- [ ] **Step 1: Look up the latest `rewrite-static-analysis` version compatible with `openrewrite-bom 8.80.1`**

Run:
```bash
curl -s "https://search.maven.org/solrsearch/select?q=g:%22org.openrewrite.recipe%22+AND+a:%22rewrite-static-analysis%22&rows=5&core=gav&wt=json" | python3 -m json.tool | grep '"v"' | head -5
```
Expected: a list of recent versions (e.g., `2.x.x`). Pick the newest stable release. If `curl` fails, default to `2.10.0` and adjust if Step 4 fails.

- [ ] **Step 2: Add the version entry to `gradle/libs.versions.toml`**

Add this line under `[libraries]` after the existing `openrewrite-migrate-java = ...` line:

```toml
openrewrite-static-analysis = { module = "org.openrewrite.recipe:rewrite-static-analysis", version = "<VERSION_FROM_STEP_1>" }
```

- [ ] **Step 3: Add the implementation dependency to `build.gradle`**

In `build.gradle`, inside the `dependencies { ... }` block, add this line right after `implementation libs.openrewrite.migrate.java`:

```groovy
    implementation libs.openrewrite.static.analysis
```

- [ ] **Step 4: Verify the dependency resolves**

Run: `./gradlew dependencies --configuration compileClasspath | grep rewrite-static-analysis`
Expected: a line like `+--- org.openrewrite.recipe:rewrite-static-analysis:2.x.x`. If it errors, downgrade or upgrade the version in `libs.versions.toml` until it resolves.

- [ ] **Step 5: Verify compile still works**

Run: `./gradlew compileJava compileTestJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml build.gradle
git commit -m "build: add rewrite-static-analysis dependency"
```

---

## Task 2: Lambda JSON recipe — constants, tests, implementation

**Files:**
- Create: `src/main/java/com/rewrite/repoMaintenance/lambdaJson/LambdaJsonConstants.java`
- Create: `src/main/java/com/rewrite/repoMaintenance/lambdaJson/UpdateLambdaJsonRecipe.java`
- Create: `src/test/java/com/rewrite/repoMaintenance/lambdaJson/UpdateLambdaJsonRecipeTest.java`

- [ ] **Step 1: Create `LambdaJsonConstants.java`**

```java
package com.rewrite.repoMaintenance.lambdaJson;

/** Hardcoded keys, target values, and file pattern for the lambda.json updater. */
public final class LambdaJsonConstants {

    private LambdaJsonConstants() {
    }

    /** Glob covering single-module (config/{name}/lambda.json) and monorepo ({module}/config/{name}/lambda.json). */
    public static final String FILE_PATTERN = "**/config/*/lambda.json";

    public static final String RUNTIME_PATH = "$.runtime";
    public static final String RUNTIME_VALUE = "java25";

    public static final String HANDLER_PATH = "$.handler";
    public static final String HANDLER_VALUE = "com.example.Handler";

    public static final String REGION_PATH = "$.deploymentCordinates.region";
    public static final String REGION_VALUE = "us-east-1";

    public static final String DELETE_FUNCTION_VERSION_PATH = "$.functionVersion";
    public static final String DELETE_VERSION_PATH = "$.version";
}
```

- [ ] **Step 2: Write the test class with all 6 tests**

Create `src/test/java/com/rewrite/repoMaintenance/lambdaJson/UpdateLambdaJsonRecipeTest.java`:

```java
package com.rewrite.repoMaintenance.lambdaJson;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.json.Assertions.json;

class UpdateLambdaJsonRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpdateLambdaJsonRecipe());
    }

    @Test
    void singleModuleLayout() {
        rewriteRun(
                json(
                        """
                        {
                          "runtime": "nodejs18.x",
                          "handler": "old.Handler",
                          "functionVersion": "5",
                          "version": "1.0",
                          "deploymentCordinates": {
                            "region": "eu-west-1"
                          }
                        }
                        """,
                        """
                        {
                          "runtime": "java25",
                          "handler": "com.example.Handler",
                          "deploymentCordinates": {
                            "region": "us-east-1"
                          }
                        }
                        """,
                        spec -> spec.path("config/myfunc/lambda.json")
                )
        );
    }

    @Test
    void monorepoLayout() {
        rewriteRun(
                json(
                        """
                        {
                          "runtime": "nodejs18.x",
                          "handler": "old.Handler",
                          "functionVersion": "5",
                          "version": "1.0",
                          "deploymentCordinates": {
                            "region": "eu-west-1"
                          }
                        }
                        """,
                        """
                        {
                          "runtime": "java25",
                          "handler": "com.example.Handler",
                          "deploymentCordinates": {
                            "region": "us-east-1"
                          }
                        }
                        """,
                        spec -> spec.path("services/api/config/api/lambda.json")
                )
        );
    }

    @Test
    void doesNotTouchJsonOutsidePattern() {
        rewriteRun(
                json(
                        """
                        {
                          "runtime": "nodejs18.x",
                          "functionVersion": "5"
                        }
                        """,
                        spec -> spec.path("some/other/lambda.json")
                )
        );
    }

    @Test
    void noOpWhenAlreadyCorrect() {
        rewriteRun(
                json(
                        """
                        {
                          "runtime": "java25",
                          "handler": "com.example.Handler",
                          "deploymentCordinates": {
                            "region": "us-east-1"
                          }
                        }
                        """,
                        spec -> spec.path("config/myfunc/lambda.json")
                )
        );
    }

    @Test
    void deploymentCoordinatesAbsent_topLevelStillUpdates() {
        rewriteRun(
                json(
                        """
                        {
                          "runtime": "nodejs18.x",
                          "handler": "old.Handler"
                        }
                        """,
                        """
                        {
                          "runtime": "java25",
                          "handler": "com.example.Handler"
                        }
                        """,
                        spec -> spec.path("config/myfunc/lambda.json")
                )
        );
    }

    @Test
    void recipeMetadataAndCompositeSize() {
        UpdateLambdaJsonRecipe recipe = new UpdateLambdaJsonRecipe();
        assertThat(recipe.getDisplayName()).isNotBlank();
        assertThat(recipe.getDescription()).contains("lambda.json");
        List<Recipe> sub = recipe.getRecipeList();
        assertThat(sub).hasSize(5);
    }
}
```

- [ ] **Step 3: Run the tests to confirm they fail (recipe class doesn't exist)**

Run: `./gradlew test --tests 'com.rewrite.repoMaintenance.lambdaJson.UpdateLambdaJsonRecipeTest'`
Expected: compilation error — `cannot find symbol class UpdateLambdaJsonRecipe`.

- [ ] **Step 4: Implement `UpdateLambdaJsonRecipe.java`**

```java
package com.rewrite.repoMaintenance.lambdaJson;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Recipe;
import org.openrewrite.json.ChangeValue;
import org.openrewrite.json.DeleteKey;

import java.util.List;

import static com.rewrite.repoMaintenance.lambdaJson.LambdaJsonConstants.*;

/**
 * Updates fixed keys in lambda.json files (runtime, handler, deploymentCordinates.region)
 * and deletes obsolete top-level keys (functionVersion, version).
 *
 * Scoped to {@link LambdaJsonConstants#FILE_PATTERN}.
 */
public class UpdateLambdaJsonRecipe extends Recipe {

    @Override
    public @NotNull String getDisplayName() {
        return "Update lambda.json keys and delete obsolete keys";
    }

    @Override
    public @NotNull String getDescription() {
        return "In lambda.json files matching " + FILE_PATTERN + ", sets runtime, handler, "
                + "and deploymentCordinates.region to fixed values; deletes top-level "
                + "functionVersion and version keys.";
    }

    @Override
    public @NotNull List<Recipe> getRecipeList() {
        return List.of(
                new ChangeValue(RUNTIME_PATH, RUNTIME_VALUE, FILE_PATTERN),
                new ChangeValue(HANDLER_PATH, HANDLER_VALUE, FILE_PATTERN),
                new ChangeValue(REGION_PATH, REGION_VALUE, FILE_PATTERN),
                new DeleteKey(DELETE_FUNCTION_VERSION_PATH, FILE_PATTERN),
                new DeleteKey(DELETE_VERSION_PATH, FILE_PATTERN)
        );
    }
}
```

> **Note on constructor signatures:** if `./gradlew compileJava` fails on the `ChangeValue` or `DeleteKey` constructors, open the OpenRewrite source jar in your IDE (or run `./gradlew :dependencies` to confirm the version) and adjust the argument count. As of `rewrite-yaml`/`rewrite-json` aligned with `rewrite-bom 8.80.1`, the 3-arg `ChangeValue(jsonPath, value, filePattern)` and 2-arg `DeleteKey(jsonPath, filePattern)` are the expected forms; if they have changed, supply `null`s for any new optional positional args.

- [ ] **Step 5: Run the tests and confirm they pass**

Run: `./gradlew test --tests 'com.rewrite.repoMaintenance.lambdaJson.UpdateLambdaJsonRecipeTest'`
Expected: `BUILD SUCCESSFUL` with 6 tests passed.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/rewrite/repoMaintenance/lambdaJson \
        src/test/java/com/rewrite/repoMaintenance/lambdaJson
git commit -m "feat: add UpdateLambdaJsonRecipe for lambda.json maintenance"
```

---

## Task 3: Dependencies recipe — constants, tests, implementation

**Files:**
- Create: `src/main/java/com/rewrite/repoMaintenance/dependencies/DependencyVersionConstants.java`
- Create: `src/main/java/com/rewrite/repoMaintenance/dependencies/UpgradeProjectDependenciesRecipe.java`
- Create: `src/test/java/com/rewrite/repoMaintenance/dependencies/UpgradeProjectDependenciesRecipeTest.java`

- [ ] **Step 1: Create `DependencyVersionConstants.java`**

```java
package com.rewrite.repoMaintenance.dependencies;

import java.util.List;

/** Hardcoded GAV → version and pluginId → version maps for the dependency upgrader. */
public final class DependencyVersionConstants {

    private DependencyVersionConstants() {
    }

    public record Gav(String groupId, String artifactId, String newVersion) {
    }

    public record PluginUpgrade(String pluginId, String newVersion) {
    }

    public static final List<Gav> DEPENDENCIES = List.of(
            new Gav("io.vertx", "vertx-core", "5.0.5"),
            new Gav("org.springframework", "spring-core", "6.2.0")
    );

    public static final List<PluginUpgrade> PLUGINS = List.of(
            new PluginUpgrade("org.springframework.boot", "3.4.0")
    );
}
```

- [ ] **Step 2: Write the test class with 8 tests**

Create `src/test/java/com/rewrite/repoMaintenance/dependencies/UpgradeProjectDependenciesRecipeTest.java`:

```java
package com.rewrite.repoMaintenance.dependencies;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.toml.Assertions.versionCatalog;

class UpgradeProjectDependenciesRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeProjectDependenciesRecipe());
    }

    @Test
    void inlineGroovyBuildGradleUpgrade() {
        rewriteRun(
                buildGradle(
                        """
                        plugins { id 'java' }
                        repositories { mavenCentral() }
                        dependencies {
                            implementation 'io.vertx:vertx-core:3.9.16'
                        }
                        """,
                        """
                        plugins { id 'java' }
                        repositories { mavenCentral() }
                        dependencies {
                            implementation 'io.vertx:vertx-core:5.0.5'
                        }
                        """
                )
        );
    }

    @Test
    void inlineKotlinDslBuildGradleUpgrade() {
        rewriteRun(
                buildGradleKts(
                        """
                        plugins { java }
                        repositories { mavenCentral() }
                        dependencies {
                            implementation("org.springframework:spring-core:5.3.0")
                        }
                        """,
                        """
                        plugins { java }
                        repositories { mavenCentral() }
                        dependencies {
                            implementation("org.springframework:spring-core:6.2.0")
                        }
                        """
                )
        );
    }

    @Test
    void versionCatalogRefUpdated() {
        rewriteRun(
                versionCatalog(
                        """
                        [versions]
                        vertx = "3.9.16"

                        [libraries]
                        vertx-core = { module = "io.vertx:vertx-core", version.ref = "vertx" }
                        """,
                        """
                        [versions]
                        vertx = "5.0.5"

                        [libraries]
                        vertx-core = { module = "io.vertx:vertx-core", version.ref = "vertx" }
                        """
                )
        );
    }

    @Test
    void pluginUpgradeInPluginsBlock() {
        rewriteRun(
                buildGradle(
                        """
                        plugins {
                            id 'org.springframework.boot' version '3.3.0'
                        }
                        """,
                        """
                        plugins {
                            id 'org.springframework.boot' version '3.4.0'
                        }
                        """
                )
        );
    }

    @Disabled("settings.gradle gradle.ext indirection is not first-class in UpgradeDependencyVersion; "
            + "tracked as a known limitation in UpgradeProjectDependenciesRecipe.md")
    @Test
    void settingsGradleExtPropertyIndirection() {
        rewriteRun(
                settingsGradle(
                        """
                        gradle.ext.vertxVersion = '3.9.16'
                        """,
                        """
                        gradle.ext.vertxVersion = '5.0.5'
                        """
                ),
                buildGradle(
                        """
                        plugins { id 'java' }
                        repositories { mavenCentral() }
                        dependencies {
                            implementation "io.vertx:vertx-core:${gradle.ext.vertxVersion}"
                        }
                        """
                )
        );
    }

    @Test
    void noOpWhenAlreadyAtTargetVersion() {
        rewriteRun(
                buildGradle(
                        """
                        plugins { id 'java' }
                        repositories { mavenCentral() }
                        dependencies {
                            implementation 'io.vertx:vertx-core:5.0.5'
                        }
                        """
                )
        );
    }

    @Test
    void noOpWhenDependencyAbsent() {
        rewriteRun(
                buildGradle(
                        """
                        plugins { id 'java' }
                        repositories { mavenCentral() }
                        dependencies {
                            implementation 'org.unrelated:lib:1.0.0'
                        }
                        """
                )
        );
    }

    @Test
    void recipeMetadataAndCompositeSize() {
        UpgradeProjectDependenciesRecipe recipe = new UpgradeProjectDependenciesRecipe();
        assertThat(recipe.getDisplayName()).isNotBlank();
        assertThat(recipe.getDescription()).contains("dependency");
        List<Recipe> sub = recipe.getRecipeList();
        assertThat(sub).hasSize(
                DependencyVersionConstants.DEPENDENCIES.size()
                        + DependencyVersionConstants.PLUGINS.size());
    }
}
```

> **Note on TOML test imports:** if `org.openrewrite.gradle.toml.Assertions.versionCatalog` is not on the classpath, replace that import and test with the equivalent helper from your installed version (search `Assertions` in `rewrite-gradle` jars). If unavailable, drop the test for now and document under "known limitation".

- [ ] **Step 3: Run the tests to confirm they fail**

Run: `./gradlew test --tests 'com.rewrite.repoMaintenance.dependencies.UpgradeProjectDependenciesRecipeTest'`
Expected: compilation error — `cannot find symbol class UpgradeProjectDependenciesRecipe`.

- [ ] **Step 4: Implement `UpgradeProjectDependenciesRecipe.java`**

```java
package com.rewrite.repoMaintenance.dependencies;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Recipe;
import org.openrewrite.gradle.UpgradeDependencyVersion;
import org.openrewrite.gradle.plugins.UpgradePluginVersion;

import java.util.ArrayList;
import java.util.List;

import static com.rewrite.repoMaintenance.dependencies.DependencyVersionConstants.DEPENDENCIES;
import static com.rewrite.repoMaintenance.dependencies.DependencyVersionConstants.PLUGINS;

/**
 * Bumps dependency and plugin versions declared in build.gradle / build.gradle.kts /
 * libs.versions.toml. Mappings are held in {@link DependencyVersionConstants}.
 */
public class UpgradeProjectDependenciesRecipe extends Recipe {

    @Override
    public @NotNull String getDisplayName() {
        return "Upgrade declared dependency and plugin versions";
    }

    @Override
    public @NotNull String getDescription() {
        return "Upgrades each dependency listed in DependencyVersionConstants.DEPENDENCIES via "
                + "UpgradeDependencyVersion, and each plugin in PLUGINS via UpgradePluginVersion. "
                + "Resolves version literals in inline build.gradle/build.gradle.kts and "
                + "libs.versions.toml version-refs automatically.";
    }

    @Override
    public @NotNull List<Recipe> getRecipeList() {
        List<Recipe> list = new ArrayList<>(DEPENDENCIES.size() + PLUGINS.size());
        for (DependencyVersionConstants.Gav gav : DEPENDENCIES) {
            list.add(new UpgradeDependencyVersion(
                    gav.groupId(),
                    gav.artifactId(),
                    gav.newVersion(),
                    null,
                    null,
                    null
            ));
        }
        for (DependencyVersionConstants.PluginUpgrade plugin : PLUGINS) {
            list.add(new UpgradePluginVersion(
                    plugin.pluginId(),
                    plugin.newVersion(),
                    null
            ));
        }
        return list;
    }
}
```

> **Note on constructor signatures:** if `./gradlew compileJava` reports a constructor mismatch, open the recipe source via your IDE and adjust the trailing `null` count. The `UpgradeDependencyVersion` constructor in BOM 8.80.1 typically accepts `(groupId, artifactId, newVersion, versionPattern, overrideManagedVersion, ?)` — supply `null` for any positional optional.

- [ ] **Step 5: Run the tests and confirm they pass**

Run: `./gradlew test --tests 'com.rewrite.repoMaintenance.dependencies.UpgradeProjectDependenciesRecipeTest'`
Expected: 7 tests pass, 1 disabled (`settingsGradleExtPropertyIndirection`).

If `versionCatalogRefUpdated` fails, this is the documented known-limitation path: either remove the test or `@Disabled` it with a comment, and ensure `docs/UpgradeProjectDependenciesRecipe.md` (Task 8) documents the gap.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/rewrite/repoMaintenance/dependencies \
        src/test/java/com/rewrite/repoMaintenance/dependencies
git commit -m "feat: add UpgradeProjectDependenciesRecipe for dependency/plugin bumps"
```

---

## Task 4: Add `style.yml` shipped style

**Files:**
- Create: `src/main/resources/META-INF/rewrite/style.yml`

- [ ] **Step 1: Create `style.yml`**

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

- [ ] **Step 2: Verify the style file is on the classpath**

Run: `./gradlew processResources && find build/resources/main/META-INF/rewrite -type f`
Expected output includes both `rewrite.yml` and `style.yml`.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/META-INF/rewrite/style.yml
git commit -m "feat: ship checkstyle-derived style.yml in recipe JAR"
```

---

## Task 5: Checkstyle autoformat recipe — tests, implementation

**Files:**
- Create: `src/main/java/com/rewrite/repoMaintenance/checkstyle/CheckstyleAutoFormatRecipe.java`
- Create: `src/test/java/com/rewrite/repoMaintenance/checkstyle/CheckstyleAutoFormatRecipeTest.java`

- [ ] **Step 1: Write the test class with 10 tests**

Create `src/test/java/com/rewrite/repoMaintenance/checkstyle/CheckstyleAutoFormatRecipeTest.java`:

```java
package com.rewrite.repoMaintenance.checkstyle;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class CheckstyleAutoFormatRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new CheckstyleAutoFormatRecipe());
    }

    @Test
    void importsReorderedIntoGroups() {
        rewriteRun(
                java(
                        """
                        package com.example;

                        import java.util.List;
                        import com.acme.Other;
                        import javax.annotation.Nullable;

                        public class A {
                          public List<String> a;
                          public Other o;
                          public Nullable n;
                        }
                        """,
                        """
                        package com.example;

                        import com.acme.Other;

                        import javax.annotation.Nullable;

                        import java.util.List;

                        public class A {
                          public List<String> a;
                          public Other o;
                          public Nullable n;
                        }
                        """
                )
        );
    }

    @Test
    void trailingWhitespaceRemoved() {
        rewriteRun(
                java(
                        "package com.example;   \n\npublic class A {}\n",
                        "package com.example;\n\npublic class A {}\n"
                )
        );
    }

    @Test
    void multipleBlankLinesCollapsed() {
        rewriteRun(
                java(
                        """
                        package com.example;



                        public class A {}
                        """,
                        """
                        package com.example;

                        public class A {}
                        """
                )
        );
    }

    @Test
    void newlineAtEndOfFileAdded() {
        rewriteRun(
                java(
                        "package com.example;\npublic class A {}",
                        "package com.example;\npublic class A {}\n"
                )
        );
    }

    @Test
    void modifierOrderCorrected() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class A {
                          static public final int X = 1;
                        }
                        """,
                        """
                        package com.example;
                        public class A {
                          public static final int X = 1;
                        }
                        """
                )
        );
    }

    @Test
    void redundantPublicOnInterfaceMethodRemoved() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public interface I {
                          public void doIt();
                        }
                        """,
                        """
                        package com.example;
                        public interface I {
                          void doIt();
                        }
                        """
                )
        );
    }

    @Test
    void indentationReformattedToTwoSpaces() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class A {
                            public void m() {
                                int x = 1;
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class A {
                          public void m() {
                            int x = 1;
                          }
                        }
                        """
                )
        );
    }

    @Test
    void simplifyBooleanComparisonToTrue() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class A {
                          public boolean m(boolean x) {
                            if (x == true) { return true; }
                            return false;
                          }
                        }
                        """,
                        """
                        package com.example;
                        public class A {
                          public boolean m(boolean x) {
                            return x;
                          }
                        }
                        """
                )
        );
    }

    @Test
    void alreadyFormattedFileIsNoOp() {
        rewriteRun(
                java(
                        """
                        package com.example;

                        public class A {
                          public int x = 1;
                        }
                        """
                )
        );
    }

    @Test
    void recipeMetadataAndCompositeSize() {
        CheckstyleAutoFormatRecipe recipe = new CheckstyleAutoFormatRecipe();
        assertThat(recipe.getDisplayName()).isNotBlank();
        assertThat(recipe.getDescription()).contains("Checkstyle");
        List<Recipe> sub = recipe.getRecipeList();
        assertThat(sub).hasSize(12);
    }
}
```

- [ ] **Step 2: Run the tests to confirm they fail**

Run: `./gradlew test --tests 'com.rewrite.repoMaintenance.checkstyle.CheckstyleAutoFormatRecipeTest'`
Expected: compilation error — `cannot find symbol class CheckstyleAutoFormatRecipe`.

- [ ] **Step 3: Implement `CheckstyleAutoFormatRecipe.java`**

```java
package com.rewrite.repoMaintenance.checkstyle;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Recipe;
import org.openrewrite.java.OrderImports;
import org.openrewrite.java.format.AutoFormat;
import org.openrewrite.java.format.BlankLines;
import org.openrewrite.java.format.EmptyNewlineAtEndOfFile;
import org.openrewrite.java.format.RemoveTrailingWhitespace;
import org.openrewrite.staticanalysis.EmptyBlock;
import org.openrewrite.staticanalysis.EqualsAvoidsNull;
import org.openrewrite.staticanalysis.ModifierOrder;
import org.openrewrite.staticanalysis.RedundantModifier;
import org.openrewrite.staticanalysis.SimplifyBooleanExpression;
import org.openrewrite.staticanalysis.SimplifyBooleanReturn;
import org.openrewrite.staticanalysis.UseJavaStyleArrayDeclarations;

import java.util.List;

/**
 * Apply Checkstyle-derived autoformat by composing built-in OpenRewrite recipes.
 * Style settings (indentation, import layout, blank lines) come from the JAR's
 * META-INF/rewrite/style.yml and apply via AutoFormat.
 */
public class CheckstyleAutoFormatRecipe extends Recipe {

    @Override
    public @NotNull String getDisplayName() {
        return "Apply Checkstyle-derived autoformat";
    }

    @Override
    public @NotNull String getDescription() {
        return "Composes OrderImports, RemoveTrailingWhitespace, BlankLines, "
                + "EmptyNewlineAtEndOfFile, ModifierOrder, RedundantModifier, "
                + "SimplifyBooleanExpression, SimplifyBooleanReturn, EqualsAvoidsNull, "
                + "EmptyBlock, UseJavaStyleArrayDeclarations, and AutoFormat to enforce a "
                + "Checkstyle-compatible style without writing any Checkstyle config to the "
                + "target repo.";
    }

    @Override
    public @NotNull List<Recipe> getRecipeList() {
        return List.of(
                new OrderImports(true),
                new RemoveTrailingWhitespace(null),
                new BlankLines(),
                new EmptyNewlineAtEndOfFile(),
                new ModifierOrder(),
                new RedundantModifier(),
                new SimplifyBooleanExpression(),
                new SimplifyBooleanReturn(),
                new EqualsAvoidsNull(),
                new EmptyBlock(),
                new UseJavaStyleArrayDeclarations(),
                new AutoFormat()
        );
    }
}
```

> **Note on constructor signatures:** if any of the recipe constructors require args you didn't supply or vice versa, adjust to the no-arg or single-arg form your installed version requires. `OrderImports(true)` passes `removeUnused=true`. `RemoveTrailingWhitespace(null)` accepts a `@Nullable` file pattern; if your version uses no-arg, drop the `null`.

- [ ] **Step 4: Run the tests and confirm they pass**

Run: `./gradlew test --tests 'com.rewrite.repoMaintenance.checkstyle.CheckstyleAutoFormatRecipeTest'`
Expected: 10 tests pass.

If `indentationReformattedToTwoSpaces` fails, the `style.yml` is not being picked up. Verify by running `./gradlew clean processResources && jar tf build/libs/openrewrite-custom-recipes-1.0.0.jar | grep style.yml`. If the file is in the jar but not applied, try invoking `AutoFormat` after explicitly naming the style via `.named(...)` — see OpenRewrite docs for `IntelliJ` style override pattern.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/rewrite/repoMaintenance/checkstyle \
        src/test/java/com/rewrite/repoMaintenance/checkstyle
git commit -m "feat: add CheckstyleAutoFormatRecipe encoding checkstyle.xml as recipes"
```

---

## Task 6: Top-level composite `PostJava25UpgradeRecipe`

**Files:**
- Create: `src/main/java/com/rewrite/repoMaintenance/PostJava25UpgradeRecipe.java`
- Create: `src/test/java/com/rewrite/repoMaintenance/PostJava25UpgradeRecipeTest.java`

- [ ] **Step 1: Write the test (one end-to-end smoke test + metadata test)**

Create `src/test/java/com/rewrite/repoMaintenance/PostJava25UpgradeRecipeTest.java`:

```java
package com.rewrite.repoMaintenance;

import com.rewrite.jdk25Upgrade.UpgradeToJava25Recipe;
import com.rewrite.repoMaintenance.checkstyle.CheckstyleAutoFormatRecipe;
import com.rewrite.repoMaintenance.dependencies.UpgradeProjectDependenciesRecipe;
import com.rewrite.repoMaintenance.lambdaJson.UpdateLambdaJsonRecipe;
import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.json.Assertions.json;

class PostJava25UpgradeRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new PostJava25UpgradeRecipe());
    }

    @Test
    void smokeTestAcrossAllFourLegs() {
        rewriteRun(
                buildGradle(
                        """
                        plugins { id 'java' }
                        repositories { mavenCentral() }
                        java {
                            toolchain { languageVersion = JavaLanguageVersion.of(21) }
                        }
                        dependencies {
                            implementation 'io.vertx:vertx-core:3.9.16'
                        }
                        """,
                        """
                        plugins { id 'java' }
                        repositories { mavenCentral() }
                        java {
                            toolchain { languageVersion = JavaLanguageVersion.of(25) }
                        }
                        dependencies {
                            implementation 'io.vertx:vertx-core:5.0.5'
                        }
                        """
                ),
                json(
                        """
                        {
                          "runtime": "nodejs18.x",
                          "handler": "old.Handler",
                          "functionVersion": "5",
                          "version": "1.0",
                          "deploymentCordinates": { "region": "eu-west-1" }
                        }
                        """,
                        """
                        {
                          "runtime": "java25",
                          "handler": "com.example.Handler",
                          "deploymentCordinates": { "region": "us-east-1" }
                        }
                        """,
                        spec -> spec.path("config/myfunc/lambda.json")
                ),
                java(
                        """
                        package com.example;
                        public class A {
                            static public final int X = 1;
                        }
                        """,
                        """
                        package com.example;
                        public class A {
                          public static final int X = 1;
                        }
                        """
                )
        );
    }

    @Test
    void recipeMetadataAndCompositeSize() {
        PostJava25UpgradeRecipe recipe = new PostJava25UpgradeRecipe();
        assertThat(recipe.getDisplayName()).isNotBlank();
        assertThat(recipe.getDescription()).contains("Java 25");
        List<Recipe> sub = recipe.getRecipeList();
        assertThat(sub).hasSize(4);
        assertThat(sub).anyMatch(r -> r instanceof UpgradeToJava25Recipe);
        assertThat(sub).anyMatch(r -> r instanceof UpdateLambdaJsonRecipe);
        assertThat(sub).anyMatch(r -> r instanceof UpgradeProjectDependenciesRecipe);
        assertThat(sub).anyMatch(r -> r instanceof CheckstyleAutoFormatRecipe);
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

Run: `./gradlew test --tests 'com.rewrite.repoMaintenance.PostJava25UpgradeRecipeTest'`
Expected: compilation error — `cannot find symbol class PostJava25UpgradeRecipe`.

- [ ] **Step 3: Implement `PostJava25UpgradeRecipe.java`**

```java
package com.rewrite.repoMaintenance;

import com.rewrite.jdk25Upgrade.UpgradeToJava25Recipe;
import com.rewrite.repoMaintenance.checkstyle.CheckstyleAutoFormatRecipe;
import com.rewrite.repoMaintenance.dependencies.UpgradeProjectDependenciesRecipe;
import com.rewrite.repoMaintenance.lambdaJson.UpdateLambdaJsonRecipe;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.Recipe;

import java.util.List;

/**
 * Top-level composite: bundles the JDK 25 upgrade with three repo-maintenance recipes.
 * Order: JDK upgrade → JSON edits → dependency bumps → format last.
 */
public class PostJava25UpgradeRecipe extends Recipe {

    @Override
    public @NotNull String getDisplayName() {
        return "Upgrade to Java 25 and run repo maintenance";
    }

    @Override
    public @NotNull String getDescription() {
        return "Composite: Java 25 build/workflow upgrade, lambda.json updates, "
                + "dependency/plugin version bumps, and Checkstyle-derived autoformat.";
    }

    @Override
    public @NotNull List<Recipe> getRecipeList() {
        return List.of(
                new UpgradeToJava25Recipe(),
                new UpdateLambdaJsonRecipe(),
                new UpgradeProjectDependenciesRecipe(),
                new CheckstyleAutoFormatRecipe()
        );
    }
}
```

- [ ] **Step 4: Run the tests and confirm they pass**

Run: `./gradlew test --tests 'com.rewrite.repoMaintenance.PostJava25UpgradeRecipeTest'`
Expected: 2 tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/rewrite/repoMaintenance/PostJava25UpgradeRecipe.java \
        src/test/java/com/rewrite/repoMaintenance/PostJava25UpgradeRecipeTest.java
git commit -m "feat: add PostJava25UpgradeRecipe top-level composite"
```

---

## Task 7: YAML mirror in `rewrite.yml`

**Files:**
- Modify: `src/main/resources/META-INF/rewrite/rewrite.yml`

- [ ] **Step 1: Append the new YAML composite to `rewrite.yml`**

Append (with the `---` separator) at the end of the file:

```yaml
---
type: specs.openrewrite.org/v1beta/recipe
name: com.recipies.yaml.PostJava25Upgrade
displayName: Java 25 upgrade + repo maintenance
description: Bundles the JDK 25 upgrade with lambda.json updates, dependency bumps, and Checkstyle autoformat.
recipeList:
  - com.recipies.yaml.UpgradeToJava25
  - com.rewrite.repoMaintenance.lambdaJson.UpdateLambdaJsonRecipe
  - com.rewrite.repoMaintenance.dependencies.UpgradeProjectDependenciesRecipe
  - com.rewrite.repoMaintenance.checkstyle.CheckstyleAutoFormatRecipe
```

- [ ] **Step 2: Verify the YAML parses by running the full test suite**

Run: `./gradlew test`
Expected: all tests pass (an invalid `rewrite.yml` would fail recipe-loading tests).

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/META-INF/rewrite/rewrite.yml
git commit -m "feat: add YAML mirror com.recipies.yaml.PostJava25Upgrade"
```

---

## Task 8: Documentation

**Files:**
- Create: `docs/UpdateLambdaJsonRecipe.md`
- Create: `docs/UpgradeProjectDependenciesRecipe.md`
- Create: `docs/CheckstyleAutoFormatRecipe.md`
- Create: `docs/PostJava25UpgradeRecipe.md`

- [ ] **Step 1: Create `docs/UpdateLambdaJsonRecipe.md`**

```markdown
# UpdateLambdaJsonRecipe

Updates fixed keys in `lambda.json` files matching `**/config/*/lambda.json`.

## What it does

| Operation | Path | Value |
|---|---|---|
| Set | `$.runtime` | `"java25"` |
| Set | `$.handler` | `"com.example.Handler"` |
| Set | `$.deploymentCordinates.region` | `"us-east-1"` |
| Delete | `$.functionVersion` | — |
| Delete | `$.version` | — |

Values are hardcoded in `LambdaJsonConstants.java`; edit and republish to change them.

## File pattern

`**/config/*/lambda.json` — covers single-module (`config/{name}/lambda.json` at repo root) and monorepo (`{module}/config/{name}/lambda.json`).

## Edge cases

- File missing → no-op.
- Target key absent → no-op (per built-in `ChangeValue`/`DeleteKey`).
- `deploymentCordinates` absent → top-level updates still happen; nested skipped silently.
- File outside the pattern → not touched.

## Usage

```bash
./gradlew --init-script /path/to/init.gradle rewriteRun
```
With `activeRecipe('com.rewrite.repoMaintenance.lambdaJson.UpdateLambdaJsonRecipe')` in `init.gradle`.

## Testing

```bash
./gradlew test --tests 'com.rewrite.repoMaintenance.lambdaJson.UpdateLambdaJsonRecipeTest'
```
```

- [ ] **Step 2: Create `docs/UpgradeProjectDependenciesRecipe.md`**

```markdown
# UpgradeProjectDependenciesRecipe

Bumps declared dependency and plugin versions across `build.gradle`, `build.gradle.kts`, and `libs.versions.toml`.

## What it does

Mappings live in `DependencyVersionConstants.java`:

| Kind | groupId / pluginId | artifactId | New version |
|---|---|---|---|
| dependency | `io.vertx` | `vertx-core` | `5.0.5` |
| dependency | `org.springframework` | `spring-core` | `6.2.0` |
| plugin | `org.springframework.boot` | — | `3.4.0` |

Edit the constants file and republish to change the list.

## Supported version sources

| Source | Supported |
|---|---|
| Inline `build.gradle` (Groovy DSL) | ✅ |
| Inline `build.gradle.kts` (Kotlin DSL) | ✅ |
| `libs.versions.toml` via `version.ref` | ✅ |
| `settings.gradle` `gradle.ext.*` indirection | ⚠️ Best-effort; the corresponding test is `@Disabled`. If your repo relies on this pattern, manually verify or fall back to inlining. |

## Usage

```bash
./gradlew --init-script /path/to/init.gradle rewriteRun
```
With `activeRecipe('com.rewrite.repoMaintenance.dependencies.UpgradeProjectDependenciesRecipe')`.

## Testing

```bash
./gradlew test --tests 'com.rewrite.repoMaintenance.dependencies.UpgradeProjectDependenciesRecipeTest'
```
```

- [ ] **Step 3: Create `docs/CheckstyleAutoFormatRecipe.md`**

```markdown
# CheckstyleAutoFormatRecipe

Encodes Checkstyle-derived formatting as a composition of OpenRewrite recipes plus a JAR-shipped `META-INF/rewrite/style.yml`. **No Checkstyle XML is written to the target repo.**

## Recipe ordering

| # | Recipe | Checkstyle module mapped |
|---|---|---|
| 1 | `OrderImports` (groups `*`, `javax`, `java`) | `ImportOrder`, `RedundantImport`, `UnusedImports` |
| 2 | `RemoveTrailingWhitespace` | `RegexpSingleline` |
| 3 | `BlankLines` (max 1) | `RegexpMultiline`, `EmptyLineSeparator` |
| 4 | `EmptyNewlineAtEndOfFile` | `NewlineAtEndOfFile` |
| 5 | `ModifierOrder` | `ModifierOrder` |
| 6 | `RedundantModifier` | `RedundantModifier` |
| 7 | `SimplifyBooleanExpression` | `SimplifyBooleanExpression` |
| 8 | `SimplifyBooleanReturn` | `SimplifyBooleanReturn` |
| 9 | `EqualsAvoidsNull` | `EqualsAvoidNull` |
| 10 | `EmptyBlock` | `EmptyStatement` (closest match) |
| 11 | `UseJavaStyleArrayDeclarations` | `ArrayTypeStyle` |
| 12 | `AutoFormat` | `Indentation`, `LeftCurly`/`RightCurly`, generic whitespace, etc. |

## Style settings

`src/main/resources/META-INF/rewrite/style.yml` pins:
- 2-space indentation, 4-space continuation
- Import layout: `*` → `javax` → `java` → static
- Max 1 blank line in declarations and code

## Known gaps (not enforced)

- **`LineLength max=120`** — no auto-wrap available in OpenRewrite. Tracked as a future enhancement.
- **Naming conventions** (`ConstantName`, `LocalVariableName`, `MemberName`, `MethodName`, `ParameterName`, `StaticVariableName`, `TypeName`) — these are checks, not safe transforms.
- `IllegalImport`, `IllegalInstantiation`, `EqualsHashCode`, `CovariantEquals`, `MissingSwitchDefault`, `InterfaceIsType`, custom `AnnotationLocation` variant — not mapped.

## Usage

```bash
./gradlew --init-script /path/to/init.gradle rewriteRun
```
With `activeRecipe('com.rewrite.repoMaintenance.checkstyle.CheckstyleAutoFormatRecipe')`.

## Testing

```bash
./gradlew test --tests 'com.rewrite.repoMaintenance.checkstyle.CheckstyleAutoFormatRecipeTest'
```
```

- [ ] **Step 4: Create `docs/PostJava25UpgradeRecipe.md`**

```markdown
# PostJava25UpgradeRecipe

Top-level composite: runs the existing JDK 25 upgrade plus the three new maintenance recipes.

## What it runs (in order)

1. `com.rewrite.jdk25Upgrade.UpgradeToJava25Recipe` — JDK toolchain + workflow YAML
2. `com.rewrite.repoMaintenance.lambdaJson.UpdateLambdaJsonRecipe` — JSON edits
3. `com.rewrite.repoMaintenance.dependencies.UpgradeProjectDependenciesRecipe` — dependency / plugin bumps
4. `com.rewrite.repoMaintenance.checkstyle.CheckstyleAutoFormatRecipe` — format last

Format runs last so it cleans up anything the upstream recipes touched.

## Entry-point matrix

| Entry point | What it runs |
|---|---|
| `com.rewrite.jdk25Upgrade.UpgradeToJava25Recipe` | Java 25 upgrade only |
| `com.rewrite.repoMaintenance.lambdaJson.UpdateLambdaJsonRecipe` | lambda.json updates only |
| `com.rewrite.repoMaintenance.dependencies.UpgradeProjectDependenciesRecipe` | dependency/plugin bumps only |
| `com.rewrite.repoMaintenance.checkstyle.CheckstyleAutoFormatRecipe` | format only |
| `com.rewrite.repoMaintenance.PostJava25UpgradeRecipe` | **all four** (recommended) |
| `com.recipies.yaml.PostJava25Upgrade` | all four; JDK leg uses Kotlin-precondition variant |

## Usage

```bash
./gradlew publishToMavenLocal
./gradlew --init-script /path/to/init.gradle rewriteRun
```
With `activeRecipe('com.rewrite.repoMaintenance.PostJava25UpgradeRecipe')` in `init.gradle`.

## Testing

```bash
./gradlew test --tests 'com.rewrite.repoMaintenance.PostJava25UpgradeRecipeTest'
```

## References

- [UpdateLambdaJsonRecipe](./UpdateLambdaJsonRecipe.md)
- [UpgradeProjectDependenciesRecipe](./UpgradeProjectDependenciesRecipe.md)
- [CheckstyleAutoFormatRecipe](./CheckstyleAutoFormatRecipe.md)
- [UpgradeToJava25Recipe](./UpgradeToJava25Recipe.md)
```

- [ ] **Step 5: Commit**

```bash
git add docs/UpdateLambdaJsonRecipe.md \
        docs/UpgradeProjectDependenciesRecipe.md \
        docs/CheckstyleAutoFormatRecipe.md \
        docs/PostJava25UpgradeRecipe.md
git commit -m "docs: add per-recipe READMEs for repo-maintenance bundle"
```

---

## Task 9: Final validation

- [ ] **Step 1: Run the full test suite**

Run: `./gradlew clean test`
Expected: `BUILD SUCCESSFUL`. All tests pass; the disabled `settingsGradleExtPropertyIndirection` shows as skipped.

- [ ] **Step 2: Run a clean build to ensure the JAR is produced**

Run: `./gradlew clean build`
Expected: `BUILD SUCCESSFUL`. JAR at `build/libs/openrewrite-custom-recipes-1.0.0.jar`.

- [ ] **Step 3: Verify both `rewrite.yml` and `style.yml` are bundled**

Run: `jar tf build/libs/openrewrite-custom-recipes-1.0.0.jar | grep META-INF/rewrite`
Expected output:
```
META-INF/rewrite/rewrite.yml
META-INF/rewrite/style.yml
```

- [ ] **Step 4: Verify the new recipes are loadable from the YAML composite**

Run: `./gradlew test --tests '*' --info 2>&1 | grep -i 'PostJava25Upgrade'`
Expected: references to both `com.rewrite.repoMaintenance.PostJava25UpgradeRecipe` and `com.recipies.yaml.PostJava25Upgrade`.

- [ ] **Step 5: Publish to local Maven for end-to-end smoke (optional)**

Run: `./gradlew publishToMavenLocal`
Expected: jar installed at `~/.m2/repository/com/rewrite/openrewrite-custom-recipes/1.0.0/`.

- [ ] **Step 6: Final commit if any docs or build files drifted**

```bash
git status
# If anything is dirty:
git add -A
git commit -m "chore: final cleanup after maintenance recipes implementation"
```

---

## Pre-merge checklist

- [ ] `./gradlew test` green (full suite)
- [ ] `./gradlew clean build` green
- [ ] All four new recipes registered or referenced in `META-INF/rewrite/rewrite.yml`
- [ ] `style.yml` present at `META-INF/rewrite/style.yml` and bundled in the JAR
- [ ] `gradle.ext` indirection test outcome documented in `UpgradeProjectDependenciesRecipe.md`
- [ ] `CheckstyleAutoFormatRecipe` `alreadyFormattedFileIsNoOp` test passes (idempotence proxy)
- [ ] All four `docs/*Recipe.md` files exist
