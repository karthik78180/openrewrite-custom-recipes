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
