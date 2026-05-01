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
