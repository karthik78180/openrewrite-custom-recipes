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
