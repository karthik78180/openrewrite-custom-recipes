# CheckstyleAutoFormatRecipe

Encodes Checkstyle-derived formatting as a composition of OpenRewrite recipes plus a JAR-shipped `META-INF/rewrite/style.yml`. **No Checkstyle XML is written to the target repo.**

## Recipe ordering

The composite ships 11 recipes (not 12; see Known Gaps below):

| # | Recipe | Checkstyle module mapped |
|---|---|---|
| 1 | `OrderImports` (groups `*`, `javax`, `java`) | `ImportOrder`, `RedundantImport`, `UnusedImports` |
| 2 | `RemoveTrailingWhitespace` | `RegexpSingleline` |
| 3 | `BlankLines` (max 1) | `RegexpMultiline`, `EmptyLineSeparator` |
| 4 | `EmptyNewlineAtEndOfFile` | `NewlineAtEndOfFile` |
| 5 | `ModifierOrder` | `ModifierOrder` |
| 6 | `SimplifyBooleanExpression` | `SimplifyBooleanExpression` |
| 7 | `SimplifyBooleanReturn` | `SimplifyBooleanReturn` |
| 8 | `EqualsAvoidsNull` | `EqualsAvoidNull` |
| 9 | `EmptyBlock` | `EmptyStatement` (closest match) |
| 10 | `UseJavaStyleArrayDeclarations` | `ArrayTypeStyle` |
| 11 | `AutoFormat` | `Indentation`, `LeftCurly`/`RightCurly`, generic whitespace, etc. |

## Style settings

`src/main/resources/META-INF/rewrite/style.yml` pins:
- 2-space indentation, 4-space continuation
- Import layout: `*` → `javax` → `java` → static
- Max 1 blank line in declarations and code

## Known gaps

- **Composite size: 11 (not 12).** `RedundantModifier` is not present in `rewrite-static-analysis 2.11.0`. The `redundantPublicOnInterfaceMethodRemoved` test is `@Disabled`. Track for re-introduction when upgrading to a newer `rewrite-static-analysis` version.

- **`indentationReformattedToTwoSpaces` is `@Disabled`:** `AutoFormat` does not load JAR-shipped `META-INF/rewrite/style.yml` in the `RewriteTest` classpath. Therefore `style.yml`'s 2-space indent is not exercised by unit tests; it applies in production via the OpenRewrite Gradle plugin.

- **Idempotence note:** `AutoFormat` with IntelliJ defaults (no style.yml loaded) inserts a blank line after `package` and expands compact class bodies (`{}` → `{\n}`). Tests that go through the full composite must account for this in their expected output.

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
