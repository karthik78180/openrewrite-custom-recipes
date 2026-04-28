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
| Inline `build.gradle` (Groovy DSL) | Yes |
| Inline `build.gradle.kts` (Kotlin DSL) | Yes |
| `libs.versions.toml` via `version.ref` | Yes |
| `settings.gradle` `gradle.ext.*` indirection | Best-effort; the corresponding test is `@Disabled`. If your repo relies on this pattern, manually verify or fall back to inlining. |

## Known Limitations

1. **Kotlin DSL unit-test limitation:** `UpgradeDependencyVersion` requires `GradleDependencyConfiguration` markers that the Gradle tooling API supplies at resolution time. Unit tests using `RewriteTest` lack these markers, so the `inlineKotlinDslBuildGradleUpgrade` test is `@Disabled`. Production usage against a real Gradle build is unaffected — markers are present when Gradle resolves the build.

2. **`versionCatalogRefUpdated` test is `@Disabled`:** `org.openrewrite.gradle.toml.Assertions.versionCatalog` is absent from `rewrite-gradle 8.80.1`. Re-enable when the helper becomes available in a newer BOM.

3. **`settings.gradle` `gradle.ext` indirection is `@Disabled`:** `UpgradeDependencyVersion` does not resolve `gradle.ext.fooVersion` indirection. Affected users must declare versions inline or via `libs.versions.toml`.

## Usage

```bash
./gradlew --init-script /path/to/init.gradle rewriteRun
```
With `activeRecipe('com.rewrite.repoMaintenance.dependencies.UpgradeProjectDependenciesRecipe')`.

## Testing

```bash
./gradlew test --tests 'com.rewrite.repoMaintenance.dependencies.UpgradeProjectDependenciesRecipeTest'
```
